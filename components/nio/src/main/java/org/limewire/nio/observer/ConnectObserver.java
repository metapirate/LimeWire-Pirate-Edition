package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;

/**
 * Defines an interface that allows connect events to be received.
 * Classes that listen on a socket are notified of connections through this 
 * interface.
 */
public interface ConnectObserver extends IOErrorObserver {
    
    /** Notification that a <code>socket</code> is not connected. */
    void handleConnect(Socket socket) throws IOException;
}