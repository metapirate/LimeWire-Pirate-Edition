package org.limewire.bittorrent;

import java.util.List;

/**
 * Provides access to meta data for the torrent.
 */
public interface TorrentInfo {
    // TODO maybe we don't want to use the same TorrentFileEntry object here as
    // on the Torrent. It has some additional fields that do not make sense.

    /**
     * Returns the current name of the torrent.  Useful if the metadata wasn't
     *  available when the download started.  Eg. magnet link start.
     */
    public String getName();
    
    /**
     * Returns the TorrentFileEntries for this torrent.
     */
    public List<TorrentFileEntry> getTorrentFileEntries();

    /**
     * Returns the length of the pieces used in this torrent.
     */
    public int getPieceLength();
    
    
    public List<TorrentTracker> getTrackers();
    
    public List<String> getSeeds();
    
}
