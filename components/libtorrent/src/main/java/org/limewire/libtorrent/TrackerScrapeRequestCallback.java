package org.limewire.libtorrent;

import java.util.Collections;
import java.util.Set;

import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;
import org.limewire.collection.IdentityHashSet;

import com.sun.jna.Callback;

public class TrackerScrapeRequestCallback implements Callback {
    
    private final static int STATUS_SUCCESS = 0;
    
    private final static int STATUS_TIMEOUT = 1;
    
    private final static int STATUS_ERROR = 2;
    
    private final static Set<TrackerScrapeRequestCallback> activeCallbacks =
        Collections.synchronizedSet(new IdentityHashSet<TrackerScrapeRequestCallback>());
    
    private final ScrapeCallback scrapeCallback;

    public TrackerScrapeRequestCallback(ScrapeCallback scrapeCallback) {
        this.scrapeCallback = scrapeCallback;
        activeCallbacks.add(this);
    }
    
    public void callback(int status, int complete, int incomplete, int downloads) {
        activeCallbacks.remove(this);
        switch (status) {
        case STATUS_SUCCESS:
            scrapeCallback.success(new TorrentScrapeData(complete, incomplete, downloads));
            break;
        case STATUS_TIMEOUT:
            scrapeCallback.failure("timeout");
            break;
        case STATUS_ERROR:
            scrapeCallback.failure("error");
            break;
        default:
            throw new IllegalArgumentException("unknown status code: " + status);
        } 
    }
}