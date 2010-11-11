package com.limegroup.gnutella.tigertree.dime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.tigertree.ThexReader;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;
import com.limegroup.gnutella.tigertree.HashTreeFactory;

@Singleton
class ThexReaderFactoryImpl implements ThexReaderFactory {
    
    private final HashTreeFactory tigerTreeFactory;
    
    @Inject
    public ThexReaderFactoryImpl(HashTreeFactory tigerTreeFactory) {
        this.tigerTreeFactory = tigerTreeFactory;
    }
    
    public ThexReader createHashTreeReader(String sha1, String root32, long fileSize) {
        return new AsyncTigerTreeReader(sha1, fileSize, root32, tigerTreeFactory);
    }

}
