package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;
import java.util.Map;

import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;

class SerialDiskManagerData implements Serializable {
    
    private static final long serialVersionUID = -6901065516261232111l;
    
    private BitSet verifiedBlocks;
    private Map<Integer, IntervalSet> partialBlocks;
    private boolean isVerifying;
    

    public boolean isVerifying() {
        return isVerifying;
    }

    public Map<Integer, IntervalSet> getPartialBlocks() {
        return partialBlocks;
    }

    public BitSet getVerifiedBlocks() {
        return verifiedBlocks;
    }

}
