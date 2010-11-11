package com.limegroup.gnutella.hashing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.limewire.io.IOUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Ability to parse and return the the start and end of the audio
 * portion of a file.
 */
public abstract class NonMetaDataHasher {
    
    protected static final Log LOG = LogFactory.getLog(NonMetaDataHasher.class);
    
    /**
     * Returns the position where the audio portion of the file begins.
     * If there was any problems locating this position in the file an
     * IOException is thrown.
     */
    public abstract long getStartPosition() throws IOException;
    
    /**
     * Returns the position where the audio portion of the file ends. 
     * If there was any problems locating this position in the file an
     * IOException is thrown.
     */
    public abstract long getEndPosition() throws IOException;
    
    /**
     * Opens the file and fills the buffer with the contents
     * located at the end of the file, filling the buffer's capacity.
     */
    protected static void fillBuffer(ByteBuffer buffer, File file, int rearOffset) throws IOException {
        FileInputStream fis = null;
        buffer.rewind();
        try {
            // using a FIS because we're reading once to a set
            // size buffer
            fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            if(fc.size() >= buffer.capacity() + rearOffset) {
                fc.position(fc.size() - buffer.capacity() - rearOffset);
            } else {
                fc.position(0);
            }
            int totalRead = 0;
            int read = 0;
            while(totalRead < buffer.capacity() && totalRead < fc.size()) {
                read = fc.read(buffer);
                totalRead += read;
                if(read == 0 || read == -1)
                    break;
            }
            buffer.limit(totalRead);
        } finally {
            IOUtils.close(fis);
        }
    }
}
