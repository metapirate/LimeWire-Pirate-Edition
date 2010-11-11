package org.limewire.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides <code>consumeAllInput</code> method that adds a data buffer to the
 * input stream.
 */
public final class ProcessUtils {
    
    private ProcessUtils() {}
    
    /**
     * Consumes all input from a Process. See also 
     * ProcessBuilder.redirectErrorStream()
     */
    public static void consumeAllInput(Process p) throws IOException {
        InputStream in = null;
        
        try {
            in = new BufferedInputStream(p.getInputStream());
            byte[] buf = new byte[1024];
            while(in.read(buf, 0, buf.length) >= 0);
        } finally {
            try {
                if(in != null) {
                    in.close();
                }
            } catch(IOException ignored) {}
        }
    }
}
