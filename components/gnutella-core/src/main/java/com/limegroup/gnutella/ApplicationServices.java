package com.limegroup.gnutella;

public interface ApplicationServices {

    public byte[] getMyBTGUID();

    public byte[] getMyGUID();
    
    /**
     * Returns true if this was the first time this LimeWire
     * was launched, false otherwise.
     */
    public boolean isNewInstall();

    /**
     * Returns true if this launch of LimeWire is using a 
     * different version of LimeWire, than it was the previous launch.
     */
    public boolean isNewJavaVersion();
}