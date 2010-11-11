package org.limewire.bittorrent;

/**
 * Represents a peer connected to a torrent.
 */
public interface TorrentPeer {
    
    /**
     * Returns a hex string representation for this torrents peer id.
     */
    public String getPeerId();

    /**
     * Returns this peers ip address.
     */
    public String getIPAddress();

    /**
     * Returns the current total upload speed to this peer in bytes/sec.
     */
    public float getUploadSpeed();

    /**
     * Returns the current total download speed from this peer in bytes/sec.
     */
    public float getDownloadSpeed();

    /**
     * Returns the current payload upload speed to this peer in bytes/sec.
     */
    public float getPayloadUploadSpeed();

    /**
     * Returns the current payload download speed from this peer in bytes/sec.
     */
    public float getPayloadDownloadSpeed();

    /**
     * Returns the peers progress downloading the torrent in a number from 0 to
     * 1
     */
    public float getProgress();

    /**
     * Returns a 2 character code representing the peers country.
     */
    public String getCountry();

    /**
     * The name of the peer's client.
     */
    public String getClientName();
    
    /**
     * Returns true if this peer is in the list of sources from the tracker.
     * The peer can be in multiple lists.
     */
    boolean isFromTracker();

    /**
     * Returns true if this peer is in the list of sources from the DHT. The
     * peer can be in multiple lists.
     */
    boolean isFromDHT();

    /**
     * Returns true if this peer is in the list of sources from peer exchange.
     * The peer can be in multiple lists.
     */
    boolean isFromPEX();

    /**
     * Returns true if this peer is in the list of sources from local service
     * discovery. The peer can be in multiple lists.
     */
    boolean isFromLSD();
    
    /**
     * Whether the peer connection is encrypted or not.
     */
    boolean isEncrypted();
}
