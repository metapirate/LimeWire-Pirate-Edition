package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Defines the interface to authenticate a host based on its IP address, port 
 * or other pieces of data.
 */
public interface SecurityToken {
    
    /**
     * Returns the <code>SecurityToken</code> as byte array.
     */
    public byte[] getBytes();
    
    /**
     * Writes the <code>SecurityToken</code> to the output stream.
     */
    public void write(OutputStream out) throws IOException;
    
    /** 
     * Validates that a <code>SecurityToken</code> was generated for the given 
     * <code>TokenData</code>.
     */
    public boolean isFor(TokenData data);
    
    /**
     * Defines a factory interface to create a {@link SecurityToken}.
     */
    public static interface TokenProvider {
        
        /**
         * Creates and returns a {@link SecurityToken} for the given 
         * <code>SocketAddress</code>.
         */
        public SecurityToken getSecurityToken(SocketAddress dst);
        
        /**
         * Creates and returns a {@link TokenData} for the given SocketAddress
         */
        public TokenData getTokenData(SocketAddress src);
    }
    
    /**<p> 
     * Defines the interface to get data as a byte[].
     * </p>
     * One use case for <code>TokenData</code> is to 
     * implement a constructor to convert the IP address in an encrypted way 
     * into the byte[]. Calls to get that data return the encrypted IP address.
     */
    public static interface TokenData {
        public byte[] getData();
    }
    
    /**
     * Creates a <code>SecurityToken</code> from a <code>SocketAddress</code>.
     */
    @Singleton
    public static class AddressSecurityTokenProvider implements TokenProvider {
        private final MACCalculatorRepositoryManager manager;
        
        @Inject
        public AddressSecurityTokenProvider(MACCalculatorRepositoryManager manager) {
            this.manager = manager;
        }
        
        public SecurityToken getSecurityToken(SocketAddress addr) {
            return new AddressSecurityToken(addr, manager);
        }

        public TokenData getTokenData(SocketAddress src) {
            return new AddressSecurityToken.AddressTokenData(src);
        }
    }
}
