package org.limewire.security;

import java.security.SecureRandom;

import org.limewire.security.SecurityToken.TokenData;
import org.limewire.util.ByteUtils;


/* package */ class TEAMACCalculator implements MACCalculator {

    // This uses TEA (Tiny Encryption Algorithm) from
    // D. Wheeler and R. Needham of Cambridge University.
    // TEA is believed to be stronger than the DES cipher
    // used in the previous LW QK algorithm, is much faster
    // in software, and has an extremely small footprint in
    // the CPU's data and instruction caches.
    
    // Length of each security token that we generate, in bytes.
    protected final int ST_LENGTH = 4;
    
    // Pre- and post- encryption cyclic shift values
    // to get random padding equivalent to the previous
    // DES implementation.
    protected final int PRE_ROTATE;
    protected final int POST_ROTATE;
    
    // TEA encryption keys
    protected final int LK0;
    protected final int LK1;
    protected final int RK0;
    protected final int RK1;
    
    // Keys for "whitening" the encryption block before and
    // after encryption.  
    protected final long PRE_WHITEN_KEY;
    protected final long POST_WHITEN_KEY;
    
    
    /* package */ TEAMACCalculator() {
       SecureRandom rand = SecurityUtils.createSecureRandomNoBlock();
       PRE_WHITEN_KEY = rand.nextLong();
       POST_WHITEN_KEY = rand.nextLong();
      
       LK0 = rand.nextInt();
       LK1 = rand.nextInt();
       RK0 = rand.nextInt();
       RK1 = rand.nextInt();
       
       int rotations = rand.nextInt();
       PRE_ROTATE = rotations & 0x3F; // Low 6 bits
       POST_ROTATE = rotations >>> 26; // High 6 bits
    }
    
    // Constructor is package scoped for unit tests
    /* package */ TEAMACCalculator(int k0, int k1, int k2, int k3,
            int preRotate, int postRotate) {
        
        // Set cyclic shifts to allow getting at different
        // parts of the output
        PRE_ROTATE = preRotate & 0x3F;
        POST_ROTATE = postRotate & 0x3F;
        
        // No whitening, to expose TEA to tests
        PRE_WHITEN_KEY = POST_WHITEN_KEY = 0;
        
        // Use given TEA keys
        LK0 = k0; LK1 = k1; RK0 = k2; RK1 = k3;
    }
    
    /**
     * Returns the raw bytes for a AddressSecurityToken, which will not contain
     * 0x1C and 0x00, to accommodate clients that poorly parse GGEP.
     */
    public byte[] getMACBytes(TokenData tokenData) {
        byte [] data = tokenData.getData();
        long key64 = encryptCBCCMAC(data);
        
        // 32-bit QK gives attackers the least amount of information
        // about our secret key while still not making it worth their
        // while to try and make a botnet out of the Gnutella network
        byte[] qkBytes = new byte[ST_LENGTH];
        
        // Copy bytes that aren't 0x00 or 0x1C into output
        int outIndex = ST_LENGTH - 1;
        for (int left=(int)(key64 >> 32), right=(int)key64; outIndex>=0; left >>>= 8) {
            if (left == 0) {
                // get more data
                left = right;
                right = ~right; // This ensures loop termination
                // Worst case is 0x1CnNnNnN 0x1CnNnNnN, where nN is 0 or 1C.
            }
            int lowByte = left & 0xFF;
            if (lowByte != 0 && lowByte != 0x1C) {
                qkBytes[outIndex] = (byte) lowByte;
                --outIndex;
            }
        }
                              
        return qkBytes;   
    }
    
    /**
     * Encrypts the array of data using CBC-MAC with TEA as the block cipher. 
     */
    final long encryptCBCCMAC(byte[] data) {
        long tag = 0;
        for (int i = 0; i < data.length; i += 8) {
            tag = encrypt(tag ^ ByteUtils.leb2long(data, i, Math.min(8, data.length - i)));
        }
        return tag;
    }

    /**
     * Encrypts a 64-bit value using the TEA block cipher.
     * 
     * The role previously played by the variable padding
     * is now done by pre- and post-encryption cyclic
     * shifting, as well as pre- and post-encryption whitening.
     * 
     * @param block the 64-bit block to be encrypted
     * @return block encrypted using the secret key material
     *     within this class.
     */
    final long encrypt(long block) {
        block = (block << PRE_ROTATE) | (block >>> (64 - PRE_ROTATE));

        // Pre-encryption whitening
        block ^= PRE_WHITEN_KEY;

        // 32 cycle (a.k.a. 64 Feistel round) TEA encryption
        int left = (int) (block >> 32);
        int right = (int) block;
        for (int cycleCount = 32, roundKey = 0; cycleCount > 0; --cycleCount) {
            roundKey += 0x9E3779B9;
            left  += ((right<<4)+LK0) ^ (right+roundKey) ^ ((right>>>5)+LK1);
            right += (( left<<4)+RK0) ^ ( left+roundKey) ^ (( left>>>5)+RK1);
        }
        block = (((long) left) << 32) | (right & 0xFFFFFFFFL);

        // Post-encryption whitening
        block ^= POST_WHITEN_KEY;

        // Post-encryption cyclic shift
        return (block << POST_ROTATE) | (block >>> (64 - POST_ROTATE));
    }
}
