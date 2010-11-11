package org.limewire.nio;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;

import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.timeout.ReadTimeout;

/**
 * An InputStream that attempts to read from a Buffer.
 *
 * The stream must be notified when data is available in the buffer
 * to be read.
 */
class BufferInputStream extends InputStream implements Shutdownable {
    
    //private static final Log LOG = LogFactory.getLog(BufferInputStream.class);
    
    
    /** the lock that reading waits on. */
    private final Object LOCK = new Object();
    
    /** The shutdownable to shutdown. */
    private final Shutdownable shutdownHandler;
    
    /** The ReadTimeout handler. */
    private final ReadTimeout readTimeoutHandler;
    
    /** the buffer that has data for reading */
    private final ByteBuffer buffer;
    
    /** The NIOInputStream to notify when we're about to read. */
    private final NIOInputStream nioStream;    
    
    /** the SelectableChannel that the buffer is read from. */
    private InterestReadableByteChannel channel;
    
    /** whether or not this stream has been shutdown. */
    private boolean shutdown = false;
    
    /** whether or not there's no data left to read on this stream. */
    private boolean finished = false;
   
    /**
     * Constructs a new BufferInputStream that reads from the given buffer,
     * using the given socket to retrieve the soTimeouts.
     */
    BufferInputStream(ByteBuffer buffer, ReadTimeout timeout,
            Shutdownable shutdown, InterestReadableByteChannel channel,
            NIOInputStream stream) {
        this.readTimeoutHandler = timeout;
        this.shutdownHandler = shutdown;
        this.buffer = buffer;
        this.channel = channel;
        this.nioStream = stream;
    }
    
    void setReadChannel(InterestReadableByteChannel newChannel) {
        synchronized(LOCK) {
            this.channel = newChannel;
        }
    }
    
    /** Returns the lock object upon which writing into the buffer should lock */
    Object getBufferLock() {
        return LOCK;
    }
    
    /** Marks this stream as finished -- having no data left to read. */
    void finished() {
        finished = true;
    }
    
    /** Reads a single byte from the buffer. */
    @Override
    public int read() throws IOException {
        synchronized(LOCK) {
            waitImpl();
            
            if(finished && buffer.position() == 0)
                return -1;
         
            buffer.flip();
            byte read = buffer.get();
            buffer.compact();
            
            // there's room in the buffer now, the channel needs some data.
            channel.interestRead(true);
            
            // must &, otherwise implicit cast can change value.
            // (for example, reading the byte -1 is very different than
            //  reading the int -1, which means EOF.)
            return read & 0xFF;
        }
    }
    
    /** Reads a chunk of data from the buffer */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (len == 0)
            return 0;
        synchronized(LOCK) {
            waitImpl();
            
            if(finished && buffer.position() == 0)
                return -1;
                
            buffer.flip();
            int available = Math.min(buffer.remaining(), len);
            buffer.get(buf, off, available);
            
            if (buffer.hasRemaining()) 
                buffer.compact();
            else 
                buffer.clear();
            
            // now that there's room in the buffer, fill up the channel
            channel.interestRead(true);
            
            return available; // the amount we read.
        }
    }
    
    /** Determines how much data can be read without blocking */
    @Override
    public int available() throws IOException {
        synchronized(LOCK) {
            return buffer.position();
        }
    }
    
    /** Waits the soTimeout amount of time. */
    private void waitImpl() throws IOException {
        long timeout = readTimeoutHandler.getReadTimeout();
        if(timeout == -1)
            throw new SocketException("unable to get read timeout");
        
        boolean looped = false;
        while(buffer.position() == 0 && !finished) {
            if(shutdown)
                throw new IOException("socket closed");
                
            if(looped && timeout != 0) 
                throw new java.net.SocketTimeoutException("read timed out (" + timeout + ")");
            
            
            nioStream.readHappening();
            
            try {
                LOCK.wait(timeout);
            } catch(InterruptedException ix) {
                throw new InterruptedIOException(ix);
            }

            looped = true;
        }

        if(shutdown)
            throw new IOException("socket closed");
    }
    
    /** Closes this InputStream & the Socket that it's associated with */
    @Override
    public void close() throws IOException  {
        shutdownHandler.shutdown();
    }
    
    /** Shuts down this socket */
    public void shutdown() {
        synchronized(LOCK) {
            shutdown = true;
            LOCK.notify();
        }
    }
    
}
    