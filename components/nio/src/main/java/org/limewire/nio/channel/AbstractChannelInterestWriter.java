package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;

/**
 * A basic channel writer that just forwards all information to the next channel
 * in line.
 */
public abstract class AbstractChannelInterestWriter implements ChannelWriter, InterestWritableByteChannel {

    /** The channel to write to & interest on. */    
    private volatile InterestWritableByteChannel channel;
    /** The next observer. */
    private volatile WriteObserver observer;
    
    
    /** {@inheritDoc} */
    public InterestWritableByteChannel getWriteChannel() {
        return channel;
    }
    
    /** {@inheritDoc} */
    public void setWriteChannel(InterestWritableByteChannel channel) {
        this.channel = channel;
        channel.interestWrite(this, true);
    }
    
    /**
     * {@inheritDoc}
     */
    public void interestWrite(WriteObserver observer, boolean status) {
        this.observer = status ? observer : null;
        InterestWritableByteChannel source = channel;
        if(source != null)
            source.interestWrite(this, status); 
    }
    
    /** Closes the underlying channel. */
    public void close() throws IOException {
        Channel source = channel;
        if(source != null)
            source.close();
    }
    
    /** Determines if the underlying channel is open. */
    public boolean isOpen() {
        Channel source = channel;
        return source != null ? source.isOpen() : false;
    }
    
    public void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }
    
    /** Unused, Unsupported. */
    public void handleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }

    public boolean hasBufferedOutput() {
        InterestWritableByteChannel channel = this.channel;
        return channel != null && channel.hasBufferedOutput();
    }
    
    public boolean handleWrite() throws IOException {
        WriteObserver interested = observer;
        if(interested != null)
            return interested.handleWrite();
        else
            return false;
    }
    
    public int write(ByteBuffer src) throws IOException {
        InterestWritableByteChannel source = channel;
        if(source == null)
            throw new IllegalStateException("no source!");
        return source.write(src);
    }

}
