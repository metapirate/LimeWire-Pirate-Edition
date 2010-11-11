package org.limewire.bittorrent;

import org.limewire.util.StringUtils;

/**
 * Data returned by a tracker scrape for a given torrent.
 */
public class TorrentScrapeData {
    
    private final long complete, incomplete, downloaded;
    
    public TorrentScrapeData(long complete, long incomplete, long downloaded) {
        this.complete = complete;
        this.incomplete = incomplete;
        this.downloaded = downloaded;
    }
    
    /**
     * @return number of peers on the tracker that have the entire torrent.
     *          aka seeders
     */
    public long getComplete() {
        return complete;
    }
    
    /**
     * @return number of peers on the tracker that have the do not 
     *          have the entire torrent.
     *          aka leechers
     */
    public long getIncomplete() {
        return incomplete;
    }
    
    /**
     * @return number of times the torrent has been downloaded.
     */
    public long getDownloaded() {
        return downloaded;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
