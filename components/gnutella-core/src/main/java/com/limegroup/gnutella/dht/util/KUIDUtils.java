package com.limegroup.gnutella.dht.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.limewire.io.GUID;
import org.limewire.mojito.KUID;

import com.limegroup.gnutella.URN;

/**
 * Utilities to convert between {@link GUID}, {@link URN} and {@link KUID}.
 */
public class KUIDUtils {

    private KUIDUtils() {}
    
    /**
     * Converts the given SHA-1 URN into a KUID.
     */
    public static KUID toKUID(URN urn) {
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("Expected a SHA-1 URN: " + urn);
        }
        return KUID.createWithBytes(urn.getBytes());
    }
    
    /**
     * Converts the given KUID into a SHA-1 URN.
     */
    public static URN toURN(KUID kuid) {
        try {
            return URN.createSHA1UrnFromBytes(kuid.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns a KUID for the GUID (KUID = SHA-1(GUID)).
     */
    public static KUID toKUID(GUID guid) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(guid.bytes());
            byte[] digest = md.digest();
            return KUID.createWithBytes(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
