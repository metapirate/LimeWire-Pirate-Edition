package com.limegroup.gnutella;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.lifecycle.ServiceScheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

@EagerSingleton
class PushEndpointCacheImpl implements PushEndpointCache {
    
    private static final Log LOG = LogFactory.getLog(PushEndpointCacheImpl.class);
    
    /**
     * A mapping from GUID to a CachedPushEndpoint.  This is used to ensure
     * that all PE's will have access to the same PushProxies, even if
     * multiple PE's exist for a single GUID.  Because access to the proxies
     * is referenced from this GUID_PROXY_MAP, the PE will always receive the
     * most up-to-date set of proxies.
     *
     * Insertion to this map must be manually performed, to allow for temporary
     * PE objects that are used to update pre-existing ones.
     *
     * There is no explicit removal from the map -- the Weak nature of it will
     * automatically remove the key/value pairs when the key is garbage
     * collected.  For this reason, all PEs must reference the exact GUID
     * object that is stored in the map -- to ensure that the map will not GC
     * the GUID while it is still in use by a PE.
     *
     * The value is a CachedPushEndpoint SetWrapper (containing a WeakReference to the
     * GUID key as well as the Set of proxies) so that subsequent PEs can 
     * reference the true key object.  A WeakReference is used to allow
     * GC'ing to still work and the map to ultimately remove unused keys.
     */
    private final Map<GUID, CachedPushEndpoint> GUID_PROXY_MAP = 
        Collections.synchronizedMap(new WeakHashMap<GUID, CachedPushEndpoint>());
    
