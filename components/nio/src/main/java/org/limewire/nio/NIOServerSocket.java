package org.limewire.nio;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.observer.AcceptChannelObserver;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.util.VersionUtils;

/**
 * A <code>ServerSocket</code> that does all of its accepting using NIO, 
 * however <code>NIOServerSocket</code> pseudo-blocks ({@link #accept()} waits
 * until an accept, or timeouts after {@link #getSoTimeout()}).
 */
public class NIOServerSocket extends ServerSocket implements AcceptChannelObserver {
    
    private static final Log LOG = LogFactory.getLog(NIOServerSocket.class);
    
    /** Channel backing this NIOServerSocket. */
    private final ServerSocketChannel channel;
    /** Socket associate of the channel. */
    private final ServerSocket socket;
    /** AcceptObserver that will be retrieving the sockets. */
    private final AcceptObserver observer;
    
    /**
     * Constructs a new, unbound, <code>NIOServerSocket</code>.
     * You must call 'bind' to start listening for incoming connections.
     */
    public NIOServerSocket() throws IOException {
        this(null);
    }
    
    /**
     * Constructs a new, unbound, <code>NIOServerSocket</code>.
     * You must call 'bind' to start listening for incoming connections.
     * All accepted connections will be routed to the given <code>AcceptObserver</code>.
     */
    public NIOServerSocket(AcceptObserver observer) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        if(observer == null)
            this.observer = new BlockingObserver();
        else
            this.observer = observer;
    }
    
    /** Constructs a new <code>NIOServerSocket</code> bound to the given port. */
    public NIOServerSocket(int port) throws IOException {
        this(port, null);
    }

    /** 
     * Constructs a new <code>NIOServerSocket</code> bound to the given port 
     * All accepted connections will be routed to the given <code>AcceptObserver</code>.
     */
    public NIOServerSocket(int port, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(port));
    }
    
    /**
     * Constructs a new <code>NIOServerSocket</code> bound to the given port, 
     * able to accept the given backlog of connections.
     */
    public NIOServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, (AcceptObserver)null);
    }
    
    /**
     * Constructs a new <code>NIOServerSocket</code> bound to the given port, 
     * able to accept the given backlog of connections.
     * All accepted connections will be routed to the given <code>AcceptObserver</code>.
     */
    public NIOServerSocket(int port, int backlog, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(port), backlog);
        
    }
    
    /**
     * Constructs a new <code>NIOServerSocket</code> bound to the given port and
     * addr, able to accept the given backlog of connections.
     */
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        this(port, backlog, bindAddr, null);
    }
    
    /**
     * Constructs a new <code>NIOServerSocket</code> bound to the given port and
     * addr, able to accept the given backlog of connections.
     * All accepted connections will be routed to the given <code>AcceptObserver</code>.
     */
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(bindAddr, port), backlog);
    }
    
    /**
     * Initializes the connection.
     * Currently this sets the channel to blocking and setReuseAddress to true.
     */
    private void init() throws IOException {
        channel.configureBlocking(false);
        socket.setReuseAddress(true);
    }

    /**
     * Accepts an incoming connection.
     * <p>
     * THIS CAN ONLY BE USED IF NO <code>AcceptObserver</code> WAS PROVIDED WHEN 
     * CONSTRUCTING THIS <code>NIOServerSocket</code>. All other attempts will 
     * cause an immediate <code>RuntimeException</code>.
     */
    @Override
    public Socket accept() throws IOException {
        if(observer instanceof BlockingObserver)
            return ((BlockingObserver)observer).accept();
        else
            throw new IllegalBlockingModeException(); 
    }
    
    /**
     * Notification that a socket has been accepted.
     */
    public void handleAcceptChannel(SocketChannel channel) throws IOException {
        observer.handleAccept(createClientSocket(channel.socket()));
    }
    
    /**
     * Notification that an IOException occurred while accepting.
     */
    public void handleIOException(IOException iox) {
        observer.handleIOException(iox);
    }
    
    /**
     * Closes this socket. 
     */
    public void shutdown() {
        try {
            close();
        } catch(IOException ignored) {}
    }
    
    /** Binds the socket to the endpoint & starts listening for incoming connections. */
    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
        NIODispatcher.instance().registerAccept(channel, this);
    }
     
    /** Binds the socket to the endpoint & starts listening for incoming connections. */
    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        socket.bind(endpoint, backlog);
        NIODispatcher.instance().registerAccept(channel, this);
    }
    
    /** Shuts down this NIOServerSocket. */
    @Override
    public void close() throws IOException {
        IOException exception;
        
        // Workaround for bugid: 4744057, fixed in Java 1.5.0_10.
        // Bug: If the channel is closed in a thread other than the selector thread,
        //      there is a potential for deadlock.
        // Note: We ONLY offload the actual shutting of the socket/channel,
        //       as we don't want to expose the observer shutdowns to the
        //       invokeAndWait, which could introduce a lot of potential deadlock.
        if(VersionUtils.isJavaVersionOrAbove("1.5.0_10") || NIODispatcher.instance().isDispatchThread()) {
            exception = shutdownSocketAndChannels();
        } else {
            Future<IOException> future = NIODispatcher.instance().getScheduledExecutorService()
                .submit(new Callable<IOException>() {
                    public IOException call() {
                        return shutdownSocketAndChannels();
                    }
            });
            
            try {
                exception = future.get();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } catch (ExecutionException ee) {
                throw new IllegalStateException(ee);
            }
        }
        
        observer.shutdown();
        
        if(exception != null)
            throw exception;
    }
    
    private IOException shutdownSocketAndChannels() {        
        IOException exception = null;
        try {
            socket.close();
        } catch(IOException iox) {
            exception = iox;
        }
        return exception;
    }
    
    /** Wraps the accepted Socket in a delegating socket. */
    protected Socket createClientSocket(Socket socket) {
        return new NIOSocket(socket);
    }


    /////////////////////////////////////////////////////////////
    /////////// Below are simple wrappers for the socket.
    /////////////////////////////////////////////////////////////    

    @Override
    public ServerSocketChannel getChannel() {
        return socket.getChannel();
    }
 
    @Override
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
    
    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    
    @Override
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }
    
    @Override
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }
    
    @Override
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }
    
    @Override
    public int getSoTimeout() throws IOException {
        return socket.getSoTimeout();
    }
    
    @Override
    public boolean isBound() {
        return socket.isBound();
    }
    
    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }
    
    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }
    
    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }
    
    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }
    
    @Override
    public String toString() {
        return "NIOServerSocket::" + socket.toString();
    }
    
    /**
     * An <code>AcceptObserver</code> that stores up connections for use with 
     * blocking accepts.
     */
    private class BlockingObserver implements AcceptObserver {
        /** List of all pending sockets that can be accepted. */
        private final List<Socket> pendingSockets = new LinkedList<Socket>();
        /** An exception that was stored and should be thrown during the next accept */
        private IOException storedException = null;
        /** Lock to be used for synchronizing access to pendingSockets. */
        private final Object LOCK = new Object();
        
        /**
         * Gets the next socket that was accepted, or throws IOException if an
         * exception occurred in this ServerSocket.
         */
        public Socket accept() throws IOException {
            synchronized (LOCK) {
                boolean looped = false;
                int timeout = getSoTimeout();
                while (!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {
                    if (looped && timeout != 0)
                        throw new SocketTimeoutException("accept timed out: " + timeout);

                    LOG.debug("Waiting for incoming socket...");
                    try {
                        LOCK.wait(timeout);
                    } catch (InterruptedException ix) {
                        throw new InterruptedIOException(ix);
                    }
                    looped = true;
                }

                IOException x = storedException;
                storedException = null;

                if (isClosed())
                    throw new SocketException("Socket Closed");
                else if (x != null)
                    throw x;
                else if (!isBound())
                    throw new SocketException("Not Bound!");
                else {
                    LOG.debug("Retrieved a socket!");
                    return pendingSockets.remove(0);
                }
            }
        }        

        /** Stores up the next Socket for use with accept. */
        public void handleAccept(Socket socket) throws IOException {
            synchronized (LOCK) {
                pendingSockets.add(socket);
                LOCK.notify();
            }
        }

        /** Notification an exception occurred. */
        public void handleIOException(IOException iox) {
            synchronized(LOCK) {
                storedException = iox;
                LOCK.notify();
            }
        }

        /** Notification that the socket was shutdown. */
        public void shutdown() {
            synchronized(LOCK) {
                // Shutdown all sockets it created.
                for(Socket next : pendingSockets) {
                    try {
                        next.close(); 
                    } catch(IOException ignored) {}
                }
                pendingSockets.clear();
                
                LOCK.notify();
            }
        }
    }
    
}