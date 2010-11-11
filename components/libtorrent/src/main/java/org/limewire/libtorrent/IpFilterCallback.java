package org.limewire.libtorrent;

import com.sun.jna.Callback;
import org.limewire.bittorrent.TorrentIpFilter;

/**
 * Called by libtorrent C++ code in order to
 * use LW's IP blacklist.
 */
public class IpFilterCallback implements Callback {
    
    private final TorrentIpFilter torrentFilter;

    public IpFilterCallback(TorrentIpFilter torrentFilter) {
        this.torrentFilter = torrentFilter;    
    }

    /**
     * JNA Function used by libtorrent code.
     * 
     * @param ipAddress ip address as big endian int to use to check with blacklist
     * @return int value for use with libtorrent (0 == allow, 1 == block)
     */
    public int callback(int ipAddress) {
        return torrentFilter.allow(ipAddress) ? 0 : 1;
    }
}
