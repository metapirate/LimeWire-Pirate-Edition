package org.limewire.bittorrent;

/**
 * Represents an event happening on a torrent. 
 */
public class TorrentEvent {
    private final Torrent torrent;
    private final TorrentEventType type;
    
    public TorrentEvent(Torrent torrent, TorrentEventType type) {
        super();
        this.torrent = torrent;
        this.type = type;
    }

    public Torrent getTorrent() {
        return torrent;
    }
    
    public TorrentEventType getType() {
        return type;
    }
}
