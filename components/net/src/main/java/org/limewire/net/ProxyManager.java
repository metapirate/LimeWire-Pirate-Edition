package org.limewire.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.limewire.net.ProxySettings.ProxyType;
import org.limewire.nio.observer.ConnectObserver;

/** Manages connecting to proxies. */
public interface ProxyManager {
    
    /** Gets the host this should proxy to. */
    public InetSocketAddress getProxyHost() throws UnknownHostException;

    /**
     * Determines the kind of proxy to use for connecting to the given address.
     */
    public ProxyType getProxyType(InetAddress address);

    /**
     * Establishes a proxy connection on the given socket.
     * This should be used for blocking connections.
     */
    public Socket establishProxy(ProxyType type, Socket proxySocket, InetSocketAddress addr, int timeout)
            throws IOException;
    
    /**
     * Returns a ProxyConnector that will establish a proxy on the given socket.
     * This should be used for non-blocking connections.
     */
    public ProxyConnector getConnectorFor(ProxyType type, ConnectObserver observer, InetSocketAddress host, int timeout);
    
    /** Defines the interface that implementations of ProxyManager should use when connecting to the proxy. */
    static interface ProxyConnector extends ConnectObserver {
        /** Returns the observer this is proxying. */
        public ConnectObserver getDelegateObserver();
        
    };

}