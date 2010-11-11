package com.limegroup.gnutella.dht;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DHTBootstrapperFactoryImpl implements DHTBootstrapperFactory {
    
    private final DHTNodeFetcherFactory dhtNodeFetcherFactory;
    
    @Inject
    public DHTBootstrapperFactoryImpl(DHTNodeFetcherFactory dhtNodeFetcherFactory) {
        this.dhtNodeFetcherFactory = dhtNodeFetcherFactory;
    }

    public DHTBootstrapper createBootstrapper(DHTController dhtController) {
        return new DHTBootstrapperImpl(dhtController, dhtNodeFetcherFactory);
    }
}
