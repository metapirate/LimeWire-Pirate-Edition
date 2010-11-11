package com.limegroup.gnutella.util;

import java.io.Serializable;

public class Data implements Serializable {
    
    public byte[] data =null;

    static final long serialVersionUID = 2238128677114591921L;
    
    public Data (byte[] b) {
        data = b;
    }
}
