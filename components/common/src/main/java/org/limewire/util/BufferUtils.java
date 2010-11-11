package org.limewire.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Provides static methods for reading, transferring and compacting a
 * {@link ByteBuffer}.
 */
public class BufferUtils {
    
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    
    /** Retrieves an empty ByteBuffer. */
    public static ByteBuffer getEmptyBuffer() {
        return EMPTY_BUFFER;
    }
    
    /**
     * Cleans some data from the buffer.
     * Returns how much more needs to be deleted.
     * <p>
     * The data in buffer is expected to be in 'reading' position.
     * That is, position should be at the end of the data, limit should be capacity.
     */
    public static long delete(ByteBuffer buffer, long amountToDelete) {
        if (buffer.position() <= amountToDelete) {
            amountToDelete -= buffer.position();
            buffer.clear();
        } else {
            // begin: [ABCDEFG* ] where * is position, ] is limit and capacity
            buffer.flip();
            // now  : [*BCDEFG^ ] where * is position, ^ is limit, ] is capacity
            buffer.position((int)amountToDelete);
            // now  : [ABCD*FG^ ] where * is position, ^ is limit, ] is capacity
            buffer.compact();
            // end  : [EFG*     ] where * is position, ] is limit and capacity

            amountToDelete = 0;
        }
        return amountToDelete;
    }
    
    /**
     * Transfers all data from 'bufferSrc' to 'dst', then reads as much data
     * as possible from 'channelSrc' to 'dst'.
     * This returns the last amount of data that could be read from the channel.
     * It does NOT return the total amount of data transferred.
     * 
     * @return the last amount of data that could be read from the channel.
     * @throws IOException
     */
    public static int readAll(ByteBuffer bufferSrc, ReadableByteChannel channelSrc, ByteBuffer dst) throws IOException {
        transfer(bufferSrc, dst, true);
        int read = 0;
        while(dst.hasRemaining() && (read = channelSrc.read(dst)) > 0);
        return read;
    }
    
    /**
     * Transfers as much data as possible from src to dst.
     * The data in 'src' will be flipped prior to transferring & then compacted.
     * Returns however much data was transferred.
     * 
     * @return The amount of data transferred
     */
    public static int transfer(ByteBuffer src, ByteBuffer dst) {
        return transfer(src, dst, true);
    }

    /**
     * Transfers as much data as possible from src to dst.
     * Returns how much data was transferred.
     * The data in 'src' will NOT be flipped prior to transferring if needsFlip is false.
     * 
     * @param needsFlip whether or not to flip src
     * @return The amount of data transferred
     */
    public static int transfer(ByteBuffer src, ByteBuffer dst, boolean needsFlip) {
        int read = 0;
        if (src != null) {
            if (needsFlip) {
                if (src.position() > 0) {
                    src.flip();
                    read = doTransfer(src, dst);
                    if(src.hasRemaining())
                        src.compact();
                    else
                        src.clear();
                }
            } else {
                if (src.hasRemaining())
                    read = doTransfer(src, dst);
            }
        }
        
        return read;
    }
    
    public static long transfer(ByteBuffer from, ByteBuffer [] to,
    		int offset, int length, boolean needsFlip) {
    	long read = 0;
    	for (int i = offset; i < offset + length ;i++)
    		read += transfer(from, to[i], needsFlip);
    	
    	return read;
    }
    
    /**
     * Transfers data from 'src' to 'dst'.  This assumes that there
     * is data in src and that it is non-null.  This also assumes that
     * 'src' is already flipped & 'dst' is ready for writing.
     * 
     * @return The amount of data transferred
     */
    private static int doTransfer(ByteBuffer src, ByteBuffer dst) {
        int read = 0;
        int remaining = src.remaining();
        int toRemaining = dst.remaining();
        if(toRemaining >= remaining) {
            dst.put(src);
            read += remaining;
        } else {
            int limit = src.limit();
            int position = src.position();
            src.limit(position + toRemaining);
            dst.put(src);
            read += toRemaining;
            src.limit(limit);
        }
        return read;
    }
    
    /**
     * Reads data from <code>src</code>, inserting it into <code>dst</code>,
     * until a full line is read. Returns true if a full line is read, false if
     * more data needs to be inserted into the buffer until a full line can be
     * read.
     */
    public static boolean readLine(ByteBuffer src, StringBuilder dst) {
        int c = -1; //the character just read        
        while(src.hasRemaining()) {
            c = src.get();
            switch(c) {
                // if this was a \n character, we're done.
                case  '\n': return true;
                // if this was a \r character, ignore it.
                case  '\r': continue;                        
                // if it was any other character, append it to the buffer.
                default: dst.append((char)c);
            }
        }

        return false;
    }
    
    /**
     * Reads data from <code>src</code>, inserting it into <code>dst</code>.
     * 
     * @return number of bytes read
     */
    public static int transfer(ByteBuffer src, StringBuilder dst) {
        int written = 0;
        while (src.hasRemaining()) {
            dst.append((char) src.get());
            written++;
        }
        return written;
    }

}
