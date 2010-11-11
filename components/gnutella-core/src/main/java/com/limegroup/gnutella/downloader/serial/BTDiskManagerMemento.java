package com.limegroup.gnutella.downloader.serial;

import java.util.Map;

import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;

public interface BTDiskManagerMemento {
    
    boolean isVerifying();

    Map<Integer, IntervalSet> getPartialBlocks();

    BitSet getVerifiedBlocks();
    
    void setVerifying(boolean verifying);
    
    void setPartialBlocks(Map<Integer, IntervalSet> partialBlocks);
    
    void setVerifiedBlocks(BitSet verifiedBlocks);
}
