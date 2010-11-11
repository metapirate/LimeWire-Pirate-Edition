package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/4".
 *  Used to ask a host you connect to to not send queries above the specified
 *  hops value....
 */
public final class HopsFlowVendorMessage extends AbstractVendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new HopsFlowVendorMessage with data from the network.
     */
    HopsFlowVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                          byte[] payload, Network network)
        throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_HOPS_FLOW, version,
              payload, network);
        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        if (getPayload().length != 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         getPayload().length);
    }

    /**
     * Constructs a new HopsFlowVendorMessage to be sent out.
     *  @param hopVal represents the upper bound value for hops that you wish to
     *  see in queries from the neighbor you send this to.  Only queries whose 
     *  hops are STRICTLY lower than hopVal are expected to be received.  A 
     *  hopVal of 0 means that NO queries should be sent at all.  A hopVal of 1
     *  would mean that only queries from the immediate neighbor should be sent.
     */
    public HopsFlowVendorMessage(byte hopVal) {
        super(F_BEAR_VENDOR_ID, F_HOPS_FLOW, VERSION, derivePayload(hopVal));
    }

    /** @return a int representing the upper bound (exclusive) that the
     *  connection you received this on wants to see from you.
     */
    public int getHopValue() {
        return ByteUtils.ubyte2int(getPayload()[0]);
    }

    /**
     * Constructs the payload of the message, given the desired hops value.
     */
    private static byte[] derivePayload(byte hopVal) {
        byte[] payload = new byte[1];
        payload[0] = hopVal;
        return payload;
    }


    /** Overridden purely for stats handling.
     */
    @Override
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }
}
