package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.util.DataUtils;

/** A Gnutella push request, used to download files behind a firewall. */
public class PushRequestImpl extends AbstractMessage implements PushRequest {
    
    private static final int STANDARD_PAYLOAD_SIZE=26;

    /** A null GGEP to mark a failed parsing. */
    private static final GGEP NULL_GGEP = new GGEP();
    
    /** The unparsed payload--because I don't care what's inside.
     *  NOTE: IP address is BIG-endian.
     */
    private byte[] payload;
    
    /** The GGEP, if parsed. */
    private GGEP ggep;

    /**
     * Wraps a PushRequest around stuff snatched from the network.
     * @exception BadPacketException the payload length is wrong
     */
    public PushRequestImpl(byte[] guid, byte ttl, byte hops,
             byte[] payload, Network network) throws BadPacketException {
        super(guid, Message.F_PUSH, ttl, hops, payload.length, network);
        if (payload.length < STANDARD_PAYLOAD_SIZE) {
            throw new BadPacketException("Payload too small: "+payload.length);
        }
        this.payload=payload;
		if(!NetworkUtils.isValidPort(getPort())) {
			throw new BadPacketException("invalid port");
		}
		String ip = NetworkUtils.ip2string(payload, 20);
		if(!NetworkUtils.isValidAddress(ip)) {
		    throw new BadPacketException("invalid address: " + ip);
		}
    }

    /**
     * Creates a new PushRequest from scratch.
     *
     * @requires clientGUID.length==16,
     *           0 < index < 2^32 (i.e., can fit in 4 unsigned bytes),
     *           ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *           0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     */
    public PushRequestImpl(byte[] guid, byte ttl,
               byte[] clientGUID, long index, byte[] ip, int port) {
    	this(guid, ttl, clientGUID, index, ip, port, Network.UNKNOWN);
    }
    
    /**
     * Creates a new PushRequest from scratch.  Allows the caller to 
     * specify the network.
     *
     * @requires clientGUID.length==16,
     *           0 < index < 2^32 (i.e., can fit in 4 unsigned bytes),
     *           ip.length==4 and ip is in <i>BIG-endian</i> byte order,
     *           0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     */
    public PushRequestImpl(byte[] guid, byte ttl,
            byte[] clientGUID, long index, byte[] ip, int port, Network network) {
        this(guid, ttl, clientGUID, index, ip, port, network, false);
    }
        
    /** Constructs a new PushRequest that optionally includes TLS data. */
    public PushRequestImpl(byte[] guid, byte ttl,
            byte[] clientGUID, long index, byte[] ip, int port, Network network, boolean useTLS) {
        super(guid, Message.F_PUSH, ttl, (byte)0, 0,network);
    	
    	if(clientGUID.length != 16) {
			throw new IllegalArgumentException("invalid guid length: "+
											   clientGUID.length);
		} else if((index&0xFFFFFFFF00000000l)!=0) {
			throw new IllegalArgumentException("invalid index: "+index);
		} else if(ip.length!=4) {
			throw new IllegalArgumentException("invalid ip length: " + ip.length);
        } else if(!NetworkUtils.isValidAddress(ip)) {
            throw new IllegalArgumentException("invalid ip "+NetworkUtils.ip2string(ip));
		} else if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentException("invalid port: "+port);
		}
        
        byte[] extra = DataUtils.EMPTY_BYTE_ARRAY;        
        if(useTLS)
            extra = PushGGEPHelper.TLS_GGEP;
        
        int payloadSize = STANDARD_PAYLOAD_SIZE + extra.length;
        payload=new byte[payloadSize];
        System.arraycopy(clientGUID, 0, payload, 0, 16);
        ByteUtils.int2leb((int)index,payload,16); //downcast ok
        payload[20]=ip[0]; //big endian
        payload[21]=ip[1];
        payload[22]=ip[2];
        payload[23]=ip[3];
        ByteUtils.short2leb((short)port,payload,24); //downcast ok
        System.arraycopy(extra, 0, payload, STANDARD_PAYLOAD_SIZE, extra.length);
        
        updateLength(payloadSize);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PushRequest#isTLSCapable()
     */
    public boolean isTLSCapable() {
        parseGGEP();
        if(ggep != null && ggep != NULL_GGEP) {
            return ggep.hasKey(GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
        } else {
            return false;
        }
    }
    
    /** Parses the GGEP block in a push, if one exists. */
    private void parseGGEP() {
        if(ggep == null && payload.length > STANDARD_PAYLOAD_SIZE) {
            try {
                ggep = new GGEP(payload, STANDARD_PAYLOAD_SIZE);
            } catch (BadGGEPBlockException e) {
                ggep = NULL_GGEP;
            }
        }
    }

    @Override
    protected void writePayload(OutputStream out) throws IOException {
		out.write(payload);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PushRequest#getClientGUID()
     */
    public byte[] getClientGUID() {
        byte[] ret=new byte[16];
        System.arraycopy(payload, 0, ret, 0, 16);
        return ret;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PushRequest#getIndex()
     */
    public long getIndex() {
        return ByteUtils.uint2long(ByteUtils.leb2int(payload, 16));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PushRequest#isFirewallTransferPush()
     */
    public boolean isFirewallTransferPush() {
        return (getIndex() == FW_TRANS_INDEX);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PushRequest#getIP()
     */
    public byte[] getIP() {
        byte[] ret=new byte[4];
        ret[0]=payload[20];
        ret[1]=payload[21];
        ret[2]=payload[22];
        ret[3]=payload[23];
        return ret;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PushRequest#getPort()
     */
    public int getPort() {
        return ByteUtils.ushort2int(ByteUtils.leb2short(payload, 24));
    }

	@Override
	public Class<? extends Message> getHandlerClass() {
	    return PushRequest.class;
	}
	
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PushRequest(");
        builder.append(super.toString()).append("\n");
        builder.append(NetworkUtils.ip2string(getIP())+":"+getPort()).append("\n");
        builder.append("FWT push: ").append(isFirewallTransferPush()).append("\n");
        builder.append("TLS: ").append(isTLSCapable()).append("\n");
        builder.append("Client GUID: ").append(GUID.toHexString(getClientGUID())).append("\n");
        return builder.toString();
    }
    
    /** A simple GGEP helper that precaches commonly used GGEPs. */
    private static class PushGGEPHelper {
        private static final byte[] TLS_GGEP;
        static {
            GGEP tlsGGEP = new GGEP();
            tlsGGEP.put(GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                tlsGGEP.write(out);
            } catch(IOException impossible) {
                ErrorService.error(impossible);
            }
            TLS_GGEP = out.toByteArray();
        }
        
        private PushGGEPHelper() {}
    }
}
