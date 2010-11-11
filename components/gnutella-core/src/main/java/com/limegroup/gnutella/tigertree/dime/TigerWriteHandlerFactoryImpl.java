package com.limegroup.gnutella.tigertree.dime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeNodeManager;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandler;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandlerFactory;

@Singleton
public class TigerWriteHandlerFactoryImpl implements HashTreeWriteHandlerFactory {

    private final HashTreeNodeManager tigerTreeNodeManager;
    
    @Inject
    public TigerWriteHandlerFactoryImpl(HashTreeNodeManager tigerTreeNodeManager) {
        this.tigerTreeNodeManager = tigerTreeNodeManager;
    }

    public HashTreeWriteHandler createTigerWriteHandler(HashTree tree) {
        return new TigerDimeWriteHandler(tree, tigerTreeNodeManager);
    }
}
