package org.limewire.nio;

import java.io.IOException;

import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.InterestReadableByteChannel;

/**
 * A simple reader that does nothing.
 * This is used primarily to allow objects that always require a non-null reader
 * to clean up references to old readers while still maintaining a non-null reader.
 */
class NoOpReader implements ChannelReadObserver {
    @Override public void handleRead() throws IOException {}
    @Override public void handleIOException(IOException iox) {}
    @Override public void shutdown() {}
    @Override public InterestReadableByteChannel getReadChannel() { return null; }
    @Override public void setReadChannel(InterestReadableByteChannel newChannel) {}

}
