package com.limegroup.gnutella.messages;

import org.limewire.io.GGEP;

/** Simple wrapper storing a GGEP block, it's start position & it's end position. */
public class SecureGGEPData {
    private final GGEP ggep;
    private final int start;
    private final int end;
    
    /** Constructs a SecureGGEPData using the given GGEP, start & end index. */
    public SecureGGEPData(GGEP ggep, int start, int end) {
        this.ggep = ggep;
        this.start = start;
        this.end = end;
    }
    
    /** Constructs a SecureGGEPData using the parser's secure info. */
    public SecureGGEPData(GGEPParser parser) {
        this.ggep = parser.getSecureGGEP();
        this.start = parser.getSecureStartIndex();
        this.end = parser.getSecureEndIndex();
    }
    
    public GGEP getGGEP() {
        return ggep;
    }
    
    public int getStartIndex() {
        return start;
    }
    
    public int getEndIndex() {
        return end;
    }

}
