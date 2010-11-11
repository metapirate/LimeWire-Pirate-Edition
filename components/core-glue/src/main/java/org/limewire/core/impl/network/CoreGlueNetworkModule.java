package org.limewire.core.impl.network;

import org.limewire.core.api.network.NetworkManager;

import com.google.inject.AbstractModule;

public class CoreGlueNetworkModule extends AbstractModule {
        
    @Override
    protected void configure() {
        bind(NetworkManager.class).to(NetworkManagerImpl.class);
    }
}
