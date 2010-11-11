package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class FileCollectionManagerImpl implements FileCollectionManager {
    
    private final SharedFileCollectionImpl defaultSharedCollection;
    
    private final IncompleteFileCollectionImpl incompleteCollection;
    
    private final SharedFileCollectionImplFactory sharedFileCollectionImplFactory;
    
    private final Map<Integer, SharedFileCollectionImpl> sharedCollections =
        new HashMap<Integer,SharedFileCollectionImpl>();

    private final EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster;
    
    private final Provider<LibraryFileData> libraryFileData;
    
    @Inject public FileCollectionManagerImpl(
            IncompleteFileCollectionImpl incompleteFileCollectionImpl,
            SharedFileCollectionImplFactory sharedFileCollectionImplFactory,
            EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster,
            Provider<LibraryFileData> libraryFileData) {
        this.libraryFileData = libraryFileData;
        this.incompleteCollection = incompleteFileCollectionImpl;
        this.incompleteCollection.initialize();
        this.sharedFileCollectionImplFactory = sharedFileCollectionImplFactory;
        this.sharedBroadcaster = sharedBroadcaster;
        this.defaultSharedCollection = sharedFileCollectionImplFactory.createSharedFileCollectionImpl(LibraryFileData.DEFAULT_SHARED_COLLECTION_ID, true, Friend.P2P_FRIEND_ID);
        this.defaultSharedCollection.initialize();
    }
    
    void loadStoredCollections() {
        for(Integer id : libraryFileData.get().getStoredCollectionIds()) {
            if(!sharedCollections.containsKey(id) && id != LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {
                SharedFileCollectionImpl collection =  sharedFileCollectionImplFactory.createSharedFileCollectionImpl(id, false);
                collection.initialize();
                synchronized(this) {
                    sharedCollections.put(id, collection);
                }
                sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
            }
        }
    }    

    FileCollection getGnutellaCollection() {
        return defaultSharedCollection;
    }

    @Override
    public synchronized SharedFileCollection getCollectionById(int collectionId) {
        if(collectionId == LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {
            return defaultSharedCollection;
        } else {
            return sharedCollections.get(collectionId);
        }
    }

    @Override
    public void removeCollectionById(int collectionId) {
        // Cannot remove the default collection.
        if(collectionId != LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {        
            // if it was a valid key, remove saved references to it
            SharedFileCollectionImpl removeFileList;
            synchronized(this) {
                removeFileList = sharedCollections.remove(collectionId);
            }
            
            if(removeFileList != null) {
                removeFileList.dispose();
                sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_REMOVED, removeFileList));
            }
        }
    }
    
    private synchronized SharedFileCollectionImpl createNewCollectionImpl(String name) {
        int newId = libraryFileData.get().createNewCollection(name);
        SharedFileCollectionImpl collection =  sharedFileCollectionImplFactory.createSharedFileCollectionImpl(newId, false);
        collection.initialize();
        sharedCollections.put(newId, collection);
        return collection;
    }
    
    @Override
    public SharedFileCollection createNewCollection(String name) {
        SharedFileCollectionImpl collection = createNewCollectionImpl(name);
        sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
        return collection;
    }
    
    @Override
    public synchronized List<SharedFileCollection> getSharedFileCollections() {
        List<SharedFileCollection> collections = new ArrayList<SharedFileCollection>(sharedCollections.size() + 1);
        collections.add(defaultSharedCollection);
        collections.addAll(sharedCollections.values());
        return collections;
    }
    
}
