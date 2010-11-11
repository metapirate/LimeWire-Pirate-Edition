package org.limewire.nio.channel;

import java.nio.channels.WritableByteChannel;

import org.limewire.nio.observer.WriteObserver;

/**
 * Defines an interface for a channel that can be written to, can receive write 
 * events (when with interest), and can forward write events to a 
 * <code>WriteObserver</code> (who could in turn notify another 
 * <code>WriteObserver</code>).
 */
public interface InterestWritableByteChannel extends WritableByteChannel, WriteObserver {
    
    /**
     * Marks the given observer as interested (or not interested, if status is false)
     * in knowing when a write can be performed on this channel.
     */
    public void interestWrite(WriteObserver observer, boolean status);

    /**
     * Returns true, if the channel has buffered data. If the channel is closed
     * before this method returns <code>false</code> the buffered data is lost.
     */
    public boolean hasBufferedOutput();
    
}