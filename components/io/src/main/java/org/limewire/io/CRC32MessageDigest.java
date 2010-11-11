package org.limewire.io;

import java.security.MessageDigest;
import java.util.zip.CRC32;

/**
 * Provides a low overhead message digest. Though a CRC is a type of hash 
 * function that takes input of unlimited size and produces output of a fixed 
 * size, it is not a message digest. The <code>MessageDigest</code> hash 
 * functions, such as MD5 or SHA-1, include additional security properties not 
 * present with a CRC. 
 *<pre>         
 *          Output Size in bits
 *      CRC                 32
 *      MD5                 128
 *      SHA-1               160
 *</pre>
 * <p>
 * See <a href ="http://en.wikipedia.org/wiki/Cryptographic_hash_function">
 * Cryptographic hash function</a> for more information. 
 * 
 */

public class CRC32MessageDigest extends MessageDigest {
    
    private final CRC32 crc = new CRC32();
    
    public CRC32MessageDigest() {
        super("CRC32");
    }

    @Override
    protected int engineGetDigestLength() {
        return 4;
    }
    
    @Override
    protected byte[] engineDigest() {
        long value = crc.getValue();
        byte[] digest = {
            (byte)((value >> 24) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >>  8) & 0xFF),
            (byte)((value      ) & 0xFF)
        };
        return digest;
    }

    @Override
    protected void engineReset() {
        crc.reset();
    }

    @Override
    protected void engineUpdate(byte input) {
        crc.update(input & 0xFF);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        crc.update(input, offset, len);
    }
}
