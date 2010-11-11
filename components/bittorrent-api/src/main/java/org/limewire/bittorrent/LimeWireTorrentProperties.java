package org.limewire.bittorrent;

/**
 * Contains Property String for returning values within a Torrent specific to LimeWire.
 */
public interface LimeWireTorrentProperties {

    /** The Max Seed Time Ratio Limit for a specific Torrent. If no limit has been set
      * on this Torrent, this may return null or -1.
      */
    public static final String MAX_SEED_TIME_RATIO_LIMIT = "MAX_SEED_TIME_RATIO_LIMIT";
    
    /** The Max Seed Ratio Limit for a specific Torrent. If no limit has been set
      * on this Torrent, this may return null or -1.
      */
    public static final String MAX_SEED_RATIO_LIMIT = "MAX_SEED_RATIO_LIMIT";
}
