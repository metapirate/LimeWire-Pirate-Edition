package org.limewire.bittorrent;

/**
 * The possible states of any torrent download piece.
 */
public enum TorrentPieceState {
    
    /**
     * The piece has been downloaded in its entirety.
     */
    DOWNLOADED,
    
    /**
     * Some of the data for the piece has been downloaded.
     */
    PARTIAL, 
    
    /**
     * Nothing has been downloaded for the piece yet, however it is available.
     */
    AVAILABLE, 
    
    /**
     * The piece is currently being downloaded or written to.
     */
    ACTIVE, 
    
    /**
     * Additional data for the piece is currently not available with the given peer set.
     */
    UNAVAILABLE;
}
