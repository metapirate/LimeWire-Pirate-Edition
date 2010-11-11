package com.limegroup.gnutella;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.io.GUID;

import com.google.inject.Provider;
import com.limegroup.gnutella.guess.GUESSEndpoint;

/**
 * Keeps track of possible ips that provide results for
 * query GUIDs and have not been queried yet. 
 */
public class BypassedResultsCache {
    
    /**
     * The maximum number of bypassed results to remember per query.
     */
    static int MAX_BYPASSED_RESULTS = 150;
    
    private final Provider<? extends ActivityCallback> _callback;
    
    private final DownloadManager _downloadManager;
    
    /**
     * Keeps track of potential sources of content.  Comprised of Sets of GUESS
     * Endpoints.  Kept tidy when searches/downloads are killed.
     */
    private final Map<GUID, Set<GUESSEndpoint>> _bypassedResults = Collections.synchronizedMap(new HashMap<GUID, Set<GUESSEndpoint>>());


    public BypassedResultsCache(Provider<? extends ActivityCallback> callback, DownloadManager downloadManager) {
        _callback = callback;
        _downloadManager = downloadManager;
    }
    
    // made package private until this is refactored to events and
    // the cache can subscribe to a query killed event
    void queryKilled(GUID guid) {
        if (!_downloadManager.isGuidForQueryDownloading(guid)) {
            _bypassedResults.remove(guid);
        }
    }
    
    // made package private until this is refactored to events and
    // the cache can subscribe to a download finished event
    void downloadFinished(GUID guid) {
        if (!isGUIDOfInterest(guid)) {
            _bypassedResults.remove(guid);
        }
    }
    
    /**
     * Returns a set of possible query endpoints for the guid or 
     * an empty set if there aren't any.
     *
     * @return the set is owned by the caller and can be modified
     */
    public Set<GUESSEndpoint> getQueryLocs(GUID guid) {
        Set<GUESSEndpoint> clone = new HashSet<GUESSEndpoint>();
        synchronized (_bypassedResults) {
            Set<GUESSEndpoint> eps = _bypassedResults.get(guid);
            if (eps != null)
                clone.addAll(eps);
        }
        return clone;
    }
    
    /**
     * Returns true if the guid is of interest to this peer, i.e. there is an 
     * active query or a download for this guid. 
     */
    private boolean isGUIDOfInterest(GUID guid) {
        return _callback.get().isQueryAlive(guid) || _downloadManager.isGuidForQueryDownloading(guid);
    }
    
    /**
     * Adds the endpoint to its internal cache and returns true if it
     * does so.
     */
    public boolean addBypassedSource(GUID guid, GUESSEndpoint endpoint) {
        if (!isGUIDOfInterest(guid)) {
            return false;
        }
        synchronized (_bypassedResults) {
            // this is a quick critical section for _bypassedResults
            // AND the set within it
            Set<GUESSEndpoint> eps = _bypassedResults.get(guid);
            if (eps == null) {
                eps = new HashSet<GUESSEndpoint>();
                _bypassedResults.put(guid, eps);
            }
            if (eps.size() < MAX_BYPASSED_RESULTS) {
                return eps.add(endpoint);
            }
            else {
                return false;
            }
        }
    }
    
}
