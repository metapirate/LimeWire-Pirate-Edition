package org.limewire.bittorrent;

import java.net.URI;

/**
 * Represents data for a torrent tracker.
 */
public interface TorrentTracker {

    /**
     * Returns the url for this tracker.
     */
    public URI getURI();

    /**
     * Returns the tier for this tracker.
     */
    public int getTier();
}
