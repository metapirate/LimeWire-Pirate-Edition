package com.limegroup.gnutella.handshaking;

import java.util.Collection;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class HandshakeServicesImpl implements HandshakeServices {

    private final Provider<ConnectionManager> connectionManager;

    private final NetworkManager networkManager;
    
    private final ConnectionServices connectionServices;

    @Inject
    HandshakeServicesImpl(Provider<ConnectionManager> connectionManager,
            NetworkManager networkManager, ConnectionServices connectionServices) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.connectionServices = connectionServices;
    }

    public HandshakeStatus getHandshakeStatusForResponse(HandshakeResponse handshakeResponse) {
        return connectionManager.get().allowConnection(handshakeResponse);
    }

    public List<? extends IpPort> getLeafNodes() {
        return connectionManager.get().getInitializedClientConnections();
    }

    public IpPort getLocalIpPort() {
        return new IpPortImpl(networkManager.getAddress(), networkManager.getPort());
    }

    public List<? extends IpPort> getUltrapeerNodes() {
        return connectionManager.get().getInitializedConnections();
    }

    public Collection<IpPort> getAvailableHosts(boolean isUltrapeer, String locale, int num) {
        return connectionServices.getPreferencedHosts(isUltrapeer, locale, num);
    }

    public HandshakeStatus getHandshakeStatusForResponseAsLeaf(HandshakeResponse handshakeResponse) {
        return connectionManager.get().allowConnectionAsLeaf(handshakeResponse);
    }

    public boolean isLeafDemotionAllowed() {
        return connectionManager.get().allowLeafDemotion();
    }

    public boolean isUltrapeer() {
        return connectionManager.get().isSupernode();
    }

    public boolean isUltrapeerNeeded() {
        return connectionManager.get().supernodeNeeded();
    }

}
