package org.limewire.security.certificate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.limewire.io.IOUtils;

import com.google.inject.Singleton;

@Singleton
public class CipherProviderImpl implements CipherProvider {

    public byte[] decrypt(byte[] ciphertext, Key key, CipherType cipherType) throws IOException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(cipherType.getDescription());
            AlgorithmParameters algParams = cipher.getParameters();
            cipher.init(Cipher.DECRYPT_MODE, key, algParams);
        } catch (GeneralSecurityException ex) {
            throw IOUtils.getIOException("Security exception while initializing: ", ex);
        }
        InputStream in = new ByteArrayInputStream(ciphertext);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CipherInputStream cin = new CipherInputStream(in, cipher);

        byte[] buffer = new byte[8];
        int bytesRead;
        while ((bytesRead = cin.read(buffer)) != -1)
            out.write(buffer, 0, bytesRead);
        in.close();
        cin.close();
        return out.toByteArray();
    }

    public byte[] encrypt(byte[] plaintext, Key key, CipherType cipherType) throws IOException {
        Cipher cp;
        try {
            cp = Cipher.getInstance(cipherType.getDescription());
            cp.init(Cipher.ENCRYPT_MODE, key);
        } catch (GeneralSecurityException ex) {
            throw IOUtils.getIOException("Security exception while initializing: ", ex);
        }

        InputStream in = new ByteArrayInputStream(plaintext);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CipherOutputStream cout = new CipherOutputStream(out, cp);

        byte[] buffer = new byte[8];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1)
            cout.write(buffer, 0, bytesRead);
        in.close();
        cout.close();
        return out.toByteArray();
    }

    public byte[] sign(byte[] plaintext, PrivateKey privateKey, SignatureType signatureType)
            throws IOException {
        try {
            Signature signer = Signature.getInstance(signatureType.getDescription());
            signer.initSign(privateKey);
            signer.update(plaintext);
            return signer.sign();
        } catch (NoSuchAlgorithmException ex) {
            throw IOUtils.getIOException("NoSuchAlgorithmException during signing: ", ex);
        } catch (InvalidKeyException ex) {
            throw IOUtils.getIOException("InvalidKeyException during signing: ", ex);
        } catch (SignatureException ex) {
            throw IOUtils.getIOException("SignatureException during signing: ", ex);
        }
    }

    public boolean verify(byte[] plaintext, byte[] signature, PublicKey publicKey,
            SignatureType signatureType) throws IOException {
        try {
            Signature signer = Signature.getInstance(signatureType.getDescription());
            signer.initVerify(publicKey);
            signer.update(plaintext);
            return signer.verify(signature);
        } catch (NoSuchAlgorithmException ex) {
            throw IOUtils.getIOException("NoSuchAlgorithmException during signing: ", ex);
        } catch (InvalidKeyException ex) {
            throw IOUtils.getIOException("InvalidKeyException during signing: ", ex);
        } catch (SignatureException ex) {
            throw IOUtils.getIOException("SignatureException during signing: ", ex);
        }
    }

}
