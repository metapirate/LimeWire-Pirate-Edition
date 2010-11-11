package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.SharedFileCollection;
import com.limegroup.gnutella.library.SharedFileCollectionChangeEvent;
import com.limegroup.gnutella.library.SharedFiles;

@EagerSingleton
class SharedFileListManagerImpl implements SharedFileListManager {
    
//    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileCollectionManager collectionManager;    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;
    
    private final EventList<SharedFileList> sharedLists = GlazedListsFactory.threadSafeList(new BasicEventList<SharedFileList>());
    private final EventList<SharedFileList> readOnlySharedLists = GlazedListsFactory.readOnlyList(sharedLists);

    private final FileView allSharedFilesView;
    
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
    
    private volatile long listsCreatedV2;
    
    private volatile long listsShared;
        
    @Inject
    SharedFileListManagerImpl(FileCollectionManager collectionManager,
            CoreLocalFileItemFactory coreLocalFileItemFactory,
            LibraryManager libraryManager, 
            @SharedFiles FileView allSharedFilesView) {
        this.collectionManager = collectionManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
        this.allSharedFilesView = allSharedFilesView;
    }
    
    @Inject void register(ListenerSupport<SharedFileCollectionChangeEvent> support) {
        allSharedFilesView.addListener(new EventListener<FileViewChangeEvent>() {
            public void handleEvent(FileViewChangeEvent event) {
                switch(event.getType()) {
                case FILE_ADDED:
                case FILE_REMOVED:
                case FILES_CLEARED:
                    changeSupport.firePropertyChange(SHARED_FILE_COUNT, null, allSharedFilesView.size());
                    break;
                }
            }
        });
        
        for(SharedFileCollection collection : collectionManager.getSharedFileCollections()) {
            collectionAdded(collection);
        }
        
        support.addListener(new EventListener<SharedFileCollectionChangeEvent>() {
            @Override
            public void handleEvent(SharedFileCollectionChangeEvent event) {
                switch(event.getType()) { 
                case COLLECTION_ADDED:
                    collectionAdded(event.getSource());
                    break;
                case COLLECTION_REMOVED:
                    collectionRemoved(event.getSource());
                    break;
                case FRIEND_ADDED:
                    friendAddedToCollection(event.getSource(), event.getFriendId());
                    break;
                case FRIEND_IDS_CHANGED:
                    friendsSetInCollection(event.getSource(), event.getNewFriendIds());
                    break;
                case FRIEND_REMOVED:
                    friendRemoved(event.getSource(), event.getFriendId());
                    break;
                case NAME_CHANGED:
                    nameChanged(event.getSource());
                    break;
                }
            }
        });
    }

    // we technically don't have to change anything here, but we want to
    // make the list trigger an event to signify that something changed,
    // so we get the index of where it used to be & reset it.
    private void nameChanged(SharedFileCollection collection) {
        setListInPlace(collection);
    }
    
    /**
     * Sets the SharedFileListImpl that holds this collection in place, allowing
     * the model to trigger an update event.
     */
    private void setListInPlace(SharedFileCollection collection) {
        sharedLists.getReadWriteLock().writeLock().lock();
        try {
            for (int i = 0; i < sharedLists.size(); i++) {
                SharedFileListImpl impl = (SharedFileListImpl)sharedLists.get(i);
                if (impl.getCoreCollection() == collection) {
                    sharedLists.set(i, impl); // reset it to trigger event.
                    break;
                }
            }
        } finally {
            sharedLists.getReadWriteLock().writeLock().unlock();
        }
    }

    private void friendRemoved(SharedFileCollection collection, String friendId) {
        SharedFileListImpl list = getListForCollection(collection);
        int oldSize = list.getFriendIds().size();
        boolean removed = list.friendRemoved(friendId);
        // If we removed the last friend, reset the list to trigger an update event.
        if(oldSize == 1 && removed) {
            setListInPlace(collection);
        }
    }

    private void friendsSetInCollection(SharedFileCollection collection, Collection<String> newFriendIds) {
        SharedFileListImpl list = getListForCollection(collection);
        boolean wasEmpty = list.getFriendIds().isEmpty();
        list.friendsSet(newFriendIds);
        boolean isEmpty = newFriendIds.isEmpty();
        // if it changed from empty => not empty, or not empty => empty, trigger an update.
        if(wasEmpty != isEmpty) {
            if (!isEmpty) {
                listsShared++;    
            }
            setListInPlace(collection);
        }
    }

    private void friendAddedToCollection(SharedFileCollection collection, String friendId) {
        SharedFileListImpl list = getListForCollection(collection);
        boolean wasEmpty = list.getFriendIds().isEmpty();
        list.friendAdded(friendId);
        // if it used to be, trigger an update
        if(wasEmpty) {
            listsShared++;
            setListInPlace(collection);
        }
    }    
    
    private void collectionAdded(SharedFileCollection collection) {
        SharedFileListImpl listImpl = new SharedFileListImpl(coreLocalFileItemFactory, collection);
        listImpl.friendsSet(collection.getFriendList());
        sharedLists.add(listImpl);
    }
    
    private void collectionRemoved(SharedFileCollection collection) {
        sharedLists.remove(getListForCollection(collection));
    }
    
    private SharedFileListImpl getListForCollection(SharedFileCollection collection) {
       sharedLists.getReadWriteLock().readLock().lock();
        try {
            for (SharedFileList list : sharedLists) {
                SharedFileListImpl impl = (SharedFileListImpl) list;
                if (impl.getCoreCollection() == collection) {
                    return impl;
                }
            }
            return null;
        } finally {
            sharedLists.getReadWriteLock().readLock().unlock();
        }
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    @Override
    public int getSharedFileCount() {
        return allSharedFilesView.size();
    }

    @Override
    public int createNewSharedFileList(String name) {
        listsCreatedV2++;
        return collectionManager.createNewCollection(name).getId();        
    }

    @Override
    public EventList<SharedFileList> getModel() {
        return readOnlySharedLists;
    }

    @Override
    public void deleteSharedFileList(SharedFileList fileList) {
        if(fileList instanceof SharedFileListImpl) {
            collectionManager.removeCollectionById(((SharedFileListImpl)fileList).getCoreCollection().getId());
        } else {
            throw new IllegalStateException("invalid FileList: " + fileList);
        }
    }
    
    @Override
    public void removeDocumentsFromPublicLists() {
        LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.setValue(false);
        EventList<SharedFileList> shareLists = getModel();
        shareLists.getReadWriteLock().readLock().lock();
        try {
            for (SharedFileList sharedFileList : shareLists) {
                if (sharedFileList.isPublic()) {
                    sharedFileList.removeFiles(new Predicate<LocalFileItem>() {
                        @Override
                        public boolean apply(LocalFileItem localFileItem) {
                            return localFileItem.getCategory() == Category.DOCUMENT;
                        }
                    });
                }
            }
        } finally {
            shareLists.getReadWriteLock().readLock().unlock();
        }
    }
}
