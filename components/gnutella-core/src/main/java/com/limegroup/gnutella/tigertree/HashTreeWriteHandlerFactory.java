package com.limegroup.gnutella.tigertree;

public interface HashTreeWriteHandlerFactory {
    
    /**
     * Creates the handler that will manage writing out the given tree.
     */
    HashTreeWriteHandler createTigerWriteHandler(HashTree tree);

}
