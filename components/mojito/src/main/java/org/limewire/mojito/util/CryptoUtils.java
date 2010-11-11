/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.zip.GZIPInputStream;

import org.limewire.mojito.exceptions.SignatureVerificationException;
import org.limewire.util.Base32;


/**
 * Miscellaneous utilities for Cryptography.
 */
public final class CryptoUtils {
    
    /** The algorithm of the Key */
    public static final String KEY_ALGORITHM = "DSA";
    
    /** The key size in bit */
    public static final int KEY_SIZE = 1024;
    
    /** The Signature algorithm */
    public static final String SIGNATURE_ALGORITHM = "SHA1withDSA";
    
    private static Signature SIGNATURE;
    
    private CryptoUtils() {}
    
    /**
     * Loads a PublicKey from the given File.
     */
    public static PublicKey loadPublicKey(File file) 
            throws IOException, SignatureException, InvalidKeyException {
        
        FileInputStream fis = null;
        GZIPInputStream gz = null;
        try {
            fis = new FileInputStream(file);
            gz = new GZIPInputStream(fis);
            return loadPublicKey(gz);
        } finally {
            if (gz != null) { gz.close(); }
        }
    }
    
    public static PublicKey loadPublicKey(String base32) 
            throws IOException, SignatureException, InvalidKeyException {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base32.decode(base32));
        GZIPInputStream gz = null;
        try {
            gz = new GZIPInputStream(bais);
            return loadPublicKey(gz);
        } finally {
            if (gz != null) { gz.close(); }
        }
    }
    
    public static PublicKey loadPublicKey(InputStream in) 
            throws IOException, SignatureException, InvalidKeyException {
        
        DataInputStream dis = new DataInputStream(in);
        
        byte[] signature = new byte[dis.readInt()];
        dis.readFully(signature);
        
        byte[] encodedKey = new byte[dis.readInt()];
        dis.readFully(encodedKey);
        
        PublicKey pubKey = createPublicKey(encodedKey);
        if (!verify(pubKey, signature, encodedKey)) {
            throw new SignatureVerificationException();
        }
        return pubKey;
    }
    
    /**
     * Creates a new KeyPair.
     */
    /*public static KeyPair createKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            kpg.initialize(KEY_SIZE);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }*/
    
    /**
     * Turns a X509 encoded key into a PublicKey object.
     */
    public static PublicKey createPublicKey(byte[] x509EncodedKey) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509EncodedKey);
            KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
            return factory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        } catch (InvalidKeySpecException err) {
            throw new RuntimeException(err);
        }
    }
    
    /**
     * Creates a Signature with the given PrivateKey that can be used 
     * for signing Data.
     */
    /*public static Signature createSignSignature(PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            return signature;
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }*/
    
    /**
     * Creates a Signature with the given PublicKey that can be used
     * for verifying Data.
     */
    /*public static Signature createVerifySignature(PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            return signature;
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }*/
    
    /**
     * Signs the given array of byte-arrays with the given PrivateKey.
     */
    public static synchronized byte[] sign(PrivateKey privateKey, byte[]... data)
            throws SignatureException, InvalidKeyException {
        
        try {
            if (SIGNATURE == null) {
                SIGNATURE = Signature.getInstance(SIGNATURE_ALGORITHM);
            }
            
            SIGNATURE.initSign(privateKey);
            
            for(byte[] d : data) {
                SIGNATURE.update(d, 0, d.length);
            }
            
            return SIGNATURE.sign();
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException(err);
        }
    }

    /**
     * Verifies given signature is correct.
     */
    public static synchronized boolean verify(PublicKey publicKey, byte[] signature, byte[]... data) 
            throws SignatureException, InvalidKeyException {
        
        if (signature == null) {
            return false;
        }
        
        try {
            if (SIGNATURE == null) {
                SIGNATURE = Signature.getInstance(SIGNATURE_ALGORITHM);
            }
            
            SIGNATURE.initVerify(publicKey);
            
            for(byte[] d : data) {
                SIGNATURE.update(d, 0, d.length);
            }
            
            return SIGNATURE.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
