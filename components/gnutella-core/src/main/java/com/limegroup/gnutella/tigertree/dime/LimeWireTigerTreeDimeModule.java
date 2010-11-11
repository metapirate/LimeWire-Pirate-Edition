package com.limegroup.gnutella.tigertree.dime;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandlerFactory;

public class LimeWireTigerTreeDimeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        
        bind(ThexReaderFactory.class).to(ThexReaderFactoryImpl.class);
        bind(HashTreeWriteHandlerFactory.class).to(TigerWriteHandlerFactoryImpl.class);
    }

}
