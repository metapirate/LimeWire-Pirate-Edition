package org.limewire.bittorrent;

/**
 * Represents an address and a port.
 */
public interface TorrentIpPort {
    /**
     * Returns the address for this ip/port pair.
     */
    public String getAddress();

    /**
     * Returns the port.
     */
    public int getPort();
}
