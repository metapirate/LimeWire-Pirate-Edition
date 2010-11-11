package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**<p>
 * Implements a security token that can write itself to
 * an output stream and queries for its validity.
 * </p><p>
 * Subclasses must implement 
 * {@link #getFromMAC(byte[], org.limewire.security.SecurityToken.TokenData)}.
 * <code>getFromMAC</code> returns the payload of 
 * the security token as it will appear on the network. 
 * </p><p>
 * See message authentication code, <a 
 * href="http://en.wikipedia.org/wiki/Message_authentication_code">MAC</a>, for 
 * more information. </p>
 */
public abstract class AbstractSecurityToken implements SecurityToken {

    private final MACCalculatorRepositoryManager mgr;
    
    /** 
     * The encrypted data.
     */
    private final byte[] _securityToken;
    
    protected AbstractSecurityToken(TokenData data, MACCalculatorRepositoryManager mgr) {
        this.mgr = mgr;
        _securityToken = getFromMAC(mgr.getMACBytes(data), data);
    }
    
    protected AbstractSecurityToken(byte [] network, MACCalculatorRepositoryManager mgr) throws InvalidSecurityTokenException {
        this.mgr = mgr;
        if (!isValidBytes(network)) {
            throw new InvalidSecurityTokenException("invalid data: " + Arrays.toString(network));
        }
        
        _securityToken = network;
    }
    
    public final boolean isFor(TokenData data) {
        if(!isValidTokenData(data)) {
            return false;
        }
        
        Iterable<byte[]> tokens = mgr.getAllBytes(data);
        for (byte[] token : tokens) {
            if (Arrays.equals(_securityToken, getFromMAC(token, data))) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determines if the given <code>TokenData</code> is valid for this 
     * <code>SecurityToken</code>. By default, all <code>TokenData</code>s are 
     * valid.
     */
    protected boolean isValidTokenData(TokenData data) {
        return true;
    }

    public final void write(OutputStream os) throws IOException {
        os.write(_securityToken);
    }

    public final byte[] getBytes() {
        byte[] copy = new byte[_securityToken.length];
        System.arraycopy(_securityToken, 0, copy, 0, _securityToken.length);
        return copy;
    }
    
    
   /**
     * @param MAC the calculated cryptographic MAC
     * @param data the <tt>TokenData</tt> this security token is created from.
     * @return the payload of this security token as it will appear on the network
     */
    protected abstract byte [] getFromMAC(byte [] MAC, TokenData data);
    
    /**
     * Determines if the given data bytes are valid.
     * By default, all non-null and non-empty bytes are valid.
     */
    protected boolean isValidBytes(byte[] network) {
        return network != null && network.length > 0;
    }
       
    
    /**
     * Should not be used if it is possible to call
     * {@link #isFor(org.limewire.security.SecurityToken.TokenData)} which
     * takes all possible {@link MACCalculatorRepository MACCalculatorRepositories}
     * into account.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SecurityToken) {
            SecurityToken t = (SecurityToken)obj;
            return Arrays.equals(_securityToken, t.getBytes());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(_securityToken);
    }

}