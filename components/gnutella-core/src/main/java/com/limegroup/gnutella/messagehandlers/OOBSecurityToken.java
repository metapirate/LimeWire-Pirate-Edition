package com.limegroup.gnutella.messagehandlers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import org.limewire.io.NetworkUtils;
import org.limewire.security.AbstractSecurityToken;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.ReplyHandler;

/**
 * A security token to be used in the OOB v3 protocol.
 */
public class OOBSecurityToken extends AbstractSecurityToken {
    

    /** 
     * Creates a security token with the provided data.
     * The query key consists of the # of results followed 
     * by the MAC checksum of the data object.
     */
    public OOBSecurityToken(OOBTokenData data, MACCalculatorRepositoryManager manager) {
        super(data, manager);
    }

    /**
     * Creates a key from data received from the network.  The first
     * byte is the # of results, the rest are the checksum to verify against.
     * @throws InvalidSecurityTokenException 
     */
    public OOBSecurityToken(byte[] network, MACCalculatorRepositoryManager manager) throws InvalidSecurityTokenException {
        super(network, manager);
    }
    
    @Override
    protected byte [] getFromMAC(byte[] b, TokenData data) {
        byte [] ret = new byte[b.length+1];
        ret[0] = (byte)((OOBTokenData)data).getNumRequests();
        System.arraycopy(b, 0, ret, 1, b.length);
        return ret;
    }
    
    /** Returns true if the data is an OOBTokenData. */
    @Override
    protected boolean isValidTokenData(TokenData data) {
        return data instanceof OOBTokenData;
    }
    
    
    public static class OOBTokenData implements SecurityToken.TokenData {        
        private final int numRequests;
        private final byte [] data;
        
        public OOBTokenData(ReplyHandler replyHandler, byte [] guid, int numRequests) {
            this(replyHandler.getInetAddress(), replyHandler.getPort(), guid, numRequests);        
        }
        
        public OOBTokenData(InetAddress address, int port, byte[] guid, int numRequests) {
            if (numRequests < 0 || numRequests > 255) {
                throw new IllegalArgumentException("requestNum to small or too large " + numRequests);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(35);
            DataOutputStream data = new DataOutputStream(baos);
            try {
                data.writeShort(port);
                if (NetworkUtils.isIPv6Compatible(address)) {
                    data.write(NetworkUtils.getIPv6AddressBytes(address));
                }
                else {
                    // unknown inet address, write its full address bytes
                    data.write(address.getAddress());
                }
                data.write(numRequests);
                data.write(guid);
            }
            catch (IOException ie) {
                ErrorService.error(ie);
            }
            this.data = baos.toByteArray();
            this.numRequests = numRequests;
            
        }
        public byte [] getData() {
            return data;
        }
        
        public int getNumRequests() {
            return numRequests;
        }
    }
}
