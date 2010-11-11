package org.limewire.security.id;

import java.security.PrivateKey;

/**
 * PrivateIdentity contains the private information that a node 
 * uses to prove its identity and do key agreement. These information, 
 * however, shall not be send to other nodes. 
 * 
 * PrivateIdentiy also includes multiInstallationMark that is used 
 * together with the node's public signature key to generate the node's 
 * GUID.
 */
public interface PrivateIdentity extends Identity {
    
    /**
     * @return the local node's private signature key
     */
    public PrivateKey getPrivateSignatureKey();
    
    /**
     * @return the local node's private Diffie-Hellman key
     */
    public PrivateKey getPrivateDiffieHellmanKey();
    
    /**
     * @return an integer used to identity different installation of the same user 
     *  if the user shares the keys among her computers.  
     */
    public int getMultiInstallationMark();
    
    /**
     * @return Byte array of the local node's privateIdentity
     *  Note that the returned byte array should not be send to other nodes in the network. 
     */
    public byte[] toByteArray();
}
