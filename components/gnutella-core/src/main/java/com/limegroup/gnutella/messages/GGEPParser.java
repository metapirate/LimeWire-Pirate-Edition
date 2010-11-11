package com.limegroup.gnutella.messages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;

/**
 * Allows multiple GGEP blocks to be parsed, storing
 * the 'secure GGEP' block separately.  Can store
 * the position where the secure block began & ended,
 * so that the rest of the data can be properly verified.
 */
public class GGEPParser {
    
    private static final Log LOG = LogFactory.getLog(GGEPParser.class);
    
    private GGEP normal = null;
    private GGEP secure  = null;
    private int secureStart = -1;
    private int secureEnd = -1;
    private int normalStart = -1;
    private int normalEnd = -1;
    
    /**
     * Scans through the data, starting at idx, looking for the first
     * spot that has GGEP_PREFIX_MAGIC_NUMBER, and parses GGEP blocks
     * from there.
     * Once a secure block is found, no other GGEPs are parsed.
     */
    public void scanForGGEPs(byte[] data, int idx) {
        // Find the beginning of the GGEP block.
        for (; 
             idx < data.length &&
             data[idx] != GGEP.GGEP_PREFIX_MAGIC_NUMBER;
             idx++);
        
        if(idx >= data.length) {
            LOG.debug("No GGEP in data");
            return; // nothing to parse.
        }
            
        int[] storage = new int[1];
        GGEP normal = null;
        GGEP secure = null;
        int secureStart = -1;
        int secureEnd = -1;
            
        try {
            while(idx < data.length) {
                // optimization: don't bother constructing (and throwing exception)
                //               if it clearly isn't a GGEP block.
                if(data[idx] != GGEP.GGEP_PREFIX_MAGIC_NUMBER)
                    break;
                
                GGEP ggep = new GGEP(data, idx, storage);
                if(ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_BLOCK)) {
                    secure = ggep;
                    secureStart = idx;
                    secureEnd = storage[0];
                    break;
                } else {
                    normalStart = idx;
                    normalEnd = storage[0];
                    if(normal == null)
                        normal = ggep;
                    else
                        normal.merge(ggep);
                    idx = storage[0];
                    storage[0] = -1;
                }
            }
        } catch (BadGGEPBlockException ignored) {
            LOG.debug("Unable to create ggep", ignored);
        }
        
        this.normal = normal;
        this.secure = secure;
        this.secureStart = secureStart;
        this.secureEnd = secureEnd;
    }
    
    
    public GGEP getNormalGGEP() {
        return normal;
    }
    
    public GGEP getSecureGGEP() {
        return secure;
    }
    
    public int getSecureStartIndex() {
        return secureStart;
    }
    
    public int getSecureEndIndex() {
        return secureEnd;
    }
    
    public int getNormalStartIndex() {
        return normalStart;
    }
    
    public int getNormalEndIndex() {
        return normalEnd;
    }
}
