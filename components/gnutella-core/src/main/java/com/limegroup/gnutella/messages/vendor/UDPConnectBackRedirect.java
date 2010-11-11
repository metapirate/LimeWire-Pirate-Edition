package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;

/** In Vendor Message parlance, the "message type" of this VMP is "LIME/8".
 *  Used to ask a host that you are connected to to try and connect back to a
 *  3rd party via UDP.
 */
public final class UDPConnectBackRedirect extends AbstractVendorMessage {

    public static final int VERSION = 1;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;
    /** The payload has a 32-bit value - the host address - at which one should
     *  connect back.
     */
    private final InetAddress _addr;

    /**
     * Constructs a new UDPConnectBackRedirect with data from the network.
     */
    UDPConnectBackRedirect(byte[] guid, byte ttl, byte hops, int version, 
                           byte[] payload, Network network) 
        throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR, 
              version, payload, network);

        if ((getVersion() == 1) && (getPayload().length != 6))
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " +
                                         payload.length);
        // get the ip from the payload
        byte[] ip = new byte[4];
        System.arraycopy(getPayload(), 0, ip, 0, ip.length);
        if (!NetworkUtils.isValidAddress(ip))
            throw new BadPacketException("Bad Host!!");
        try {
            _addr = InetAddress.getByName(NetworkUtils.ip2string(ip));
        }
        catch (UnknownHostException uhe) {
            throw new BadPacketException("Bad InetAddress!!");
        }

        // get the port from the payload....
        _port = ByteUtils.ushort2int(ByteUtils.leb2short(getPayload(), 
                                                         ip.length));
        if (!NetworkUtils.isValidPort(_port))
            throw new BadPacketException("invalid port");
    }


    /**
     * Constructs a new UDPConnectBackRedirect to be sent out.
     * @param port the port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    public UDPConnectBackRedirect(GUID guid, InetAddress addr, int port) {
        super(F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR, VERSION, 
              derivePayload(addr, port));
        setGUID(guid);
        _addr = addr;
        _port = port;
    }

    /** You need this to connect back with a Pong with this guid.
     */
    public GUID getConnectBackGUID() {
        return new GUID(getGUID());
    }

    public InetAddress getConnectBackAddress() {
        return _addr;
    }

    public int getConnectBackPort() {
        return _port;
    }

    private static byte[] derivePayload(InetAddress addr, int port) {
        try {
            // i do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] ip = addr.getAddress();
            if(!NetworkUtils.isValidAddress(ip))
                throw new IllegalArgumentException("invalid addr: " + addr);
            baos.write(ip); // write _addr
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
