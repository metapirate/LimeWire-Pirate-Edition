package org.limewire.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

import org.limewire.util.FileUtils;

/** Provides a non-blocking cryptographically strong random number generator. */
public class SecurityUtils {
    
    private static SecureRandom rand = createSecureRandomNoBlock();
    
    /**
     * On some OSes, creating a new <code>SeucureRandom</code> instance
     * with the default constructor may block if the OS's
     * internal entropy pool runs low. On MS Windows, OS X, and Linux, and 
     * pretty much any modern Unix, this method will not block.
     */
    public static SecureRandom createSecureRandomNoBlock() {
        File urandom = new File("/dev/urandom");
        InputStream randStream = null;
        try {
            if (urandom.canRead()) {
                // OS X, Linux, FreeBSD, Solaris, etc.
                byte[] seed = new byte[32];
                randStream = new FileInputStream(urandom);
                for(int offset=0; offset < 32;) {
                    offset += randStream.read(seed, offset, 32-offset);
                }
                return new SecureRandom(seed);
            }
        } catch (SecurityException ignored) {
        } catch (IOException ignored) {
        } finally {
            FileUtils.close(randStream);
        }
        
        // Either we're on MS Windows, or some fringe OS that
        // doesn't have /dev/urandom or doesn't let normal
        // users use /dev/urandom.  In the Windows case, this
        // won't block.
        return new SecureRandom();
    }
    
    /**
     * @return a random number in 4 bytes as the nonce
     */    
    public static byte[] createNonce(){
        byte [] buf = new byte[4];
        rand.nextBytes(buf);        
        return buf;
    }
}
