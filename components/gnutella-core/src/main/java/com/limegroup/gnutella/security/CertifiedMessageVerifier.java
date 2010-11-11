package com.limegroup.gnutella.security;

import java.security.SignatureException;

import org.limewire.io.IpPort;

/**
 * Verifies certified messages.
 * <p>
 * Certified messages are message that contain a key version, a signature,
 * a payload signed by that signature and optionally a certificate with a 
 * public key to verify the message.
 * <p>
 * The certificate would have to be verified itself.
 */
public interface CertifiedMessageVerifier {
    
    /**
     * Verifies a <code>message</code> from <code>messageSource</code>
     * @param message the message to verify
     * @param messageSource the source from which the message was sent, used
     * for reporting, can be null
     * @return the certificate that was used for verifying the 
     * @throws SignatureException if the message does not verify
     */
    Certificate verify(CertifiedMessage message, IpPort messageSource) throws SignatureException;
    
    /**
     * Defines a certified message.
     */
    public interface CertifiedMessage {
        /**
         * @return a key version > -1 
         */
        int getKeyVersion();
        byte[] getSignature();
        byte[] getSignedPayload();
        Certificate getCertificate();
    }
}
