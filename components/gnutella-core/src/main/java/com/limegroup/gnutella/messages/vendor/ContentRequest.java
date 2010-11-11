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
 * A request for content.
 */
public class ContentRequest extends AbstractVendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentRequest(byte[] guid, byte ttl, byte hops, int version, byte[] payload, Network network) 
      throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_REQ, version, payload, network);
        if (getPayload().length < 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
    }
    
    /**
     * Constructs a new ContentRequest for the given SHA1 URN.
     */
    public ContentRequest(URN sha1) {
        super(F_LIME_VENDOR_ID, F_CONTENT_REQ, VERSION, derivePayload(sha1));
    }

    /**
     * Constructs the payload from given SHA1 Urn.
     */
    private static byte[] derivePayload(URN sha1) {
        if(sha1 == null)
            throw new NullPointerException("null sha1");
        
        GGEP ggep =  new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_SHA1, sha1.getBytes());        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        return out.toByteArray();
    }
    
    /** Gets the URN -- this will inefficiently parse the GGEP each time it's called. */
    public URN getURN() {
        try {
            GGEP ggep = new GGEP(getPayload(), 0);
            return URN.createSHA1UrnFromBytes(ggep.getBytes(GGEPKeys.GGEP_HEADER_SHA1));
        } catch (BadGGEPBlockException e) {
        } catch (BadGGEPPropertyException e) {
        } catch(IOException iox) {
        }
        
        return null;
    }
}
