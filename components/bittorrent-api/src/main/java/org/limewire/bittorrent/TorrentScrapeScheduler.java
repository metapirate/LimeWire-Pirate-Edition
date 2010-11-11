package org.limewire.bittorrent;

import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;

public interface TorrentScrapeScheduler {

    /**
     * Initiate a scrape request asyncronously.  Results will be available
     *  by {link #getScrapeDataIfAvailable()}.  If the result is already
     *  cached this will have no effect.
     */
    void queueScrapeIfNew(Torrent torrent);
    
    /**
     * Initiate a scrape request asyncronously.  Results will be available
     *  by {link #getScrapeDataIfAvailable()} and notified using the callback.
     *  Will queue the scrape regardless of the cache.  Essentially forces an
     *  update.
     */
    void queueScrape(Torrent torrent, ScrapeCallback callback);

    /**
     * Get any scrape results if available.
     * 
     * @return null if no scrape data available.
     */
    TorrentScrapeData getScrapeDataIfAvailable(Torrent torrent);

}
