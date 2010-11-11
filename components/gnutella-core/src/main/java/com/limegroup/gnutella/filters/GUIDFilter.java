package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;

/** 
 * Blocks GUIDs from runaway Qtrax queries.
 */
public class GUIDFilter implements SpamFilter {
    private final byte[] BAD_BYTES={
        (byte)0x41, (byte)0x61, (byte)0x42, (byte)0x62, (byte)0x5a};
    private final int BAD_BYTES_LENGTH=BAD_BYTES.length;

    /** Disallows m if it has the bad GUID. */
    public boolean allow(Message m) {
        byte[] guid=m.getGUID();
        for (int i=0; i<BAD_BYTES_LENGTH; i++) {
            if (guid[i]!=BAD_BYTES[i])
                return true;    //Does not match; allow.
        }
        return false;           //Does match; disallow.
    }           
}
