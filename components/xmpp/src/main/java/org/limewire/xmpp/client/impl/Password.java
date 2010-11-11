package org.limewire.xmpp.client.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.limewire.security.certificate.CipherProvider;

/**
 * This class encrypts/decrypts from/to a plaintext password using
 * symmetric key encryption (AES).  It does the following:
 * <p>
 * Given a plaintext password:
 * <pre>
 * 1. Generate symmetric encryption Key
 * 2. Encrypt password
 * 3. Store both key and password together, base64 encoded:
 *    base64 encoding ([1 byte key length][encryption key][1 byte passwd length][encrypted passwd])
 * </pre>
 * Given an encrypted block of data (key and password):
 * <pre>
 * 1. Base64 decode the encoded data
 * 2. Read in the key length
 * 3. Read in the key
 * 4. Use the key to decrypt the encrypted password, and return the password
 * </pre>
 *
*/
public final class Password {

    private static final CipherProvider.CipherType CIPHER_TYPE = CipherProvider.CipherType.AES;
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * The password to decrypt/encrypt
     * When isEncrypted is true, this represents the encrypted password
     * Otherwise, this is the plaintext password
     */
    private final String passwordString;
    private final boolean isEncrypted;

    private CipherProvider cipherProvider;


    Password(CipherProvider cipherProvider, String passwordString, boolean isEncrypted) {
        this.cipherProvider = cipherProvider;
        this.passwordString = passwordString;
        this.isEncrypted = isEncrypted;
    }

    public String encryptPassword() throws IOException, NoSuchAlgorithmException {

        if (isEncrypted) {
            throw new IllegalStateException("Password is already encrypted; Cannot encrypt.");
        }

        KeyGenerator kgen = KeyGenerator.getInstance(CIPHER_TYPE.getDescription());
        SecretKey key = kgen.generateKey();

        byte[] keyAsBytes = key.getEncoded();

        byte[] encrypted = cipherProvider.encrypt(
                passwordString.getBytes(DEFAULT_ENCODING), key, CIPHER_TYPE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeField(baos, keyAsBytes);
        writeField(baos, encrypted);

        byte[] magicBytes = baos.toByteArray();
        return new String(Base64.encodeBase64(magicBytes), DEFAULT_ENCODING);
    }

    public String decryptPassword() throws IOException, GeneralSecurityException {

        if (!isEncrypted) {
            throw new IllegalStateException("Password is not encrypted; Cannot decrypt.");    
        }

        byte[] magicBytes = Base64.decodeBase64(passwordString.getBytes(DEFAULT_ENCODING));


        ByteArrayInputStream bais = new ByteArrayInputStream(magicBytes);

        byte[] keyAsBytes;
        byte[] encryptedPassword;

        keyAsBytes = readField(bais);
        encryptedPassword = readField(bais);


        SecretKeySpec keySpec = new SecretKeySpec(keyAsBytes, CIPHER_TYPE.getDescription());

        byte[] pwdAsBytes = cipherProvider.decrypt(encryptedPassword, keySpec, CIPHER_TYPE);


        // should have reached the end - any extraneous bytes indicates corrupted bytes
        if (bais.available() > 0) {
            throw new IOException("Additional bytes after encryption key");
        }
        return new String(pwdAsBytes, DEFAULT_ENCODING);

    }

    private static void writeField(ByteArrayOutputStream baos, byte[] data) throws IOException {
        baos.write(data.length);
        baos.write(data);
    }

    private static byte[] readField(ByteArrayInputStream bais) throws IOException {

        // read length of key
        int fieldStatedLen = bais.read();

        if (fieldStatedLen <= 0) {
            throw new IOException("Corrupt key detected");
        }

        byte[] fieldBytes = new byte[fieldStatedLen];
        int actualFieldLen;
        try {
            actualFieldLen = bais.read(fieldBytes);
        } catch (IOException e) {
            throw new IOException("Corrupt key detected: " + e.getMessage());
        }


        if (actualFieldLen != fieldStatedLen) {
            throw new IOException("Corrupt key detected: Mismatch between " +
                    "stated key length and actual key length.");
        }
        return fieldBytes;
    }
}
