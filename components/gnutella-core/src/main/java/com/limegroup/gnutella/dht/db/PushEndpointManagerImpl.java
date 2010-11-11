package com.limegroup.gnutella.dht.db;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

/**
 * Transparently interfaces with {@link PushEndpointService} 
 * and {@link PushEndpointCache}.
 * <p>
 * It looks up {@link PushEndpoint push endpoints} in the cache and if it
 * can't find a good instance there decides if it asks the finder to look up
 * the push endpoint.
 */
@Singleton
class PushEndpointManagerImpl implements PushEndpointService {

    private static final Log LOG = LogFactory.getLog(PushEndpointManagerImpl.class);
    
    private final PushEndpointCache pushEndpointCache;
    private final PushEndpointService pushEndpointFinder;
    private final ConcurrentMap<GUID, AtomicLong> lastSearchTimeByGUID = new ConcurrentHashMap<GUID, AtomicLong>(); 
    
    private volatile long timeBetweenSearches = DHTSettings.TIME_BETWEEN_PUSH_PROXY_QUERIES.getValue();
    
    @Inject
    public PushEndpointManagerImpl(PushEndpointCache pushEndpointCache, 
            @Named("dhtPushEndpointFinder") PushEndpointService pushEndpointFinder) {
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFinder = pushEndpointFinder;
    }
    
    /**
     * Set time in milliseconds between searches.
     * 
     * Mainly for testing but could be exposed if necessary. 
     */
    void setTimeBetweenSearches(long timeBetweenSearches) {
        this.timeBetweenSearches = timeBetweenSearches;
    }
    
    /**
     * Gets time between searches in milliseconds.  
     */
    long getTimeBetweenSearches() {
        return timeBetweenSearches;
    }
    
    public void findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener) {
        listener = SearchListenerAdapter.nonNullListener(listener);
        PushEndpoint cachedPushEndpoint = pushEndpointCache.getPushEndpoint(guid);
        if (cachedPushEndpoint != null && !cachedPushEndpoint.getProxies().isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found cached endpoint: " + cachedPushEndpoint);
            }
            listener.handleResult(cachedPushEndpoint);
        } else {
            long currentTime = System.currentTimeMillis();
            AtomicLong lastSearchTime = lastSearchTimeByGUID.putIfAbsent(guid, new AtomicLong(currentTime));
            // no old value, just start a search, knowing we are set as last search time
            if (lastSearchTime == null) {
                startSearch(guid, listener);
            } else {
                long lastSearch = lastSearchTime.longValue();
                // check if we can start a new search
                if (currentTime - lastSearch > timeBetweenSearches) {
                    // only start a search if we succeed setting the current time and it was still the last time
                    if (lastSearchTime.compareAndSet(lastSearch, currentTime)) {
                        startSearch(guid, listener);
                    } else {
                        listener.searchFailed();
                    }
                } else {
                    listener.searchFailed();
                }
            }
        }
    }
    
    void startSearch(GUID guid, final SearchListener<PushEndpoint> listener) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting dht search for: " + guid);
        }
        
        if (!DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
            listener.searchFailed();
            return;
        }
        
        pushEndpointFinder.findPushEndpoint(guid, new SearchListener<PushEndpoint>() {
                public void handleResult(PushEndpoint result) {
                    // notify cache about it
                    result.updateProxies(true);
                    IpPort externalAddress = result.getValidExternalAddress();
                    if (externalAddress != null) {
                        pushEndpointCache.setAddr(result.getClientGUID(), externalAddress);
                    }
                    listener.handleResult(result);
                }
                public void searchFailed() {
                    LOG.debug("dht search failed");
                    listener.searchFailed();
                }
        });
        purge();
    }

    public PushEndpoint getPushEndpoint(GUID guid) {
        BlockingSearchListener<PushEndpoint> listener = new BlockingSearchListener<PushEndpoint>();
        findPushEndpoint(guid, listener);
        return listener.getResult();
    }
    
    /**
     * Purges entries older than 2 * {@link #getTimeBetweenSearches()}.
     */
    void purge() {
        long currentTime = System.currentTimeMillis();
        for (Entry<GUID, AtomicLong> entry : lastSearchTimeByGUID.entrySet()) {
            assert entry != null : "entry is null: " + lastSearchTimeByGUID;
            assert entry.getValue() != null : "value of entry is null: " + lastSearchTimeByGUID;
            if (currentTime - entry.getValue().get() > 2 * timeBetweenSearches) {
                lastSearchTimeByGUID.remove(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * Access for testing only
     */
    ConcurrentMap<GUID, AtomicLong> getLastSearchTimeByGUID() {
        return lastSearchTimeByGUID;
    }
    
}