    private final HTTPHeaderUtils httpHeaderUtils;

    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    PushEndpointCacheImpl(HTTPHeaderUtils httpHeaderUtils, NetworkInstanceUtils networkInstanceUtils) {
        this.httpHeaderUtils = httpHeaderUtils;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    @Inject
    public void register(final @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, ServiceScheduler serviceScheduler) {
        serviceScheduler.scheduleWithFixedDelay("PushEndpointCacheImpl WeakHashMap Cleaner", new WeakCleaner(), 30, 30, TimeUnit.SECONDS, backgroundExecutor);
    }

    /**
     * Overwrites the current known push proxies for the host specified
     * by the GUID, using the the proxies as written in the httpString.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param httpString comma-separated list of proxies and possible proxy features
     */
    public void overwriteProxies(byte [] guid, String httpString) {
        Set<? extends Connectable> newSet = httpHeaderUtils.decodePushProxies(httpString, ",");
        overwriteProxies(guid, newSet);
    }

    /**
     * Overwrites any stored proxies for the host specified by the guid.
     * 
     * @param guid the guid whose proxies to overwrite
     * @param newSet the proxies to overwrite with
     */
    public void overwriteProxies(byte[] guid, Set<? extends IpPort> newSet) {
        GUID g = new GUID(guid);
        CachedPushEndpoint wrapper ;
        synchronized(GUID_PROXY_MAP) {
            wrapper = GUID_PROXY_MAP.get(g);
            if (wrapper==null) {
                wrapper = new CachedPushEndpoint(g, newSet);
                GUID_PROXY_MAP.put(g, wrapper);
            } else {
                wrapper.overwriteProxies(newSet);
            }
        }
    }
    
    public void removePushProxy(byte[] bytes, IpPort pushProxy) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing push proxy: " + pushProxy + " for " + new GUID(bytes));
        }
        GUID guid = new GUID(bytes);
        CachedPushEndpoint cachedPushEndpoint = GUID_PROXY_MAP.get(guid);
        if (cachedPushEndpoint != null) {
            cachedPushEndpoint.removePushProxy(pushProxy);
        }
    }

    /**
     * updates the external address of all PushEndpoints for the given guid
     */
    public void setAddr(byte [] guid, IpPort addr) {
        GUID g = new GUID(guid);
        CachedPushEndpoint current = getCached(g);
        if (current!=null)
            current.setIpPort(addr);
    }

    /**
     * Sets the fwt version supported for all PEs pointing to the
     * given client guid.
     */
    public void setFWTVersionSupported(byte[] guid,int version){
    	GUID g = new GUID(guid);
    	CachedPushEndpoint current = getCached(g);
    	if (current!=null)
    		current.setFWTVersion(version);
    }
    
    public CachedPushEndpoint getCached(GUID guid) {
        return GUID_PROXY_MAP.get(guid);
    }

    public PushEndpoint getPushEndpoint(GUID guid) {
        CachedPushEndpoint cached = GUID_PROXY_MAP.get(guid);
        return cached != null ? cached.createClone() : null;
    }
    
    public void findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener) {
        PushEndpoint pushEndpoint = getPushEndpoint(guid);
        if (pushEndpoint != null) {
            listener.handleResult(pushEndpoint);
        } else {
            listener.searchFailed();
        }
    }

    public GUID updateProxiesFor(GUID guid, PushEndpoint pushEndpoint, boolean valid) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating proxies for: " + guid + " with: " + pushEndpoint + ", valid: " + valid);
        }
        
        CachedPushEndpoint existing;
        GUID guidRef = null;
        
        synchronized(GUID_PROXY_MAP) {
            existing = GUID_PROXY_MAP.get(guid);

            // try to get a hard ref so that the mapping won't expire
            if (existing!=null)
                guidRef=existing.getGuid();         

            // if we do not have a mapping for this guid, or it just expired,
            // add a new one atomically
            // (we don't care about the proxies of the expired mapping)
            if (existing == null || guidRef==null) {                
                existing = new CachedPushEndpoint(guid, pushEndpoint.getFeatures(), pushEndpoint.getFWTVersion(), 
                        valid ? pushEndpoint.getProxies() : IpPort.EMPTY_SET);
                GUID_PROXY_MAP.put(guid, existing);
                return guid;
            }
        }
        
        // if we got here, means we did have a mapping.  no need to
        // hold the map mutex when updating just the set
        existing.updateProxies(pushEndpoint.getProxies(), valid);
        return guidRef;
    }
    
    public void clear() {
        GUID_PROXY_MAP.clear();
    }
    
    private final class WeakCleaner implements Runnable {
        public void run() {
            GUID_PROXY_MAP.size();
        }
    }
    
    class CachedPushEndpoint extends AbstractPushEndpoint {
        
        private final WeakReference<GUID> _guidRef;
        /**
         * Class invariant: never null
         */
        private Set<IpPort> _proxies;
        private byte _features;
        private int _fwtVersion;
        private IpPort _externalAddr;
        private final byte[] guid;
        
        CachedPushEndpoint(GUID guid, Set<? extends IpPort> proxies) {
            this(guid, (byte)0, 0, proxies);
        }
        
        CachedPushEndpoint(GUID guid, byte features, int version, Set<? extends IpPort> proxies) {
            this.guid = guid.bytes();
            _guidRef = new WeakReference<GUID>(guid);
            _features=features;
            _fwtVersion=version;
            overwriteProxies(proxies);
        }
        
        public synchronized void removePushProxy(IpPort pushProxy) {
            // It's imortant to use IPortSet instead of Collections.singleton for
            // comparator correctness based on IpPort.COMPARATOR
            updateProxies(new IpPortSet(pushProxy), false);
        }
        
        public synchronized void updateProxies(Set<? extends IpPort> s, boolean add){
            Set<IpPort> existing = new IpPortSet(_proxies);
            if (add)
                existing.addAll(s);
            else
                existing.removeAll(s);
            
            _proxies = Collections.unmodifiableSet(existing);
        }
        
        public synchronized void overwriteProxies(Set<? extends IpPort> proxies) {
            _proxies = Collections.unmodifiableSet(new IpPortSet(proxies));
        }
        
        public synchronized Set<IpPort> getProxies() {
            return _proxies;
        }
        
        public synchronized byte getFeatures() {
            return _features;
        }
        
        public synchronized int getFWTVersion() {
            return _fwtVersion;
        }
        
        public synchronized void setFeatures(byte features) {
            _features=features;
        }
        
        public synchronized void setFWTVersion(int version){
            _fwtVersion=version;
        }
        
        public synchronized void setIpPort(IpPort addr) {
            _externalAddr = addr;
        }
        
        public synchronized IpPort getIpPort() {
            return _externalAddr;
        }
        
        public GUID getGuid() {
            return _guidRef.get();
        }

        public synchronized PushEndpoint createClone() {
            return new PushEndpointImpl(guid, getProxies(), 
                    getFeatures(), getFWTVersion(), 
                    getValidExternalAddress(), PushEndpointCacheImpl.this, networkInstanceUtils);
        }

        public byte[] getClientGUID() {
            return guid;
        }

        public synchronized int getPort() {
            IpPort address = _externalAddr;
            return address != null ? address.getPort() : 6346;
        }

        public synchronized IpPort getValidExternalAddress() {
            return _externalAddr;
        }

        public boolean isLocal() {
            return false;
        }

        public synchronized void updateProxies(boolean good) {
            if (!good) {
                _proxies = Collections.emptySet();
            }
        }

        public synchronized String getAddress() {
            IpPort address = _externalAddr;
            return address != null ? _externalAddr.getAddress() : RemoteFileDesc.BOGUS_IP;
        }
        
        @Override
        public String getAddressDescription() {
            IpPort addr = getValidExternalAddress();
            return addr == null ? null : addr.getAddress();
        }

        public InetAddress getInetAddress() {
            return null;
        }

        public InetSocketAddress getInetSocketAddress() {
            return null;
        }
    }

}
