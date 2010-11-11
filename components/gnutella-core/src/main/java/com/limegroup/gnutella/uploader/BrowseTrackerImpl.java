package com.limegroup.gnutella.uploader;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.browse.server.BrowseTracker;

import com.google.inject.Singleton;

@Singleton
public class BrowseTrackerImpl implements BrowseTracker {
    
    private final Map<String, Date> browseHistory;
    private final Map<String, Date> refreshHistory;
    
    public BrowseTrackerImpl() {
        this.browseHistory = new ConcurrentHashMap<String, Date>();
        this.refreshHistory = new ConcurrentHashMap<String, Date>();
    }
    
    @Override
    public void browsed(String friendId) {
        browseHistory.put(friendId, new Date());    
    }

    @Override
    public Date lastBrowseTime(String friendId) {
        return browseHistory.get(friendId);
    }

    @Override
    public void sentRefresh(String friendId) {
        refreshHistory.put(friendId, new Date());
    }

    @Override
    public Date lastRefreshTime(String friendId) {
        return refreshHistory.get(friendId);
    }
}
