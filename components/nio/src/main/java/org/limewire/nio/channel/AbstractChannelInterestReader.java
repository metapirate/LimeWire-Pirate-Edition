package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.util.BufferUtils;

/**
 * Handles allocating a <code>ByteBuffer</code> of a given size, allows chained 
 * readers to read from this, deals with propagating interest to a
 * source channel, propagates <code>close</code> and <code>isOpen</code>, 
 * implements a stubbed-out <code>handleIOException</code>, and marks when 
 * the channel has been shutdown. 
 * <p>
 * Subclasses only need to implement <code>handleRead</code> to move data from 
 * the source channel into the buffer and react to what was read. 
 */
public abstract class AbstractChannelInterestReader implements ChannelReadObserver, InterestScatteringByteChannel {
    
    /**
     * Implementors should write to <code>buffer</code>.
     */
    protected ByteBuffer buffer;
    protected InterestReadableByteChannel source;
    protected boolean shutdown;
    
    public AbstractChannelInterestReader(int bufferSize) {
        buffer = ByteBuffer.allocate(bufferSize);
    }
    
    public int read(ByteBuffer dst) {
        return BufferUtils.transfer(buffer, dst);
    }
    
    public long read(ByteBuffer [] dst) {
    	return read(dst, 0, dst.length);
    }
    
    public long read(ByteBuffer [] dst, int offset, int length) {
    	return BufferUtils.transfer(buffer, dst, offset, length, true);
    }

    public void shutdown() {
        shutdown = true;
    }

    public InterestReadableByteChannel getReadChannel() {
        return source;
    }

    public void setReadChannel(InterestReadableByteChannel newChannel) {
        this.source = newChannel;
    }

    public void interestRead(boolean status) {
        source.interestRead(status);
    }

    public void close() throws IOException {
        source.close();
    }

    public boolean isOpen() {
        return source.isOpen();
    }

    public void handleIOException(IOException iox) {}
    
}
