package com.limegroup.gnutella.util;

import java.util.List;

/**
 * Encapsulates code dealing with FEC.
 */
public interface FECUtils {
    /**
     * @param data the data to encode
     * @param packetSize how large each packet should be
     * @param redundancy number over 1.0f 
     * @return the data packets in order.
     */
    public List<byte[]> encode(byte [] data, int packetSize, float redundancy);
    
    /**
     * @param packets the received packets.  Missing packets are null.
     * @param size the size of the decoded data
     * @return the decoded data, null on failure
     */
    public byte [] decode(List<byte[]> packets, int size);
}
