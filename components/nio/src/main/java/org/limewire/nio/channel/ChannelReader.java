package org.limewire.nio.channel;

/**
 * Defines the interface to set and get {@link InterestReadableByteChannel 
 * InterestReadableByteChannels} as the source for reading.
 */
public interface ChannelReader {
    
    /** Set the source channel. */
    void setReadChannel(InterestReadableByteChannel newChannel);
    
    /** Gets the source channel. */
    InterestReadableByteChannel getReadChannel();
}