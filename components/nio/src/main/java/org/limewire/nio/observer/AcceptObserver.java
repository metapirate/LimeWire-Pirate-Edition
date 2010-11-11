package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;

/**
 * Defines the interface that allows accept events to be received. An observer 
 * watches a subject and performs the acceptance of a socket when the subject
 * is ready (signaled via an accept event).
 */
public interface AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notification that a socket is ready.
     */
    void handleAccept(Socket socket) throws IOException;
}