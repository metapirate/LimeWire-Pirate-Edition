package org.limewire.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;

/**
 * An OutputStream that attempts to write from a Buffer.
 *
 * The stream must be notified when data is available in the buffer
 * to be read.
 */
 class BufferOutputStream extends OutputStream implements Shutdownable {
    
    //private static final Log LOG = LogFactory.getLog(BufferOutputStream.class);
    
    
    /** the lock that reading waits on. */
    private final Object LOCK = new Object();
    
    /** the handler to get for shutdown on close */
    private final Shutdownable handler;
    
    /** The WriteObserver to use when the channel is set for interest. */
    private final WriteObserver writeObserver;
    
    /** the buffer that has data for writing */
    private final ByteBuffer buffer;
    
    /** the InterestWriteChannel that the buffer is written from. */
    private final InterestWritableByteChannel channel;
    
    /** whether or not this stream has been shutdown. */
    private boolean shutdown = false;
    
    /**
     * Constructs a new BufferOutputStream that writes data to the given buffer.
     */
    BufferOutputStream(ByteBuffer buffer, Shutdownable handler,
                       WriteObserver observer, InterestWritableByteChannel channel) {
        this.handler = handler;
        this.buffer = buffer;
        this.channel = channel;
        this.writeObserver = observer;
    }
    
    /** Returns the lock object upon which writing into the buffer should lock */
    Object getBufferLock() {
        return LOCK;
    }
    
    /** Writes a single byte to the buffer. */
    @Override
    public void write(int x) throws IOException {
        synchronized(LOCK) {
            waitImpl();
            
            buffer.put((byte)(x & 0xFF));
            
            // there's data in the buffer now, the channel can write it.
            channel.interestWrite(writeObserver, true);
        }
    }
    
    /** Writes a chunk of data to the buffer */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        synchronized(LOCK) {
            while(len > 0) {
                waitImpl();
                
                int available = Math.min(buffer.remaining(), len);
                buffer.put(buf, off, available);
                off += available;
                len -= available;
            
                // now that there's data in the buffer, write with the channel
                channel.interestWrite(writeObserver, true);
            }
        }
    }
    
    /** Forces all data currently in the buffer to be written to the channel. */
    @Override
    public void flush() throws IOException {
        synchronized(LOCK) {
            // Since that adds no data to the buffer, we do not need to interest a write.
            // This simply waits until the existing buffer is emptied into the TCP stack,
            // via whatever mechanism normally clears the buffer (via writes).
            while(buffer.position() > 0) {
                if(shutdown)
                    throw new IOException("socket closed");
                
                try {
                    LOCK.wait();
                } catch(InterruptedException ix) {
                    throw new InterruptedIOException(ix);
                }
            }   
        }
    }
    
    /** Waits until there is space in the buffer to write to. */
    private void waitImpl() throws IOException {
        while(!buffer.hasRemaining()) {
            if(shutdown)
                throw new IOException("socket closed");
                
            try {
                LOCK.wait();
            } catch(InterruptedException ix) {
                throw new InterruptedIOException(ix);
            }
            
        }

        if(shutdown)
            throw new IOException("socket closed");
    }
    
    /** Closes this InputStream & the Socket that it's associated with */
    @Override
    public void close() {
        handler.shutdown();
    }
    
    /** Shuts down this socket */
    public void shutdown() {
        synchronized(LOCK) {
            shutdown = true;
            LOCK.notify();
        }
    }
    
}
    