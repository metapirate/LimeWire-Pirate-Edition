package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.GUID;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

/** In Vendor Message parlance, the "message type" of this message is "LIME/20".
 *  Used to ask a Ultrapeer you are connected to to be your PushProxy.
 *  This message has no payload - we simply set the client guid as the GUID of
 *  the message.
 */
public final class PushProxyRequest extends AbstractVendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new PushProxyRequest from network data.
     */
    PushProxyRequest(byte[] guid, byte ttl, byte hops, int version, 
                     byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, 
              version, payload, network);

        if (getVersion() > VERSION) // we don't support it!!
            throw new BadPacketException("UNSUPPORTED VERSION");

        // there is no payload
    }


    /**
     * Constructs a new PushProxyRequest to be sent out.
     * @param guid Your client guid.  Used to route PushRequests to you.
     */
    public PushProxyRequest(GUID guid) {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, VERSION,
              DataUtils.EMPTY_BYTE_ARRAY);
        setGUID(guid);
    }

    /** The client GUID of the guy who wants to be PushProxied. 
     */
    public GUID getClientGUID() {
        return new GUID(getGUID());
    }

    /** Overridden purely for stats handling.
     */
    @Override
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }
}
