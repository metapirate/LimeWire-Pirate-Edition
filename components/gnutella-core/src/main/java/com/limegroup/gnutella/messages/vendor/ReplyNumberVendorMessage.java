package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/12".
 *  This message contains a unsigned byte (1-255) that tells you how many
 *  results the sending host has for the guid of a query (the guid of this
 *  message is the same as the original query).  The recieving host can ACK
 *  this message with a LimeACKVendorMessage to actually recieve the replies.
 *
 *  This message must maintain backwards compatibility between successive
 *  versions.  This entails that any new features would grow the message
 *  outward but shouldn't change the meaning of older fields.  This could lead
 *  to some issues (i.e. abandoning fields does not allow for older fields to
 *  be reused) but since we don't expect major changes this is probably OK.
 *
 *  Note that this behavior of maintaining backwards compatiblity is really
 *  only necessary for UDP messages since in the UDP case there is probably no
 *  MessagesSupportedVM exchange.
 */
public final class ReplyNumberVendorMessage extends AbstractVendorMessage {


    public static final int OLD_VERSION = 2;
    public static final int VERSION = 3;
    
    /**
     * whether we can receive unsolicited udp
     */
    protected static final byte UNSOLICITED=0x1;

    /**
     * Constructs a new ReplyNumberVendorMessages with data from the network.
     */
    ReplyNumberVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload, Network network) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
              payload, network);
        
        if (version < OLD_VERSION)
            throw new BadPacketException("ancient version");
        
        // only allow current version to come from network
        // unless OOBv2 is allowed
        if (version < VERSION) {
            if (SearchSettings.DISABLE_OOB_V2.getBoolean())
                throw new BadPacketException("OOB v2 not allowed");
            if (getPayload().length != 2)
                throw new BadPacketException("v2 message too large");
        }
        
        // loosen the condition on the message size to allow this message version
        // to have a GGEP in the future
        if (getPayload().length < 2)
            throw new BadPacketException("VERSION " + version+" UNSUPPORTED PAYLOAD LEN: " +
                    getPayload().length);
    }

    /**
     * Constructs a new ReplyNumberVendorMessage to be sent out.
     *  @param numResults The number of results (1-255 inclusive) that you have
     *  for this query.  If you have more than 255 just send 255.
     *  @param replyGUID The guid of the original query/reply that you want to
     *  send reply info for.
     */
    ReplyNumberVendorMessage(GUID replyGUID, int version, int numResults, boolean canReceiveUnsolicited) {
        super(F_LIME_VENDOR_ID, F_REPLY_NUMBER, version, derivePayload(numResults, canReceiveUnsolicited));
        setGUID(replyGUID);
    }
    
    /** Constructs the payload from the desired number of results. */
    private static byte[] derivePayload(int numResults, boolean canReceiveUnsolicited) {
        if ((numResults < 1) || (numResults > 255))
            throw new IllegalArgumentException("Number of results too big: " +
                                               numResults);
        byte[] bytes = new byte[2];
        ByteUtils.short2leb((short) numResults, bytes, 0);
        bytes[1] = canReceiveUnsolicited ? ReplyNumberVendorMessage.UNSOLICITED : 0x0;
        
        return bytes;
    }

    
    /** @return an int (1-255) representing the amount of results that a host
     *  for a given query (as specified by the guid of this message).
     */
    public int getNumResults() {
        return ByteUtils.ubyte2int(getPayload()[0]);
    }
    
    public boolean canReceiveUnsolicited() {
        if (getVersion() ==1) 
            return true;
        else 
            return (getPayload()[1] & UNSOLICITED) == UNSOLICITED;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ReplyNumberVendorMessage) {
            GUID myGuid = new GUID(getGUID());
            GUID otherGuid = new GUID(((VendorMessage) other).getGUID());
            int otherResults = 
                ((ReplyNumberVendorMessage) other).getNumResults();
            return ((myGuid.equals(otherGuid)) && 
                    (getNumResults() == otherResults) &&
                    super.equals(other));
        }
        return false;
    }

    /** Overridden purely for stats handling.
     */
    @Override
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }
    
    public boolean isOOBv3() {
        return getVersion() == VERSION;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append(", num results: ").append(getNumResults());
        builder.append(", canReceiveUnsolicited:").append(canReceiveUnsolicited());
        return builder.toString();
    }
}
