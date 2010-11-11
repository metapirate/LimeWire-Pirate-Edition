package org.limewire.security.certificate;

import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

/**
 * Provides encrypt/decrypt support along with signing and verification.
 */
public interface CipherProvider {
    /**
     * Takes the given plaintext, and encrypts it with the given key. The cipher
     * type tells what to initialize the underlying {@link Cipher} as.
     */
    byte[] encrypt(byte[] plaintext, Key key, CipherType cipherType) throws IOException;

    /**
     * Takes the given ciphertext, and decrypts it with the given key.The cipher
     * type tells what to initialize the underlying {@link Cipher} as.
     */
    byte[] decrypt(byte[] ciphertext, Key key, CipherType cipherType) throws IOException;

    /**
     * @return the signature for the given plaintext using the given key and the
     *         given cipher type.
     */
    byte[] sign(byte[] plaintext, PrivateKey privateKey, SignatureType signatureType) throws IOException;

    /**
     * @return true if the given signature verifies, false if it fails.
     * @throws IOException thrown if there is a problem during the process, NOT
     *         related to verification.
     */
    boolean verify(byte[] plaintext, byte[] signature, PublicKey publicKey, SignatureType signatureType)
            throws IOException;

    public enum CipherType {
        RSA("RSA/ECB/PKCS1Padding"), AES("AES");

        private String description;

        /** The description to be passed to {@link Cipher#getInstance(String)}. */
        public String getDescription() {
            return description;
        }

        private CipherType(String description) {
            this.description = description;
        }
    }

    public enum SignatureType {
        SHA1_WITH_RSA("SHA1withRSA");

        private String description;

        /** The description to be passed to {@link Cipher#getInstance(String)}. */
        public String getDescription() {
            return description;
        }

        private SignatureType(String description) {
            this.description = description;
        }
    }
}
