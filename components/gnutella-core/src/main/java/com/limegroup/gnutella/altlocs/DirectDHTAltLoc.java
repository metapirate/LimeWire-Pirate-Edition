package com.limegroup.gnutella.altlocs;

import java.io.IOException;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;

import com.limegroup.gnutella.URN;

/**
 * An AltLoc that was found through the DHT.
 */
public class DirectDHTAltLoc extends DirectAltLoc {

    private final long fileSize;
    
    private final byte[] ttroot;
    
    public DirectDHTAltLoc(IpPort address, URN sha1, long fileSize, byte[] ttroot,
            NetworkInstanceUtils networkInstanceUtils)
            throws IOException {
        super(address, sha1, networkInstanceUtils);
        
        this.fileSize = fileSize;
        this.ttroot = ttroot;
    }
    
    /**
     * Returns the File size or -1 if it's unknown.
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * Returns the TigerTree root hash or null if it's unknown.
     */
    public byte[] getRootHash() {
        return ttroot;
    }
}
