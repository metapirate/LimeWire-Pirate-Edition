package org.limewire.security;

import java.security.Signature;
import java.security.SignatureException;


/** 
 * Defines the interface to set a message's security state.
 */
public interface SecureMessage {
    
    public enum Status {
        /** A message that has not been verified.    */
        INSECURE,
        /** A message that was attempted to be verified but failed verification.  */
        FAILED,
        /** A message that was attempted to be verified and passed verification.  */
        SECURE
    }
    
    /** Sets whether or not the message is verified. */
    public void setSecureStatus(Status secureStatus);
    
    /** Determines if the message was verified. */
    public Status getSecureStatus();

    /** Returns the bytes of the signature from the secure GGEP block. */
    public byte[] getSecureSignature();
    
    /** Passes in the appropriate bytes of the payload to the signature. */
    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException;
}
