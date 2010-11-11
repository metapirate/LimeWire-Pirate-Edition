package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

public interface VendorMessageFactory {

    /**
     * Registers a VendorMessageParser under the provided selector (unsigned short) 
     * and Vendor ID.
     */
    public void setParser(int selector, byte[] vendorId,
            VendorMessageParser parser);

    /**
     * Returns a VendorMessageParser for the provided selector 
     * and vendor ID or null if no such parser is registered.
     */
    public VendorMessageParser getParser(int selector, byte[] vendorId);

    public VendorMessage deriveVendorMessage(byte[] guid, byte ttl, byte hops,
            byte[] fromNetwork, Network network) throws BadPacketException;

    /**
     * The interface for custom VendorMessageParser(s).
     */
    public static interface VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException;
    }
    
}