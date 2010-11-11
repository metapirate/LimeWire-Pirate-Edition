package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;

/** In Vendor Message parlance, the "message type" of this VMP is "GTKG/7".
 *  Used to ask a host you connect to do a UDP ConnectBack.
 *
 *  VERSIONING INFO:
 *  -------------------------
 *  Version 2 of this message will fold the connect back guid into the guid
 *  of the message.  In order to transition, we should follow a 3 step process:
 *  1) allow this class to accept version 2 format
 *  2) after 1) has been released for a while, start using version 2
 *  3) some time after 2), stop accepting 1) (optional)
 */
public final class UDPConnectBackVendorMessage extends AbstractVendorMessage {

    public static final int VERSION = 2;

    /** The payload has a 16-bit unsigned value - the port - at which one should
     *  connect back.
     */
    private final int _port;
 
    /** The GUID that should be used for connect back.
     */
    private final GUID _guid;

    /** The encoding of the port and the guid.
     */
   
    /**
	 * The network constructor. 
	 */
    UDPConnectBackVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                                byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 
              version, payload, network);
              
        try {
            payload = getPayload();
            ByteArrayInputStream bais;
            switch(getVersion()) {
            case 1:
                if( payload.length != 18 )
                    throw new BadPacketException("invalid version1 payload");
                bais = new ByteArrayInputStream(payload);
                _port = ByteUtils.ushort2int(ByteUtils.leb2short(bais));
                byte[] guidBytes = new byte[16];
                bais.read(guidBytes, 0, guidBytes.length);
                _guid = new GUID(guidBytes);
                break;
            case 2:
                if( payload.length != 2 )
                    throw new BadPacketException("invalid version2 payload");
                bais = new ByteArrayInputStream(payload);
                _port = ByteUtils.ushort2int(ByteUtils.leb2short(bais));
                _guid = new GUID(super.getGUID());
                break;
            default:
                throw new BadPacketException("Unsupported Version");
            }

            if( !NetworkUtils.isValidPort(_port) )
                throw new BadPacketException("invalid connectback port.");
        }
        catch (IOException ioe) {
            throw new BadPacketException("Couldn't read from a ByteStream!!!");
        }
    }


    /**
     * Constructs a new UDPConnectBackVendorMessage to be sent out.
     * @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     *  @param guid The guid you want people to connect back with.  Serves as
     *  a flag that the connect back is 'unsolicited'.
     */
    public UDPConnectBackVendorMessage(int port, GUID guid) {
        super(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK, 1, 
              derivePayload(port, guid));
        _port = port;
        _guid = guid;
    }

    public int getConnectBackPort() {
        return _port;
    }

    public GUID getConnectBackGUID() {
        return _guid;
    }

    /**
     * Constructs the payload given the desired port & guid.
     */
    private static byte[] derivePayload(int port, GUID guid) {
        try {
            // do it during construction....
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteUtils.short2leb((short)port,baos); // write port
            baos.write(guid.bytes());
            return baos.toByteArray();
        } catch (IOException ioe) {
            ErrorService.error(ioe);
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
