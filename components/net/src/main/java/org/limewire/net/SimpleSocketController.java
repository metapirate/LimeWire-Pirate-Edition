package org.limewire.net;

import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A SocketController that blindly connects to the given address
 * (deferring the connection through proxies, if necessary).
 */
@Singleton
class SimpleSocketController extends AbstractSocketController {    
    
    @Inject
    SimpleSocketController(ProxyManager proxyManager, SocketBindingSettings defaultSocketBindingSettings) {
        super(proxyManager, defaultSocketBindingSettings);
    }

    /** Allows endless # of sockets. */
    public int getNumAllowedSockets() {
        return Integer.MAX_VALUE;
    }

    /** Does nothing. */
    public boolean removeConnectObserver(ConnectObserver observer) {
        return false;
    }
    
    /** Returns 0. */
    public int getNumWaitingSockets() {
        return 0;
    }

}
