package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.limewire.nio.observer.WriteObserver;

/**
 * A {@link InterestWritableByteChannel} that delegates to a {@link WritableByteChannel}
 * and ignores all other methods.
 */
public class NoInterestWritableByteChannel implements InterestWritableByteChannel {
    
    private final WritableByteChannel delegate;
    
    public NoInterestWritableByteChannel(WritableByteChannel delegate) {
        this.delegate = delegate;
    }

    public void close() throws IOException {
        delegate.close();
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    public boolean hasBufferedOutput() {
        return false;
    }

    public void interestWrite(WriteObserver observer, boolean status) {
    }

    public boolean handleWrite() throws IOException {
        return false;
    }

    public void handleIOException(IOException iox) {
    }

    public void shutdown() {
    }

    
    

}
