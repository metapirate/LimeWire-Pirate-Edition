package org.limewire.nio.observer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Defines the interface that allows <code>SocketChannel</code> accept events
 * to be received. Classes that listen on a socket are notified of new 
 * connections through this interface.
 */
public interface AcceptChannelObserver extends IOErrorObserver {

    /**
     * Notification that a <code>SocketChannel</code> has been accepted. The
     * channel is in non-blocking mode.
     */
    void handleAcceptChannel(SocketChannel channel) throws IOException;
}