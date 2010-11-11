package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.service.ErrorService;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * <p>
 * Eventually, it will be routed like a push request 
 * to firewalled alternate locations.
 * <p>
 * As long as the pinging host can receive solicited udp 
 * it can be firewalled as well.
 * <p>
 * Illustration of [firewalled] NodeA pinging firewalled host NodeB:
 * 
 * <xmp>
 * NodeA --------(PUSH_PING,udp)-------->Push
 *    <-------------------(udp)--------- Proxy
 *                                       /|\  | (tcp)
 *                                        |   |
 *                                        |  \|/
 *                                        NodeB
 * </xmp>
 */

public class HeadPing extends AbstractVendorMessage implements HeadPongRequestor {
    
    /*
     * Version 1: Initial revision.
     * Version 2: Signals support for understanding TLS info about push proxies of push altlocs.
     */
    
    /** The initial version; expected a binary (non-GGEP) HeadPong response. */
    private static final int EXPECTS_BINARY_RESPONSE_VERSION = 1;
    
    /** The current version. */
    public static final int VERSION = 2;

    /**
     * Requested content of the pong.
     */
    public static final int PLAIN = 0x0;

    public static final int INTERVALS = 0x1;

    public static final int ALT_LOCS = 0x2;

    public static final int PUSH_ALTLOCS = 0x4;

    public static final int FWT_PUSH_ALTLOCS = 0x8;

    public static final int GGEP_PING = 0x10;

    /**
     * A ggep field name containing the client guid of the node we would like
     * this ping routed to.
     */
    private static final String GGEP_PUSH = "PUSH";

    /**
     * the feature mask.
     */
    public static final int FEATURE_MASK = 0x1F;

    /** The URN of the file being requested */
    private final URN _urn;

    /** The format of the response that we desire */
    private final byte _features;

    /** The GGEP fields in this pong, if any */
    private GGEP _ggep;

    /**
     * The client GUID of the host we wish this ping routed to. null if pinging
     * directly.
     */
    private final GUID _clientGUID;

    /**
     * Creates a message object with data from the network.
     */
    protected HeadPing(byte[] guid, byte ttl, byte hops, int version, byte[] payload,
            Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload, network);

        // see if the payload is valid
        if (getVersion() == VERSION && (payload == null || payload.length < 42))
            throw new BadPacketException();

        _features = (byte) (payload[0] & FEATURE_MASK);

        // parse the urn string.
        String urnStr = StringUtils.getASCIIString(payload, 1, 41);
        if (!URN.isUrn(urnStr))
            throw new BadPacketException("udp head request did not contain an urn");
        try {
            _urn = URN.createSHA1Urn(urnStr);
        } catch (IOException oops) {
            throw new BadPacketException("failed to parse an urn");
        }

        // parse the GGEP if any
        if ((_features & GGEP_PING) == GGEP_PING) {
            if (payload.length < 43)
                throw new BadPacketException("no ggep was found.");
            try {
                _ggep = new GGEP(payload, 42, null);
            } catch (BadGGEPBlockException bpx) {
                throw new BadPacketException("invalid ggep block");
            }
        }

        // extract the client guid if any
        GUID clientGuid = null;
        if (_ggep != null) {
            if (_ggep.hasValueFor(GGEP_PUSH)) {
                try {
                    clientGuid = new GUID(_ggep.getBytes(GGEP_PUSH));
                } catch (BadGGEPPropertyException noGuid) {
                }
            }
        }

        _clientGUID = clientGuid;

    }

    /**
     * Creates a new udp head request.
     * 
     * @param sha1 the urn to get information about.
     * @param features which features to include in the response
     */

    public HeadPing(GUID g, URN sha1, int features) {
        this(g, sha1, null, features);
    }

    public HeadPing(GUID g, URN sha1, GUID clientGUID, int features) {
        super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION, derivePayload(sha1, clientGUID, features));
        _features = (byte) (features & FEATURE_MASK);
        _urn = sha1;
        _clientGUID = clientGUID;
        setGUID(g);
    }

    /**
     * Creates a plain udp head request.
     */
    public HeadPing(URN urn) {
        this(new GUID(GUID.makeGuid()), urn, PLAIN);
    }

    /**
     * Creates a duplicate ping with ttl and hops appropriate for a new vendor
     * message.
     */
    public HeadPing(HeadPing original) {
        super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, original.getVersion(), original.getPayload());
        _features = original.getFeatures();
        _urn = original.getUrn();
        _clientGUID = original.getClientGuid();
        setGUID(new GUID(original.getGUID()));
    }

    private static byte[] derivePayload(URN urn, GUID clientGUID, int features) {

        features = features & FEATURE_MASK;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);

        String urnStr = urn.httpStringValue();

        GGEP ggep = null;
        if (clientGUID != null) {
            features |= GGEP_PING; // make sure we indicate we'll have ggep.
            ggep = new GGEP();
            ggep.put(GGEP_PUSH, clientGUID.bytes());
        }

        try {
            daos.writeByte(features);
            daos.writeBytes(urnStr);
            if (ggep != null)
                ggep.write(daos);
        } catch (IOException huh) {
            ErrorService.error(huh);
        }

        return baos.toByteArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.messages.vendor.HeadPongRequestor#getUrn()
     */
    public URN getUrn() {
        return _urn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.messages.vendor.HeadPongRequestor#requestsRanges()
     */
    public boolean requestsRanges() {
        return (_features & INTERVALS) == INTERVALS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.messages.vendor.HeadPongRequestor#requestsAltlocs
     * ()
     */
    public boolean requestsAltlocs() {
        return (_features & ALT_LOCS) == ALT_LOCS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.messages.vendor.HeadPongRequestor#requestsPushLocs
     * ()
     */
    public boolean requestsPushLocs() {
        return (_features & PUSH_ALTLOCS) == PUSH_ALTLOCS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.messages.vendor.HeadPongRequestor#requestsFWTPushLocs
     * ()
     */
    public boolean requestsFWTOnlyPushLocs() {
        return (_features & FWT_PUSH_ALTLOCS) == FWT_PUSH_ALTLOCS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.messages.vendor.HeadPongRequestor#getFeatures()
     */
    public byte getFeatures() {
        return _features;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.messages.vendor.HeadPongRequestor#getClientGuid()
     */
    public GUID getClientGuid() {
        return _clientGUID;
    }

    public boolean isPongGGEPCapable() {
        return getVersion() > EXPECTS_BINARY_RESPONSE_VERSION;
    }

}
