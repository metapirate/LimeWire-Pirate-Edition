package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

/**
 * This message allows leafs to enable or disable proxying for all or specific
 * versions of the out-of-band protocol.
 * 
 * * Versions between 1 and 255 are supported.
 * * The value 0 is reserved to enable proxying for all versions
 * * A message without payload disables proxying for all versions
 */
public class OOBProxyControlVendorMessage extends AbstractVendorMessage {

    public static final int VERSION = 1;
    
    public static enum Control {
        DISABLE_FOR_ALL_VERSIONS(-1),
        ENABLE_FOR_ALL_VERSIONS(0),
        DISABLE_VERSION_1(1),
        DISABLE_VERSION_2(2),
        DISABLE_VERSION_3(3),
        
        // here for testing purposes because of signed bytes
        DISABLE_VERSION_255(255);
        
        private int version;
        
        private Control(int version) {
            if (version > 255) {
                throw new IllegalArgumentException("version must be smaller than 256");
            }
            this.version = version;
        }
        
        public int getVersion() {
            return version;
        }
    }
    
    
    public OOBProxyControlVendorMessage(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_OOB_PROXYING_CONTROL, version, payload, network);
        if (getVersion() != VERSION) {
            throw new BadPacketException("Unsupported version or payload length");
        }
    }
    
    public OOBProxyControlVendorMessage(Control control) {
        super(F_LIME_VENDOR_ID, F_OOB_PROXYING_CONTROL, VERSION, derivePayload(control));
    }

    private static byte[] derivePayload(Control control) {
        if (control == Control.DISABLE_FOR_ALL_VERSIONS) {
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
        else {
            return new byte[] { (byte)control.getVersion() };
        }
    }
    
    public static OOBProxyControlVendorMessage createDoNotProxyMessage() {
        return new OOBProxyControlVendorMessage(Control.DISABLE_FOR_ALL_VERSIONS);
    }
    
    public static OOBProxyControlVendorMessage createDoProxyMessage() {
        return new OOBProxyControlVendorMessage(Control.ENABLE_FOR_ALL_VERSIONS);
    }

    /**
     * Returns {@link Integer#MAX_VALUE} when proxying is disabled for all versions,
     * otherwise the value of the highest protocol version for which proxying
     * is disabled.
     */
    public int getMaximumDisabledVersion() {
        byte[] payload = getPayload();
        if (payload.length == 0) {
            return Integer.MAX_VALUE;
        }
        else {
            return payload[0] & 0xFF;            
        }
    }
    
}
