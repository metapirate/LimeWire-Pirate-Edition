package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.Signature;
import java.security.SignatureException;

import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEPKeys;
import com.limegroup.gnutella.messages.GGEPParser;
import com.limegroup.gnutella.messages.SecureGGEPData;


/**
 * A ggep-based message that may have a specific return address.  It requires
 * a routing version number and must be secure.
 */
public class RoutableGGEPMessage extends AbstractVendorMessage implements SecureMessage, VendorMessage.ControlMessage {
    
    static final String RETURN_ADDRESS_KEY = "RA";
    static final String VERSION_KEY = "V";
    static final String TO_ADDRESS_KEY = "TO";

    /** Whether or not this message has been verified as secure. */
    private Status _secureStatus = Status.INSECURE;
    
    /**
     * The ggep field that this message is.
     */
    protected final GGEP ggep;
    
    /**
     * Secure ggep data.
     */
    private final SecureGGEPData secureData;
    
    /**
     * The return address of this message.
     */
    private final IpPort returnAddress;
    
    /**
     * The destination address of this message
     */
    private final IpPort destAddress;
    
    /**
     * The routing version of this message
     */
    private final long routableVersion;
    
    protected RoutableGGEPMessage(byte[] guid, byte ttl, byte hops, 
            byte [] vendor, int selector, int version, byte[] payload, Network network)
    throws BadPacketException {
        super(guid, ttl, hops, vendor, selector, version, payload, network);

        // parse ggep
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(payload, 0);
        GGEP ggep = parser.getSecureGGEP();
        if (ggep == null) 
            throw new BadPacketException("no secure ggep");
        this.secureData = new SecureGGEPData(parser);
        ggep = parser.getNormalGGEP();
        if (ggep == null) // no ggep at all?
            throw new BadPacketException("no normal ggep");
        this.ggep = ggep;

        // get routable version 
        long routableVersion;
        try {
            routableVersion = ggep.getLong(VERSION_KEY);
        } catch (BadGGEPPropertyException bad){
            routableVersion = -1;
        }
        this.routableVersion = routableVersion;
        
        // get return address if any
        IpPort retAddr = null;
        try {
            byte [] returnAddress = ggep.get(RETURN_ADDRESS_KEY);
            if (returnAddress != null)
                retAddr = NetworkUtils.getIpPort(returnAddress, ByteOrder.LITTLE_ENDIAN);
        } catch (InvalidDataException bleh) {}
        this.returnAddress = retAddr;

        // get destination address if any
        IpPort destAddr = null;
        try {
            byte [] destAddress = ggep.get(TO_ADDRESS_KEY);
            if (destAddress != null)
                destAddr = NetworkUtils.getIpPort(destAddress, ByteOrder.LITTLE_ENDIAN);
        } catch (InvalidDataException bleh) {}
        this.destAddress = destAddr;
    }
    
    protected RoutableGGEPMessage(byte [] vendor, int selector, int version, GGEPSigner signer, GGEP ggep) {
        super(vendor, selector, version, derivePayload(signer, ggep));
        this.ggep = ggep;
        // nodes cannot create messages with custom return address or version.
        this.returnAddress = null;
        this.destAddress = null;
        this.routableVersion = -1;
        this.secureData = null;
    }
    
    private static byte [] derivePayload(GGEPSigner signer, GGEP ggep) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ggep.write(baos);
            signer.getSecureGGEP(ggep).write(baos);
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        }
        return baos.toByteArray();
    }
    
    /**
     * @return the address responses to this message should be sent to.
     * null if none was present.
     */
    public IpPort getReturnAddress() {
        return returnAddress;
    }
    
    /**
     * @return the address this message was intended to go to.
     * null if none was present.
     */
    public IpPort getDestinationAddress() {
        return destAddress;
    }
    
    /**
     * @return the routable version of this message
     * -1 if none was present
     */
    public long getRoutableVersion() {
        return routableVersion;
    }

    public byte[] getSecureSignature() {
        SecureGGEPData sg = secureData;
        if(sg != null) {
            try {
                return sg.getGGEP().getBytes(GGEPKeys.GGEP_HEADER_SIGNATURE);
            } catch(BadGGEPPropertyException bgpe) {
                return null;
            }
        } else {
            return null;
        }
    }

    public Status getSecureStatus() {
        return _secureStatus;
    }

    public void setSecureStatus(Status secureStatus) {
        _secureStatus = secureStatus;
    }

    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
        SecureGGEPData sg = secureData;       
        if(sg != null) {
            signature.update(getPayload(), 0, sg.getStartIndex());
            int end = sg.getEndIndex();
            int length = getPayload().length - end;
            signature.update(getPayload(), end, length);
        }
        
    }
    
    public static interface GGEPSigner {
        /**
         * @param original the ggep to be signed
         * @return a secure ggep containing signature of the signed ggep.
         */
        GGEP getSecureGGEP(GGEP original);
    }
    
}
