package com.limegroup.gnutella.security;

import java.security.PublicKey;

import org.limewire.util.Objects;

/**
 * Immutable, threadsafe, implements value equality.
 * <p>
 * The string represenation of this certificate is:
 * <pre>
 * base32(signature)|keyVersion as integer literal|base32(public key)
 * </pre>
 */
class CertificateImpl implements Certificate {

    private final byte[] signature;
    private final byte[] signedPayload;
    private final int keyVersion;
    private final PublicKey publicKey;
    private final String certificateString;

    public CertificateImpl(byte[] signature, byte[] signedPayload, int keyVersion, PublicKey publicKey,
            String certificateString) {
        this.signature = signature;
        this.signedPayload = signedPayload;
        this.keyVersion = keyVersion;
        this.publicKey = publicKey;
        this.certificateString = Objects.nonNull(certificateString, "certificateString");
    }
    
    @Override
    public int getKeyVersion() {
        return keyVersion;
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public byte[] getSignedPayload() {
        return signedPayload;
    }

    @Override
    public String getCertificateString() {
        return certificateString;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Certificate)) {
            return false;
        }
        return certificateString.equals(((Certificate)obj).getCertificateString()); 
    }
    
    @Override
    public int hashCode() {
        return certificateString.hashCode();
    }
    
    @Override
    public String toString() {
        return certificateString;
    }
}
