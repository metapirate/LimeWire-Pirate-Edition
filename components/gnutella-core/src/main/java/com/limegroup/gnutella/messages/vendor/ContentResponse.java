/**
 * 
 */
package com.limegroup.gnutella.messages.vendor;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEPKeys;

/**
 * A response of content.
 */
public class ContentResponse extends AbstractVendorMessage {

    public static final int VERSION = 1;
    
    /** The ggep contained in here. */
    private GGEP ggep;

    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload, Network network) 
      throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_RESP, version, payload, network);
        if (getPayload().length < 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
        
        try {
            this.ggep = new GGEP(getPayload(), 0);
        } catch(BadGGEPBlockException bgbe) {
            throw new BadPacketException(bgbe);
        }
    }
    
    /**
     * Constructs a new ContentRequest for the given SHA1 URN.
     */
    public ContentResponse(URN sha1, boolean okay) {
        super(F_LIME_VENDOR_ID, F_CONTENT_RESP, VERSION, derivePayload(sha1, okay));
        
        try {
            this.ggep = new GGEP(getPayload(), 0);
        } catch(BadGGEPBlockException bgbe) {
            ErrorService.error(bgbe);
        }        
    }

    /**
     * Constructs the payload from given SHA1 Urn & okay flag.
     */
    private static byte[] derivePayload(URN sha1, boolean okay) {
        if(sha1 == null)
            throw new NullPointerException("null sha1");
        
        GGEP ggep =  new GGEP();
        // TODO use bytes instead of String, or pack into GUID.
        ggep.put(GGEPKeys.GGEP_HEADER_SHA1, sha1.getBytes());
        ggep.put(GGEPKeys.GGEP_HEADER_SHA1_VALID, okay ? 1 : 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        return out.toByteArray();
    }
    
    /**
     * Gets the URN this msg is for.
     */
    public URN getURN() {
        try {
            return URN.createSHA1UrnFromBytes(ggep.getBytes(GGEPKeys.GGEP_HEADER_SHA1));
        } catch(IOException iox) {
            return null;
        } catch(BadGGEPPropertyException bgpe) {
            return null;
        }
    }
    
    /**
     * Gets the 'ok' flag for the URN.
     */
    public boolean getOK() {
        try {
            // nonzero values are okay.
            return ggep.getInt(GGEPKeys.GGEP_HEADER_SHA1_VALID) != 0;
        } catch(BadGGEPPropertyException bgpe) {
            return false;
        }
    }
}
