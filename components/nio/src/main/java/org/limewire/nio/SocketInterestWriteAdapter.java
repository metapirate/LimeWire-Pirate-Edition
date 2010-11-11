package org.limewire.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;

/**
 * Adapter that forwards InterestWriteChannel.interest(..)
 * calls on to NIODispatcher, as well as forwarding handleWrite
 * events to the last party that was interested.  All WritableByteChannel
 * calls are delegated to the SocketChannel.
 */
class SocketInterestWriteAdapter implements InterestWritableByteChannel {
    
    /** the last party that was interested.  null if none. */
    private volatile WriteObserver interested;
    /** the SocketChannel this is proxying. */
    private SocketChannel channel;
    /** whether or not we're shutdown. */
    private boolean shutdown = false;
    
    /** Constructs a new SocketInterestWriteAdapater */
    SocketInterestWriteAdapter(SocketChannel channel) {
        this.channel = channel;
    }
    
    /** Writes the buffer to the underlying SocketChannel, returning the amount written. */
    public int write(ByteBuffer buffer) throws IOException {
        return channel.write(buffer);
    }
    
    /** Closes the SocketChannel */
    public void close() throws IOException {
        channel.close();
    }
    
    /** Determines if the SocketChannel is open */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Marks the given observer as either interested or not interested in receiving
     * write events from the socket.
     */
    public synchronized void interestWrite(WriteObserver observer, boolean on) {
        if(!shutdown) {
            interested = on ? observer : null;
            NIODispatcher.instance().interestWrite(channel, on);
        }
    }
    
    /**
     * Forwards the write event to the last observer who was interested.
     */
    public boolean handleWrite() throws IOException {
        WriteObserver chain = interested;
        if(chain != null) 
            return chain.handleWrite();
        else
            return false;
    }
    
    /**
     * Shuts down the next link if the chain, if there is any.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
            shutdown = true;
        }

        Shutdownable chain = interested;
        if(chain != null)
            chain.shutdown();
        interested = null;
    }
    
    /** Unused, Unsupported. */
    public void handleIOException(IOException x) {
        throw new RuntimeException("unsupported", x);
    }

    public boolean hasBufferedOutput() {
        return false;
    }
    
}