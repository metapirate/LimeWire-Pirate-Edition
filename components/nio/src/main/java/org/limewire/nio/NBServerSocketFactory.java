package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import org.limewire.nio.observer.AcceptObserver;

/**
 * Constructs non-blocking {@link ServerSocket ServerSockets}.
 */
public abstract class NBServerSocketFactory extends ServerSocketFactory {
    
    /**
     * Constructs a new, unconnected <code>ServerSocket</code> that will notify
     * the given <code>AcceptObserver</code> when new connections arrive. You 
     * must call 'bind' on the socket to begin accepting new connections.
     * 
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(AcceptObserver observer) throws IOException;
    
    /**
     * Constructs a new <code>ServerSocket</code> bound at the given port.
     * The given observer will be notified when new incoming connections are accepted.
     * 
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(int port, AcceptObserver observer) throws IOException;
    
    /**
     * Constructs a new <code>ServerSocket</code> bound at the given port, using 
     * the given backlog. The given <code>AcceptObserver</code> will be notified 
     * when new incoming connections are accepted.
     * 
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(int port, int backlog, AcceptObserver observer) throws IOException;
    
    /**
     * Constructs a new <code>ServerSocket</code> bound at the given port and 
     * given address, using the given backlog.
     * The given <code>AcceptObserver</code> will be notified when new incoming 
     * connections are accepted.
     * 
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddr, AcceptObserver observer) throws IOException;
}
