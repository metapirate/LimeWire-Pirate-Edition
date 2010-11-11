package org.limewire.nio.channel;

import java.nio.channels.ReadableByteChannel;

/**
 * Defines an interface to turn on or off interest in reading from the channel.
 */
public interface InterestReadableByteChannel extends ReadableByteChannel {

    /** 
     * Allows this <code>ReadableByteChannel</code> to be told that someone is 
     * no longer interested in reading from it. Conversely, you can signal
     * interest in reading.
     * @param status true means interest in reading, false means a lack of
     * interest.
     */
    public void interestRead(boolean status);
    
}
