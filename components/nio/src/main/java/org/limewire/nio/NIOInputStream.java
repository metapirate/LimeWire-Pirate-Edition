package org.limewire.nio;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestScatteringByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.timeout.ReadTimeout;
import org.limewire.nio.timeout.SoTimeout;
import org.limewire.util.BufferUtils;


/**
 * Manages reading data from the network & piping it to a blocking input stream.
 *
 * This uses a BufferInputStream that waits on a lock when no data is available.
 * The stream exposes a BufferLock that should be notified when data is available
 * to be read.
 *
 * InterestReadChannel is implemented so that future ReadObservers can take over
 * reading and use this NIOInputStream as a source channel to read any buffered
 * data.
 */

class NIOInputStream implements ChannelReadObserver, InterestScatteringByteChannel, ReadTimeout {
    
    private final Shutdownable shutdownHandler;
    private final SoTimeout soTimeoutHandler;
    private InterestReadableByteChannel channel;
    private BufferInputStream source;
    private volatile Object bufferLock;
    private ByteBuffer buffer;
    private boolean shutdown;
    private boolean lastFilled = false;
    
 
    /**
     * Constructs a class that will allow asynchronous read events to be
     * piped to an InputStream.
     * 
     * @param soTimeouter Socket object to use to retrieve the soTimeout for 
     *                    the input stream timing out while reading.
     * @param shutdowner  Object to shutdown when the InputStream is closed.
     * @param channel     Channel to do reads from.
     */
    NIOInputStream(SoTimeout soTimeouter, Shutdownable shutdowner, InterestReadableByteChannel channel) {
        this.soTimeoutHandler = soTimeouter;
        this.shutdownHandler = shutdowner;
        this.channel = channel;
    }
    
    /**
     * Creates the pipes & buffer.
     */
    synchronized NIOInputStream init() throws IOException {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("Already closed!");
        
        buffer = NIODispatcher.instance().getBufferCache().getHeap(); 
        source = new BufferInputStream(buffer, this, shutdownHandler, channel, this);
        bufferLock = source.getBufferLock();
        
        return this;
    }
    
    /**
     * Reads from this' channel (which is the temporary ByteBuffer,
     * not the SocketChannel) into the given buffer.
     */
    public int read(ByteBuffer toBuffer) {
        return BufferUtils.transfer(buffer, toBuffer);
    }
    
    public long read(ByteBuffer[] dst, int offset, int length) {
    	return BufferUtils.transfer(buffer,dst, offset, length, true);
    }
    
    public long read(ByteBuffer [] dst) {
    	return read(dst,0, dst.length);
    }
    
    /**
     * Retrieves the InputStream to read from.
     */
    synchronized InputStream getInputStream() throws IOException {
        if(buffer == null)
            init();
        
        return source;
    }
    
    /**
     * Notification that a read can happen on the SocketChannel.
     */
    public void handleRead() throws IOException {
        // This is a hack to allow TLSNIOSockets to handshake
        // without init'ing the whole InputStream.
        if(bufferLock == null) {
            int read = channel.read(BufferUtils.getEmptyBuffer());
            // We're trying to read into an empty buffer -- not any real data,
            // so if we read EOF, that means this connection is closed.
            // TODO: Make sure that this is correct!  Maybe a better way is to turn
            // read interest off, or do nothing and leave the responsibility to the
            // channel we're reading from?  Read interest shouldn't even be on unless
            // that channel requested it, or this was initialized...
            if(read == -1)
                throw new ClosedChannelException();
        } else {
            synchronized(bufferLock) {
                int read = 0;
                
                // read everything we can.
                while(buffer.hasRemaining() && (read = channel.read(buffer)) > 0);
                
                if(read == -1)
                    source.finished();
                
                // If there's data in the buffer, we're interested in writing.
                if(buffer.position() > 0 || read == -1)
                    bufferLock.notify();
        
                // if there's room in the buffer, we're interested in more reading ...
                // if not, we're not interested in more reading, but we should remember
                // that we just filled it up, so if someone notifies us they want to read,
                // we can immediately trigger a read.
                if(!buffer.hasRemaining()) {
                    lastFilled = true;
                    channel.interestRead(false);
                } else {
                    lastFilled = false;
                }
                
                // if we read EOF, no more stuff to read.
                if(read == -1) {
                    channel.interestRead(false);
                }
            }
        }
    }
    
    /** Notification from BufferInputStream that we want to try to read. */
    void readHappening() {
        synchronized(bufferLock) {
            if(lastFilled) {
                NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
                    public void run() {
                        try {
                            handleRead();
                        } catch(IOException iox) {
                            channel.interestRead(true);
                            shutdownHandler.shutdown();
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Shuts down all internal channels.
     * The SocketChannel should be shut by NIOSocket.
     */
    public synchronized void shutdown() {
        if(shutdown)
            return;
        shutdown = true;

        if(source != null)
            source.shutdown();
        
        if(buffer != null) {
            NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
                public void run() {
                    NIODispatcher.instance().getBufferCache().release(buffer);
                }
            });
        }
    }
    
    /** Unused */
    public void handleIOException(IOException iox) {
        throw new RuntimeException("unsupported operation", iox);
    }    
    
    /**
     * Does nothing, since this is implemented for ReadableByteChannel,
     * and that is used for reading from the temporary buffer --
     * there is no buffer to close in this case.
     */
    public void close() throws IOException {
    }
    
    /**
     * Always returns true, since this is implemented for ReadableByteChannel,
     * and the Buffer is always available for reading.
     */
    public boolean isOpen() {
        return true;
    }
    
    /**
     * Does nothing.
     */
    public void interestRead(boolean status) {
        channel.interestRead(status);
    }
    
    public InterestReadableByteChannel getReadChannel() {
        return channel;
    }
    
    public void setReadChannel(InterestReadableByteChannel newChannel) {
        this.channel = newChannel;
        synchronized(this) {
            // If we have a source already created, set its read channel.
            if(source != null) {
                source.setReadChannel(newChannel);
            }
        }
    }

    public long getReadTimeout() {
        try {
            return soTimeoutHandler.getSoTimeout();
        } catch(SocketException se) {
            return -1;
        }
    }
}
                
        
    