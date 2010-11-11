package com.limegroup.gnutella.downloader.serial;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;


public class BTDiskManagerMementoImpl implements BTDiskManagerMemento, Serializable {

    private Map<String, Object> serialObjects = new HashMap<String, Object>();
    
    @SuppressWarnings("unchecked")
    public Map<Integer, IntervalSet> getPartialBlocks() {
        return (Map<Integer, IntervalSet>)serialObjects.get("partialBlocks");
    }

    public BitSet getVerifiedBlocks() {
        return (BitSet)serialObjects.get("verifiedBlocks");
    }

    public boolean isVerifying() {
        Boolean b = (Boolean)serialObjects.get("verifying");
        if(b == null)
            return false;
        else
            return b;
    }

    public void setPartialBlocks(Map<Integer, IntervalSet> partialBlocks) {
        serialObjects.put("partialBlocks", partialBlocks);
    }

    public void setVerifiedBlocks(BitSet verifiedBlocks) {
        serialObjects.put("verifiedBlocks", verifiedBlocks);
    }

    public void setVerifying(boolean verifying) {
        serialObjects.put("verifying", verifying);
    }

}
