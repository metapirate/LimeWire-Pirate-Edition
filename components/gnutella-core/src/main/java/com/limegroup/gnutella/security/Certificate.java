package com.limegroup.gnutella.security;

import java.security.PublicKey;

/**
 * Encapusulates a certificate, not necessarily valid.
 * <p>
 * A certificate is a triple of: (signature, key version, public key). The
 * signature verifies that the key version and public key are actually correct
 * and authorized.
 */
public interface Certificate {

    public static final int IGNORE_ID = Integer.MAX_VALUE;
    
    /**
     * The signature of the certificate to verify its validity.
     */
    byte[] getSignature();
    /**
     * The public key issued by the  certificate 
     */
    PublicKey getPublicKey();
    /**
     * The key version of the certificate. Messages verified by this certificate
     * will have to contain this key version. 
     */
    int getKeyVersion();
    /**
     * The signed payload signed by {@link #getSignature()} which can be used
     * by a {@link CertificateVerifier} to verify the validity of this certificate. 
     */
    byte[] getSignedPayload();
    /**
     * The string representation of the certificate as it is potentially used
     * on the wire. 
     */
    String getCertificateString();
}
