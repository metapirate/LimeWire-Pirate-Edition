package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.Socket;

import org.limewire.nio.observer.ConnectObserver;


/**
 * Specialized base class for download-related Observers for better 
 * type safety
 */
abstract class HTTPConnectObserver implements ConnectObserver {

    /**
     * unlike other ConnectObservers these do not throw.
     */
    public abstract void handleConnect(Socket socket);

    /**
     * IOExceptions are ignored.
     */
    public void handleIOException(IOException iox) {}

}
