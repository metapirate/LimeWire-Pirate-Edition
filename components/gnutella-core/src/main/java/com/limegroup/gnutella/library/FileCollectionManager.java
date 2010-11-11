package com.limegroup.gnutella.library;

import java.util.List;

public interface FileCollectionManager {

    /** Returns a {@link SharedFileCollection} with the given id. */
    SharedFileCollection getCollectionById(int collectionId);

    /** Removes the shared collection. */
    void removeCollectionById(int collectionId);
    
    /** Returns a new collection named the given name. */
    SharedFileCollection createNewCollection(String name);
    
    /** Returns all current shared collections. */
    List<SharedFileCollection> getSharedFileCollections();

}
