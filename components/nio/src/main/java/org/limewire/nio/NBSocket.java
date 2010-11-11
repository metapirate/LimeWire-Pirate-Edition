package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;

/**
 * Abstract class that allows a <code>Socket</code> to provide
 * a connection method that takes a {@link ConnectObserver}.
 */
public abstract class NBSocket extends Socket {
    /**
     * Connects to the specified address within the given timeout (in milliseconds).
     * The given <code>ConnectObserver</code> will be notified of success or failure.
     * In the event of success, <code>observer.handleConnect</code> is called. 
     * In a failure, <code>observer.shutdown</code> is called. 
     * <code>observer.handleIOException</code> is never called.
     * <p>
     * Returns true if this was able to connect immediately. The observer is still
     * notified about the success even it it was immediate.
     * Returns false if it was unable to connect immediately. The observer will
     * receive the connection events.
     * <p>
     * This method always returns immediately.
     */
    public abstract boolean connect(SocketAddress addr, int timeout, ConnectObserver observer);
    
    /**
     * Sets an arbitrary <code>Shutdownable</code> as a shutdown observer.
     * This observer is useful for being notified if the socket is shutdown prior to
     * connect being called.
     */
    public abstract void setShutdownObserver(Shutdownable observer);
    
    // a bunch of Constructors.
    
    public NBSocket() {
        super();
    }
    
    public NBSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }
    
    public NBSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }
    
    public NBSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }
    
    public NBSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }    
}
