package com.limegroup.gnutella.bootstrap;

import org.limewire.inject.AbstractModule;

public class LimeWireBootstrapModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Bootstrapper.class).to(BootstrapperImpl.class);
        bind(TcpBootstrap.class).to(TcpBootstrapImpl.class);
        bind(UDPHostCache.class).to(UDPHostCacheImpl.class);
    }
}
