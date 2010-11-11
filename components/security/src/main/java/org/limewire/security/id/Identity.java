package org.limewire.security.id;

import java.math.BigInteger;
import java.security.PublicKey;

import org.limewire.io.GUID;

/** 
 * An Identity of a node includes 4 fields:
 * the node's signature public key
 * the node's GUID generated using the public key
 * the node's Diffie-Hellman public component for key agreement
 * a signature covering the above fields. 
 * 
 * All the 4 fields are public information and should all be sent to 
 * remote nodes if requested. 
 */
public interface Identity {
    /**
     * @return the local node's secure GUID
     */
    public abstract GUID getGuid();

    /**
     * @return the local node's signature public key
     */
    public abstract PublicKey getPublicSignatureKey();

    /**
     * @return the local node's Diffie-Hellman public component 
     */
    public abstract BigInteger getPublicDiffieHellmanComponent();

    /**
     * @return the local node's signature on the other fields of the identity.
     */
    public abstract byte[] getSignature();
}