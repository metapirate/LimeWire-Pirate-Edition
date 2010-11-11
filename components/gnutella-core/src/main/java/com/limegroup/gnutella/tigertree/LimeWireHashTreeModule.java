package com.limegroup.gnutella.tigertree;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.tigertree.dime.LimeWireTigerTreeDimeModule;

public class LimeWireHashTreeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        binder().install(new LimeWireTigerTreeDimeModule());
        
        bind(HashTreeCache.class).to(HashTreeCacheImpl.class);
        bind(HashTreeFactory.class).to(HashTreeFactoryImpl.class);
        bind(HashTreeNodeManager.class).to(HashTreeNodeManagerImpl.class);
    }

}
