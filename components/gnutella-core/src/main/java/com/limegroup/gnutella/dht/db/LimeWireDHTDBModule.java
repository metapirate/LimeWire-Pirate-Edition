package com.limegroup.gnutella.dht.db;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class LimeWireDHTDBModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AltLocFinder.class).to(AltLocFinderImpl.class);
        bind(PushEndpointService.class).annotatedWith(Names.named("pushEndpointManager")).to(PushEndpointManagerImpl.class);
        bind(PushEndpointService.class).annotatedWith(Names.named("dhtPushEndpointFinder")).to(DHTPushEndpointFinder.class);
    }

}
