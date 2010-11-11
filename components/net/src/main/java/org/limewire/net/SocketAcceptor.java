package org.limewire.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.AcceptObserver;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches threads
 * to handle requests. This allows simple HTTP requests.
 */
public class SocketAcceptor {

    private final ConnectionDispatcher dispatcher;

    /** 
     * The socket that listens for incoming connections.
     * <p> 
     * Note: Obtain <code>this</code> lock before accessing. 
     */
    private ServerSocket listeningSocket = null;

    private volatile int listeningPort = -1;

    private volatile boolean localOnly;

    public SocketAcceptor(ConnectionDispatcher connectionDispatcher) {
        this.dispatcher = connectionDispatcher;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening. If
     *          that fails, this is <i>not</i> modified and IOException is
     *          thrown. If port==0, tells this to stop listening to incoming
     *          connections. This is properly synchronized and can be called
     *          even while run() is being called.
     */
    public synchronized void bind(int port) throws IOException {
        // if unchanged, do nothing.
        if (this.listeningSocket != null && this.listeningPort == port) {
            return;
        }

        // try new port.
        ServerSocket newSocket = SocketFactory.newServerSocket(port,
                new SocketListener());

        // close old socket
        if (listeningSocket != null) {
            IOUtils.close(listeningSocket);
        }

        // replace with new sock.
        this.listeningSocket = newSocket;
        this.listeningPort = port;
    }

    public synchronized void unbind() {
        if (this.listeningSocket == null) {
            return;
        }

        IOUtils.close(this.listeningSocket);
        this.listeningSocket = null;
        this.listeningPort = -1;
    }

    /**
     * Return the listening port.
     */
    public int getPort() {
        return listeningPort;
    }

    /**
     * Returns the dispatcher that dispatches incoming requests to a handler.
     */
    public ConnectionDispatcher getDispatcher() {
        return dispatcher;
    }

    /** Dispatches sockets to a thread that'll handle them. */
    private class SocketListener implements AcceptObserver {

        public void handleIOException(IOException ignored) {
        }

        public void shutdown() {
        }

        public void handleAccept(Socket client) {
            // only allow local connections
            if (isLocalOnly() && !NetworkUtils.isLocalHost(client)) {
                IOUtils.close(client);
                return;
            }

            // dispatch asynchronously if possible
            if (client instanceof NIOMultiplexor) {
                // TODO: This always allows TLS right now -- should conform to NetworkManager.isIncomingTLSEnabled
                ((NIOMultiplexor) client).setReadObserver(new AsyncConnectionDispatcher(dispatcher, client, null, true));
            } else {
                ThreadExecutor.startThread(new BlockingConnectionDispatcher(
                        dispatcher, client, null),
                        "BlockingConnectionDispatchRunner");
            }
        }
    }

}
