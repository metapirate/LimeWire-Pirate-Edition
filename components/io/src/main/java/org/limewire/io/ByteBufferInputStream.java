package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.limewire.util.BufferUtils;

/**
 * Wraps {@link ByteBuffer ByteBuffers} so it can be accessed as an
 * {@link InputStream}. <code>ByteBufferInputStream</code> contains an internal
 * buffer that contains bytes that may be read from the stream. 
 * <code>ByteBufferInputStream</code> is similar to {@link ByteArrayInputStream}, 
 * however it uses <code>ByteBuffer</code>. 
 * <p>
 * <code>ByteBufferInputStream</code> is not synchronized. 
 */
public class ByteBufferInputStream extends InputStream {

    /** An empty ByteBuffer */
    private static final ByteBuffer[] EMPTY = new ByteBuffer[0];

    /** The index of the current ByteBuffer */
    protected int index = 0;
    
    /** The index of the marked ByteBuffer */
    protected int mark = -1;
    
    /** The array of ByteBuffer(s) we're reading from */
    protected ByteBuffer[] buffers;
    
    /**
     * Creates a ByteBufferInputStream that is reading from
     * one or more ByteBuffer(s).
     */
    public ByteBufferInputStream(ByteBuffer... buffers) {
        this.buffers = buffers;
    }
    
    /**
     * Returns the underlying array of ByteBuffer(s).
     */
    public ByteBuffer[] getBuffers() {
        if (buffers == EMPTY) {
            throw new IllegalStateException("InputStream is closed");
        }
        
        return buffers;
    }
    
    @Override
    public int available() {
        int available = 0;
        for(int i = buffers.length-1; i >= index; --i) {
            available += buffers[i].remaining();
        }
        return available;
    }
    
    @Override
    public void close() {
        index = 0;
        mark = -1;
        buffers = EMPTY;
    }

    @Override
    public void mark(int readlimit) {
        if (index < buffers.length) {
            mark = index;
            for(int i = buffers.length-1; i >= mark; --i) {
                buffers[i].mark();
            }
        }
    }

    @Override
    public void reset() {
        if (mark != -1) {
            index = mark;
            for(int i = buffers.length-1; i >= index ; --i) {
                buffers[i].reset();
            }
            mark = -1;
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() {
        while(index < buffers.length) {
            ByteBuffer b = buffers[index];
            if (b.hasRemaining()) {
                return b.get() & 0xFF;
            }
            
            // Try next ByteBuffer
            ++index;
        }
        
        return -1;
    }
    
    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        
        if (index >= buffers.length) {
            return -1;
        } else if (index == buffers.length-1 
                && !buffers[index].hasRemaining()) {
            return -1;
        } else if (len == 0 || b.length == 0) {
            return (available() > 0 ? 0 : -1);
        }
        
        int r = 0;
        while(index < buffers.length) {
            ByteBuffer buf = buffers[index];
            
            // The number of bytes we can read
            int l = Math.min(buf.remaining(), len-r);
            
            // Do read
            buf.get(b, off+r, l);
            r += l;
            
            // Exit if done
            if (r == len) {
                break;
            }
            
            // Try next ByteBuffer
            ++index;
        }
        
        // Return EOF if no bytes were available
        return (r > 0 ? r : -1);
    }
    
    @Override
    public long skip(long n) {
        long s = 0L;
        while(index < buffers.length) {
            ByteBuffer b = buffers[index];
            
            // The number of bytes we can skip in this ByteBuffer
            long l = Math.min(b.remaining(), n-s);
            b.position(b.position() + (int)l);
            s += l;
            
            // Exit if done
            if (s == n) {
                break;
            }
            
            // Try next ByteBuffer
            ++index;
        }
        
        return s;
    }
    
    /** Reads as much as possible into the destination buffer. */
    public long read(ByteBuffer dst) {
        long read = 0L;
        while(index < buffers.length) {
            ByteBuffer b = buffers[index];
            if(b.hasRemaining())
                read += BufferUtils.transfer(b, dst, false);
            if(!dst.hasRemaining())
                break;
            // Try next ByteBuffer
            ++index;
        }
        return read;
    }
    
    /**
     * Returns a reference to the existing buffers if possible.
     * If not possible, returns a copy of the data in a new buffer.
     * <p>
     * This advances the read mark, as if the requested data has
     * been read.
     * <p>
     * If there wasn't enough data to place into the buffer, the
     * buffer is created with as much data as possible.
     */
    public ByteBuffer bufferFor(int length) {
        ByteBuffer ret = null;
        
        // make sure we advance to the first buffer with data left.
        // this lets the reference work correctly.
        while(index < buffers.length) {
            if(buffers[index].hasRemaining())
                break;
            index++;
        }
        
        if(index < buffers.length) {
            length = Math.min(length, available());
            ByteBuffer b = buffers[index];
            if(b.remaining() >= length ) {
                int oldLimit = b.limit();
                b.limit(b.position() + length);
                ret = b.slice();
                b.limit(oldLimit);
                b.position(b.position() + length);
            } else {
                ret = ByteBuffer.allocate(length);
                read(ret);
                ret.flip();
            }
        } else {
            ret = BufferUtils.getEmptyBuffer();
        }
        return ret;
    }
}
