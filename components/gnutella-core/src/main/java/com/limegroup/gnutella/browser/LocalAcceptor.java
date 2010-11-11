package com.limegroup.gnutella.browser;

import java.io.IOException;

import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketAcceptor;
import org.limewire.service.MessageService;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches threads
 * to handle requests. This allows simple HTTP requests.
 */
@EagerSingleton
public class LocalAcceptor implements Service {

    private final static int FIRST_PORT = 45100;

    private final SocketAcceptor acceptor;

    @Inject
    public LocalAcceptor(@Named("local") ConnectionDispatcher connectionDispatcher) {
        this.acceptor = new SocketAcceptor(connectionDispatcher);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Local Socket Listener");
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public void initialize() {
    }

    /**
     * Starts listening to incoming connections.
     */
    public void start() {
        if (!bind(FIRST_PORT)) {
            MessageService.showError(I18nMarker.marktr("LimeWire was unable to set up a port to listen for incoming connections. Some features of LimeWire may not work as expected."));
        }
    }
    
    public void stop() {
        acceptor.unbind();
    }

    private boolean bind(final int listeningPort) {
        // try 20 different consecutive ports from 45100 to 45119 (inclusive)
        // no longer random, since this listening socket is used by the
        // executable stub to launch magnet downloads, so the stub needs to know
        // what (small) range of ports to try...
        for (int i = 0; i < 20; i++) {
            try {
                acceptor.bind(listeningPort + i);
                return true;
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    /**
     * Return the listening port.
     */
    public int getPort() {
        return acceptor.getPort();
    }

    /**
     * Returns the dispatcher that dispatches incoming requests to a handler.
     */
    public ConnectionDispatcher getDispatcher() {
        return acceptor.getDispatcher();
    }

}
