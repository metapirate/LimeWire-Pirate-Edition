package com.limegroup.gnutella.net.address;

import java.io.IOException;

import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.SocketsManager;
import org.limewire.net.TLSManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.net.address.AddressConnector;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;

/**
 * This class handles connecting to addresses of type {@link Connectable}.
 */
@EagerSingleton
public class ConnectableConnector implements AddressConnector {

    private static final Log LOG = LogFactory.getLog(ConnectableConnector.class, "address-connecting");

    private final SocketsManager socketsManager;
    private final TLSManager tlsManager;

    @Inject
    public ConnectableConnector(SocketsManager socketsManager, TLSManager tlsManager) {
        this.socketsManager = socketsManager;
        this.tlsManager = tlsManager;
        socketsManager.registerConnector(this);
    }

    @Override
    public boolean canConnect(Address address) {
        boolean canConnect = address instanceof Connectable;
        LOG.debugf("{0} connect remote address {1}", (canConnect ? "can" : "can not"), address);
        return canConnect;
    }

    @Override
    public void connect(Address address, ConnectObserver observer) {
        Connectable connectable = (Connectable)address;
        try {
            ConnectType type = connectable.isTLSCapable() && tlsManager.isOutgoingTLSEnabled() ? ConnectType.TLS : ConnectType.PLAIN;
            socketsManager.connect(connectable.getInetSocketAddress(), 10 * 1000, observer, type);
        } catch (IOException e) {
            observer.handleIOException(e);
        }
    }

}
