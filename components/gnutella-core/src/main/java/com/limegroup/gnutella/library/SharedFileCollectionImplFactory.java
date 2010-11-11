package com.limegroup.gnutella.library;

/** A factory for creating {@link SharedFileCollectionImpl}s. */
interface SharedFileCollectionImplFactory {
    
    SharedFileCollectionImpl createSharedFileCollectionImpl(int collectionId, boolean publicCollection, String... defaultFriendIds);

}
