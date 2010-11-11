package com.limegroup.gnutella;

import org.limewire.io.GUID;

/**
 * A map of one GUID to another.
 * This supports the ability to declare that the mapping should expire
 * at a certain time.  If no time is specified, the mapping may expire
 * at a default interval.
 */
public interface GuidMap {

    /** Adds a mapping between the original guid and the new GUID. */
    public void addMapping(byte[] origGUID, byte[] newGUID);
    
    /**
     * Adds a mapping between the original guid and the new GUID.
     * The mapping should expire at the given interval.
     */
    public void addMapping(byte[] origGUID, byte[] newGUID, long expireInterval);    

    /** Gets the origGUID, given the newGUID. */
    public byte[] getOriginalGUID(byte[] newGUID);
    
    /** Gets the newGUID, given the origGUID. */
    public GUID getNewGUID(GUID origGUID);
}
