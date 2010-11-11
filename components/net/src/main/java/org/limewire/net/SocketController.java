package org.limewire.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;

/**
 * Allows sockets to be connected by arbitrary means.
 * One example implementation could immediately connecting the Socket,
 * whereas another might queue sockets so that only a certain amount
 * can connect at a given time.
 */
public interface SocketController {

    /** 
     * Enqueue's this socket for wanting a connection.
     * @param factory the factory which will create the socket.
     * @param remoteAddress the address the socket will connect to.
     * @param localAddress the local address this should be to.  (null allowed)
     * @param timeout the amount of time to wait before timing out the connection.
     * @param observer the ConnectObserver to notify about success or failure.
     */ 
    Socket connect(NBSocketFactory factory, InetSocketAddress remoteAddress, InetSocketAddress localAddress, int timeout, ConnectObserver observer) throws IOException;
    
    /**
     * Dequeues any connections enqueued by the given ConnectObserver.
     * Returns true if a connection was successfully dequeued.
     */
    boolean removeConnectObserver(ConnectObserver observer);
    
    /**
     * Returns the number of sockets this controller is allowed to connect at once.
     * Any connection attempts enqueued beyond this amount will wait until a prior
     * attempt has finished.
     */
    int getNumAllowedSockets();
    
    /** Returns the number of sockets currently in queue, waiting to begin connecting. */
    int getNumWaitingSockets();
    
}
