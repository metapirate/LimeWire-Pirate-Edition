package org.limewire.bittorrent;

import java.net.URI;

public interface TorrentTrackerScraper {

    /**
     * Submit the scrape request.  Notification will be returned through the callback
     *
     * @return the shutdownable for the connection, or null if no 
     *          connection was supported.
     */
    RequestShutdown submitScrape(URI trackerAnnounceUri, String urn, ScrapeCallback callback);

    public static interface ScrapeCallback {
        void success(TorrentScrapeData data);
        void failure(String reason);
    }
    
    public static interface RequestShutdown {
        void shutdown();
    }
    
}
