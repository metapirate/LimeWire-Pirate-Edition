package org.limewire.bittorrent;

/**
 * Torrent specific interface to find out if an IP address is banned. 
 */
public interface TorrentIpFilter {

    /**
     * Method returning whether an IP Address is allowed
     * for use in torrents.
     * 
     * @param ipAddress
     * @return true if the integer ipAddress is allowed, false otherwise.
     */
    boolean allow(int ipAddress);  
}
