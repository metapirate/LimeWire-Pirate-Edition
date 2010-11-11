package org.limewire.security;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.util.ByteUtils;

/**
 * A token that embeds an IP address and port, allowing one side to 
 * generate a token for a specific IP address/port pair that cannot be easily 
 * guessed.
 */
public final class AddressSecurityToken extends AbstractSecurityToken {

    /** As detailed by the GUESS specification 
     * (<a href="https://www.limewire.org/fisheye/viewrep/~raw,r=1.2/limecvs/core/guess_01.html">
     * Gnutella UDP Extension for Scalable Searches</a> ). */
    public static final int MIN_QK_SIZE_IN_BYTES = 4;
    /** As detailed by the GUESS specification. */
    public static final int MAX_QK_SIZE_IN_BYTES = 16;
    
    /** Cached value to make hashCode() much faster. */
    private final int _hashCode;

    /** Generates a <code>AddressSecurityToken</code> for a given 
     * <code>SocketAddress</code>. For a given <code>SocketAddress</code>, 
     * using a different SecretKey and/or SecretPad will result in a different 
     * <code>AddressSecurityToken</code>. 
     *  
     */
    public AddressSecurityToken (SocketAddress address, MACCalculatorRepositoryManager mgr) {
        this(((InetSocketAddress)address).getAddress(),
                ((InetSocketAddress)address).getPort(), 
                mgr);
    }
    
    /** Generates an <code>AddressSecurityToken</code> for a given IP:Port combo.
     *  For a given IP:Port combo, using a different SecretKey and/or SecretPad
     *  will result in a different <code>AddressSecurityToken</code>. 
     *  
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public AddressSecurityToken (InetAddress ip, int port, MACCalculatorRepositoryManager mgr) {
        this(new AddressTokenData(ip,port), mgr);
    }
    
    public AddressSecurityToken(AddressTokenData data,MACCalculatorRepositoryManager mgr) {
        super(data, mgr);
        _hashCode = genHashCode(getBytes());
    }    

    public AddressSecurityToken(byte[] key, MACCalculatorRepositoryManager mgr) throws InvalidSecurityTokenException {
        super(key.clone(), mgr);
        _hashCode = genHashCode(getBytes());
    }
    
    private int genHashCode(byte [] key) {
        // TODO: Can't we use Arrays.hashCode(byte[]) ???
        // While we have key in the CPU data cache, calculate _hashCode
        int code = 0x5A5A5A5A;
        // Mix all bits of key fairly evenly into code
        for (int i = key.length - 1; i >= 0; --i) {
            code ^= (0xFF & key[i]);
            // One-to-one mixing function from RC6 cipher:  
            // f(x) = (2*x*x + x) mod 2**N
            // We only care about the low-order 32-bits, so there's no
            // need to use longs to emulate 32-bit unsigned multiply.
            code = (code * ((code << 1) + 1));
            // Left circular shift (rotate) code by 5 bits
            code = (code >>> 27) | (code << 5);
        }
        return code;
    }
    
    @Override
    protected byte [] getFromMAC(byte [] key, TokenData ignored) {
        for (int i = key.length - 1; i >= 0; --i) {
            // The old prepareForNetwork() seemed to leave cobbs encoding to get
            // of nulls?  TODO: is it okay to leave nulls alone?
            if (key[i] == 0x1c) {
                key[i] = (byte) (0xFA);
            }
        }
        return key;
    }
    
    
    public boolean isFor(SocketAddress address) {
        InetAddress ip = ((InetSocketAddress)address).getAddress();
        int port = ((InetSocketAddress)address).getPort();
        return isFor(ip, port);
    }
    
    public boolean isFor(InetAddress ip, int port) {
        return isFor(new AddressTokenData(ip, port));
    }
    
    @Override
    public int hashCode() {
       return _hashCode;
    }

    /** Returns a String with the <code>AddressSecurityToken</code> represented 
     * in hexadecimal.
     */
    @Override
    public String toString() {
        return "{AddressSecurityToken: " + (new BigInteger(1, getBytes())).toString(16) + "}";
    }

    //--------------------------------------
    //--- PUBLIC STATIC CONSTRUCTION METHODS

    /**
     * Determines if the bytes are valid for a <code>key</code>.
     */
    public static boolean isValidSecurityTokenBytes(byte[] key) {
        return key != null &&
               key.length >= MIN_QK_SIZE_IN_BYTES &&
               key.length <= MAX_QK_SIZE_IN_BYTES;
    }
    
    @Override
    protected boolean isValidBytes(byte [] key) {
        return isValidSecurityTokenBytes(key);
    }
    
    /** Converts the IP address and port into an encrypted <code>byte[]</code>. */
    public static class AddressTokenData implements SecurityToken.TokenData {
        protected final byte[] data;
        
        public AddressTokenData(SocketAddress address) {
            this(((InetSocketAddress)address).getAddress(),
            ((InetSocketAddress)address).getPort());
        }
        
        public AddressTokenData(InetAddress addr, int port) {
            // get all the input bytes....
            byte[] ipBytes = addr.getAddress();
            int ipInt = 0;
            // Load the first 4 bytes into ipInt in little-endian order,
            // with the twist that any negative bytes end up flipping
            // all of the higher order bits, but we don't care.
            for(int i=3; i >= 0; --i) {
                ipInt ^= ipBytes[i] << (i << 3);
            }
            
            // Start out with 64 bits |0x00|0x00|port(2bytes)|ip(4bytes)|
            // and encrypt it with our secret key material.
            data = new byte[8];
            ByteUtils.int2beb(port, data, 0);
            ByteUtils.int2beb(ipInt, data, 4);
            
        }
        
        public byte [] getData() {
            return data;
        }
    }
}
