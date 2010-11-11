package org.limewire.core.impl.network;

import java.io.IOException;

import org.limewire.core.api.network.NetworkManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NetworkManagerImpl implements NetworkManager {

    private com.limegroup.gnutella.NetworkManager coreNetworkManager;
    
    @Inject
    public NetworkManagerImpl(com.limegroup.gnutella.NetworkManager coreNetworkManager) {
        this.coreNetworkManager = coreNetworkManager;
    }
    
    @Override
    public boolean isIncomingTLSEnabled() {
        return coreNetworkManager.isIncomingTLSEnabled();
    }

    @Override
    public boolean isOutgoingTLSEnabled() {
        return coreNetworkManager.isOutgoingTLSEnabled();
    }

    @Override
    public void setIncomingTLSEnabled(boolean value) {
        coreNetworkManager.setIncomingTLSEnabled(value);
    }

    @Override
    public void setOutgoingTLSEnabled(boolean value) {
        coreNetworkManager.setOutgoingTLSEnabled(value);
    }

    @Override
    public void portChanged() {
        coreNetworkManager.portChanged();
    }

    @Override
    public void setListeningPort(int port) throws IOException {
        coreNetworkManager.setListeningPort(port);
    }

    @Override
    public boolean addressChanged() {
        return coreNetworkManager.addressChanged();
    }

    @Override
    public void validateTLS() {
        coreNetworkManager.validateTLS();
    }

    @Override
    public byte[] getExternalAddress() {
        return coreNetworkManager.getExternalAddress();
    }
}
