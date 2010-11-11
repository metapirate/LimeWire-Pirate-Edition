package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;

/** In Vendor Message parlance, the "message type" of this VMP is "BEAR/7".
 *  Used to ask a host you connect to do a TCP ConnectBack.
 */
public final class TCPConnectBackVendorMessage extends AbstractVendorMessage {

    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;

    /**
     * Constructs a new TCPConnectBackVendorMessage with data from the network.
     */
    TCPConnectBackVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                byte[] payload, Network network) 
        throws BadPacketException {
        super(guid, ttl, hops, F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, version,
              payload, network);

        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");

        if (getPayload().length != 2)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the port from the payload....
        _port = ByteUtils.ushort2int(ByteUtils.leb2short(getPayload(), 0));
        if( !NetworkUtils.isValidPort(_port) )
            throw new BadPacketException("invalid port");
    }


    /**
     * Constructs a new TCPConnectBackVendorMessage to be sent out.
     * @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public TCPConnectBackVendorMessage(int port) {
        super(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK, VERSION, 
              derivePayload(port));
        _port = port;
    }

    public int getConnectBackPort() {
        return _port;
    }

    /**
     * Constructs the payload given the desired port.
     */
    private static byte[] derivePayload(int port) {
        try {
            // i do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteUtils.short2leb((short)port,baos); // write _port
            return baos.toByteArray();
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }
    }

    /** Overridden purely for stats handling.
     */
    @Override
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

}
