package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;
import org.limewire.friend.api.Friend;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterImpl;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.limegroup.gnutella.library.FileViewChangeEvent.Type;

/**
 * The default implementation of {@link FileViewManager}.
 * 
 * This uses {@link MultiFileView}s to represent a {@link FileView} backed by
 * multiple other views. Any mutable operation on the MultiFileView is performed
 * by this class, and this class is responsible for properly locking all mutable
 * operations.
 */
@EagerSingleton
class FileViewManagerImpl implements FileViewManager {
    
    private static final Log LOG = LogFactory.getLog(FileViewManagerImpl.class);
    
    private final LibraryImpl library;
    
    /** Lock held to mutate any structure in this class or to mutate a MultiFileView. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    /** A FileView containing every file that is shared with anyone. */
    // We cannot construct it here because MultiFileView's constructor implicitly
    // uses FileViewManagerImpl.this.library, which might not be set until
    // the FileViewManagerImpl's constructor finishes.
    private final MultiFileView allSharedFilesView;
    
    /** 
     * The share id mapped to the MultiFileView of all FileDescs visible by that id.
     * This is lazily built as people request views for a given id, which is why
     * scattered throughout this class places check for fileViewsPerFriend(id) != null.
     */
    private final Map<String, MultiFileView> fileViewsPerFriend = new HashMap<String, MultiFileView>();

    /** Any collection this is mapping to a MultiFileView. */
    private final Collection<SharedFileCollection> sharedCollections = new ArrayList<SharedFileCollection>();
    
    /** Multicaster used to broadcast events for a MultiFileView. */
    private final SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster =
        new SourcedEventMulticasterImpl<FileViewChangeEvent, FileView>();

    @Inject
    public FileViewManagerImpl(LibraryImpl library) {
        this.library = library;
        this.allSharedFilesView = new MultiFileView("All Shared Files");
    }
    
    @Inject void register(ListenerSupport<FileViewChangeEvent> viewListeners,
                          ListenerSupport<SharedFileCollectionChangeEvent> collectionListeners,
                          FileCollectionManager collectionManager,
                          ListenerSupport<FileDescChangeEvent> fileDescListeners) {
        for(SharedFileCollection collection : collectionManager.getSharedFileCollections()) {
            collectionAdded(collection);
        }
        
        viewListeners.addListener(new EventListener<FileViewChangeEvent>() {
            @Override
            public void handleEvent(FileViewChangeEvent event) {
                LOG.debugf("Handling event {0}", event);
                if(!(event.getSource() instanceof IncompleteFileCollection)) {                
                    switch(event.getType()) {
                    case FILE_ADDED:
                        if(isFileAddable(event.getFileDesc())) {
                            fileAddedToCollection(event.getFileDesc(), (SharedFileCollection)event.getFileView(), false);
                        }
                        break;
                    case FILE_REMOVED:
                        fileRemovedFromCollection(event.getFileDesc(), (SharedFileCollection)event.getFileView(), false);
                        break;
                    case FILES_CLEARED:
                        if(event.isLibraryClear()) {
                            clearAllViews();
                        } else {
                            collectionCleared((SharedFileCollection)event.getFileView());
                        }
                        break;
                    case FILE_CHANGED:
                        fileChangedInCollection(event.getFileDesc(), event.getOldValue(), (SharedFileCollection)event.getFileView());
                        break;
                    case FILE_META_CHANGED:
                        fileMetaChangedInCollection(event.getFileDesc(), (SharedFileCollection)event.getFileView());
                        break;
                    }
                }
            }
        });
        
        collectionListeners.addListener(new EventListener<SharedFileCollectionChangeEvent>() {
            @Override
            public void handleEvent(SharedFileCollectionChangeEvent event) {
                LOG.debugf("Handling event {0}", event);
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
                case FRIEND_REMOVED:
                    friendRemovedFromCollection(event.getSource(), event.getFriendId());
                    break;
                case FRIEND_IDS_CHANGED:
                    friendIdsChangedInCollection(event.getSource(), event.getOldFriendIds(), event.getNewFriendIds());
                    break;
                }
            }
        });
    }
    
    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {
        multicaster.addListener(listener);
    }
    
    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
        return multicaster.removeListener(listener);
    }
    
    FileView getAllSharedFilesView() {
        return allSharedFilesView;
    }
    
    FileView getGnutellaFileView() {
        return getFileViewForId(Friend.P2P_FRIEND_ID);
    }
    
    /**
     * Adds a new list of K => List<V> element to the map if v is not empty,
     * creating the map if it doesn't already exist.
     */
    private <K, V> Map<K, List<V>> addToOrCreateMapOfList(Map<K, List<V>> map, K k, List<V> v) {
        if(!v.isEmpty()) {
            if(map == null) {
                map = new HashMap<K, List<V>>();
            }
            map.put(k, v);
        }
        return map;
    }
    
    /**
     * Notification that a new collection was created. This looks at all the
     * share ids the collection is shared with any adds itself as a backing view
     * to any {@link MultiFileView}s that are mapped by that id.
     * 
     * An event will be triggered for each {@link FileDesc} that was added to
     * each {@link MultiFileView}.
     */
    private void collectionAdded(SharedFileCollection collection) {
        LOG.debugf("New collection {0} added", collection);
        
        Map<FileView, List<FileDesc>> addedFiles = null;
        rwLock.writeLock().lock();
        try {
            sharedCollections.add(collection);
            List<String> friendList = collection.getFriendList();

            // if it was shared with atleast one person, add it to the 'all shared' view.
            if(!friendList.isEmpty()) {
                List<FileDesc> added = allSharedFilesView.addNewBackingView(collection);
                addedFiles = addToOrCreateMapOfList(addedFiles, allSharedFilesView, added);                
            }
            
            for(String id : friendList) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    List<FileDesc> added = view.addNewBackingView(collection);
                    LOG.debugf("Added collection {0} to view {1}, added {2}", collection, view, added);
                    addedFiles = addToOrCreateMapOfList(addedFiles, view, added);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        if(addedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : addedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_ADDED, fd));
                }
            }
        }
    }
    
    /**
     * Notification that a collection was removed. This will remove the
     * collection as a backing view for any {@link MultiFileView}s that were
     * mapped to by any share ids the collection is shared with.
     * 
     * An event will be sent for each {@link FileDesc} that was removed from
     * each {@link MultiFileView}.
     */
    private void collectionRemoved(SharedFileCollection collection) {
        Map<FileView, List<FileDesc>> removedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            sharedCollections.remove(collection);
            
            List<FileDesc> removed = allSharedFilesView.removeBackingView(collection);
            removedFiles = addToOrCreateMapOfList(removedFiles, allSharedFilesView, removed);
            
            for(MultiFileView view : fileViewsPerFriend.values()) {
                removed = view.removeBackingView(collection);
                LOG.debugf("Removed collection {0} from view {1}, added {2}", collection, view, removed);
                removedFiles = addToOrCreateMapOfList(removedFiles, view, removed);
            }
        } finally {
            rwLock.writeLock().unlock();
        }   
        
        if(removedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : removedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_REMOVED, fd));
                }
            }
        }
    }
    
    /**
     * Notification that a collection is shared with another person.
     * 
     * This will add the collection as a backing view for the
     * {@link MultiFileView} that exists for this id, if any view exists.
     * 
     * An event will be sent for each {@link FileDesc} that was added to the
     * {@link MultiFileView}.
     */
    private void friendAddedToCollection(SharedFileCollection collection, String id) {
        Map<FileView, List<FileDesc>> addedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            // make sure it's added to the 'all' view --
            // if the all view already contained it, this will return
            // an empty list.
            List<FileDesc> added = allSharedFilesView.addNewBackingView(collection);
            addedFiles = addToOrCreateMapOfList(addedFiles, allSharedFilesView, added);
            
            MultiFileView view = fileViewsPerFriend.get(id);
            if(view != null) {
                added = view.addNewBackingView(collection);
                LOG.debugf("Friend {0} added to collection {1}, changing view {2}, added {3}", id, collection, view, added);
                addedFiles = addToOrCreateMapOfList(addedFiles, view, added);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(addedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : addedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_ADDED, fd));
                }
            }
        }
    }
    
    /**
     * Notification that a collection is no longer shared with a person.
     * 
     * This will remove the collection as a backing view from the
     * {@link MultiFileView} for that id, if any view exists.
     * 
     * An event will be sent for each {@link FileDesc} that was removed from the
     * {@link MultiFileView}.
     */
    private void friendRemovedFromCollection(SharedFileCollection collection, String id) {
        Map<FileView, List<FileDesc>> removedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            // if there are no more friends this collection is shared with,
            // remove it from the 'all' view.
            if(collection.getFriendList().isEmpty()) {
                List<FileDesc> removed = allSharedFilesView.removeBackingView(collection);
                removedFiles = addToOrCreateMapOfList(removedFiles, allSharedFilesView, removed);
            }
            
            MultiFileView view = fileViewsPerFriend.get(id);
            if(view != null) {
                List<FileDesc> removed = view.removeBackingView(collection);
                LOG.debugf("Friend {0} removed from collection {1}, changing view {2}, removed {2}", id, collection, view, removed);
                removedFiles = addToOrCreateMapOfList(removedFiles, view, removed);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(removedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : removedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_REMOVED, fd));
                }
            }
        }
    }

    /**
     * Notification that a collection's set of shared friends changed.
     * 
     * This will add the collection as a backing view for the
     * {@link MultiFileView} that exists for the new ids and remove the
     * collection as a backing view for any old ids.
     * 
     * An event will be sent for each {@link FileDesc} that was added or removed from
     * the views.
     */
    private void friendIdsChangedInCollection(SharedFileCollection collection, Collection<String> oldIds, Collection<String> newIds) {
        Map<FileView, List<FileDesc>> addedFiles   = null;
        Map<FileView, List<FileDesc>> removedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            if(newIds.isEmpty()) { // if new is empty, must remove files from global list.
                List<FileDesc> removed = allSharedFilesView.removeBackingView(collection);
                removedFiles = addToOrCreateMapOfList(removedFiles, allSharedFilesView, removed);
            } else if(oldIds.isEmpty()) { // if old was empty, must add files to global list.
                List<FileDesc> added = allSharedFilesView.addNewBackingView(collection);
                addedFiles = addToOrCreateMapOfList(addedFiles, allSharedFilesView, added);
            }
            
            // Add any ids that were added.
            List<String> addedFriends = new ArrayList<String>(newIds);
            addedFriends.removeAll(oldIds);
            for(String id : addedFriends) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    List<FileDesc> added = view.addNewBackingView(collection);
                    LOG.debugf("Friend {0} added to collection {1}, changing view {2}, added {3}", id, collection, view, added);
                    addedFiles = addToOrCreateMapOfList(addedFiles, view, added);
                }
            }
            
            // Remove any ids that were removed.
            List<String> removedFriends = new ArrayList<String>(oldIds);
            removedFriends.removeAll(newIds);
            for(String id : removedFriends) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    List<FileDesc> removed = view.removeBackingView(collection);
                    LOG.debugf("Friend {0} removed from collection {1}, changing view {2}, removed {2}", id, collection, view, removed);
                    removedFiles = addToOrCreateMapOfList(removedFiles, view, removed);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(addedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : addedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_ADDED, fd));
                }
            }
        }
        
        if(removedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : removedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_REMOVED, fd));
                }
            }
        }
    }
    
    /** Notification that the library was cleared, and we need to clean all our views. */
    private void clearAllViews() {
        List<FileView> clearedViews = new ArrayList<FileView>();
        
        rwLock.writeLock().lock();
        try {
            if(allSharedFilesView.size() > 0) {
                allSharedFilesView.clear();
                clearedViews.add(allSharedFilesView);
            }
            
            for(MultiFileView view : fileViewsPerFriend.values()) {
                if(view.size() > 0) {
                    view.clear();
                    clearedViews.add(view);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        for(FileView view : clearedViews) {
            multicaster.broadcast(new FileViewChangeEvent(view, Type.FILES_CLEARED, true));
        }
        
    }
    
    /**
     * Notification that a particular collection was cleared.
     * @param collection
     */
    private void collectionCleared(SharedFileCollection collection) {
        Map<FileView, List<FileDesc>> removedFiles = null;
        
        rwLock.writeLock().lock();
        try {
            List<FileDesc> removed = allSharedFilesView.fileViewCleared(collection);
            removedFiles = addToOrCreateMapOfList(removedFiles, allSharedFilesView, removed);
            LOG.debugf("Collection cleared {0}, changing all view, removed {1}", collection, removed);
            
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    removed = view.fileViewCleared(collection);
                    LOG.debugf("Cleared collection {0}, changing view {1}, removed {2}", collection, view, removed);
                    removedFiles = addToOrCreateMapOfList(removedFiles, view, removed);
                }
                
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(removedFiles != null) {
            for(Map.Entry<FileView, List<FileDesc>> entry : removedFiles.entrySet()) {
                for(FileDesc fd : entry.getValue()) {
                    multicaster.broadcast(new FileViewChangeEvent(entry.getKey(), Type.FILE_REMOVED, fd));
                }
            }
        }
    }

    /** 
     * Notification that a file was removed from a shared collection.
     * 
     * If that collection was shared with any friends and views are created for
     * those friends, then the file is attempted to be removed from the view.
     * The file will only be removed from the view if it was only shared
     * through this collection (if it was also shared through another collection
     * with the same friend, the file is not removed from the view).
     * 
     * If forceRemoval is true, this will remove the file from any views this backed,
     * regardless of if the view is backed by other collections that may have
     * the file.
     */
    private void fileRemovedFromCollection(FileDesc fileDesc, SharedFileCollection collection, boolean forceRemoval) {
        List<FileView> removedViews = null;
        
        rwLock.writeLock().lock();
        try {
            if(allSharedFilesView.fileRemovedFromView(fileDesc, collection, forceRemoval)) {
                removedViews = new ArrayList<FileView>();
                removedViews.add(allSharedFilesView);
            }
            
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    if(view.fileRemovedFromView(fileDesc, collection, forceRemoval)) {
                        if(removedViews == null) {
                            removedViews = new ArrayList<FileView>();
                        }
                        removedViews.add(view);
                        LOG.debugf("File {0} removed from collection {1}, changing view {2}", fileDesc, collection, view);
                    } else {
                        if(LOG.isDebugEnabled()) {
                            LOG.debugf("File {0} removed from collection {1}, but didn't change view {2}.  View contains file? {3}", fileDesc, collection, view, view.contains(fileDesc));
                        }
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(removedViews != null) {
            for(FileView view : removedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_REMOVED, fileDesc));
            }
        }
    }

    /**
     * Essentially does a combination of
     * {@link #fileRemovedFromCollection(FileDesc, SharedFileCollection)} &
     * {@link #fileAddedToCollection(FileDesc, SharedFileCollection)}, except it
     * sends a change event if succesful (and a remove event if the new one
     * could not be added).
     */
    private void fileChangedInCollection(FileDesc newFileDesc, FileDesc oldFileDesc, SharedFileCollection collection) {
        List<FileView> changedViews = null;
        List<FileView> removedViews = null;
        boolean addable = isFileAddable(newFileDesc);
        
        rwLock.writeLock().lock();
        try {
            if(allSharedFilesView.fileRemovedFromView(oldFileDesc, collection, false)) {
                if(addable && allSharedFilesView.fileAddedFromView(newFileDesc, collection)) {
                    changedViews = new ArrayList<FileView>();
                    changedViews.add(allSharedFilesView);
                } else {
                    removedViews = new ArrayList<FileView>();
                    removedViews.add(allSharedFilesView);
                }
            }
            
            for(String id : collection.getFriendList()) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    if(view.fileRemovedFromView(oldFileDesc, collection, false)) {
                        if(addable && view.fileAddedFromView(newFileDesc, collection)) {
                            if(changedViews == null) {
                                changedViews = new ArrayList<FileView>();
                            }
                            changedViews.add(view);
                            LOG.debugf("File {0} changed from old file {1} in collection {1}, changing view {2}", newFileDesc, oldFileDesc, collection, view);
                        } else if(!view.contains(newFileDesc)) {
                            if(removedViews == null) {
                                removedViews = new ArrayList<FileView>();
                            }
                            removedViews.add(view);
                            LOG.debugf("File {0} changed from old file {1} in collection {1}, couldn't add new file to view {2}", newFileDesc, oldFileDesc, collection, view);
                        }
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(changedViews != null) {
            for(FileView view : changedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_CHANGED, oldFileDesc, newFileDesc));
            }
        }
        if(removedViews != null) {
            for(FileView view : removedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_REMOVED, oldFileDesc));
            }
        }
    }
    
    /**
     * Notification that metadata about a filedesc has changed in a shared collection.
     * 
     * This could be because URNs added, or metadata changed.  We have to take different
     * actions in different situations here.  If a URN was added, we want to add the file
     * to the views.  If metadata changed, we want to possibly remove the file from the view,
     * because it may be a store file (which shouldn't be in views!).  If the file already existed
     * in the collection, we just want to change the metadata about it.
     */
    private void fileMetaChangedInCollection(FileDesc fd, SharedFileCollection collection) {
        if(isFileAddable(fd)) {
            fileAddedToCollection(fd, collection, true);
        } else {
            fileRemovedFromCollection(fd, collection, true);
        }
    }

    /**
     * Notification that a file was added to a shared collection.
     * 
     * If the collection was shared with any friends that have a view created,
     * and the file does not already exist in the view, the file will be added
     * to the view and an event will be sent for that view.
     * 
     * If the file already existed in the collection, and change is true,
     * we send a notification that the meta changed for the file.
     */
    private void fileAddedToCollection(FileDesc fileDesc, SharedFileCollection collection, boolean changed) {
        List<FileView> addedViews = null;
        List<FileView> changedViews = null;
        
        rwLock.writeLock().lock();
        try {
            List<String> friendList = collection.getFriendList();
            if(!friendList.isEmpty()) {
                // if the list was shared with anyone, add it to our all shared list!
                if(allSharedFilesView.fileAddedFromView(fileDesc, collection)) {
                    addedViews = new ArrayList<FileView>();
                    addedViews.add(allSharedFilesView);
                } else if(changed) {
                    // it was already in the view, so fire a changed event.
                    changedViews = new ArrayList<FileView>();
                    changedViews.add(allSharedFilesView);
                }
            }
            
            for(String id : friendList) {
                MultiFileView view = fileViewsPerFriend.get(id);
                if(view != null) {
                    if(view.fileAddedFromView(fileDesc, collection)) {
                        if(addedViews == null) {
                            addedViews = new ArrayList<FileView>();
                        }
                        addedViews.add(view);
                        LOG.debugf("File {0} added to collection {1}, changing view {2}", fileDesc, collection, view);
                    } else if(changed) {
                        LOG.debugf("File {0} changed in collection {1}, changing view {2}", fileDesc, collection, view);
                        if(changedViews == null) {
                            changedViews = new ArrayList<FileView>();
                        }
                        changedViews.add(view);
                    } else {
                        if(LOG.isDebugEnabled()) {
                            LOG.debugf("File {0} added to collection {1}, but didn't change view {2}, view contains file ? {3}", fileDesc, collection, view, view.contains(fileDesc));
                        }
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(addedViews != null) {
            for(FileView view : addedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_ADDED, fileDesc));
            }
        }
        
        if(changedViews != null) {
            for(FileView view : changedViews) {
                multicaster.broadcast(new FileViewChangeEvent(view, Type.FILE_META_CHANGED, fileDesc));
            }
        }
    }
    
    @Override
    public FileView getFileViewForId(String id) {
        MultiFileView view;
        rwLock.readLock().lock();
        try {
            view = fileViewsPerFriend.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
        
        if(view == null) {        
            rwLock.writeLock().lock();
            try {
                // recheck within lock -- it may have been created
                view = fileViewsPerFriend.get(id);
                if(view == null) {
                    view = createFileView(id);
                    fileViewsPerFriend.put(id, view);
                }
            } finally {
                rwLock.writeLock().unlock();   
            }
        }
        
        return view;
    }
    
    private MultiFileView createFileView(String id) {
        LOG.debugf("Creating new file view for id {0}", id);
        MultiFileView view = new MultiFileView(id);
        initialize(view, id);
        return view;
    }
    
    private void initialize(MultiFileView view, String id) {
        for(SharedFileCollection collection : sharedCollections) {
            if(collection.getFriendList().contains(id)) {
                LOG.debugf("Adding backing view of {0} to view for id {1}", collection, id);
                view.addNewBackingView(collection);
            }
        }
    }

    /**
     * Determines if the FileDesc can be added to a view. FileDescs without URNs
     * and files that are shareable.
     */
    private boolean isFileAddable(FileDesc fd) {
        return fd.getSHA1Urn() != null && fd.isShareable();
    }

    /**
     * An implementation of {@link FileView} that is backed by other FileViews.
     * This implementation is intended to work in concert with {@link FileViewManagerImpl}
     * and is used to return the view of files that any single person in a 
     * {@link SharedFileCollection#getFriendList()}.  Many different collections
     * can be shared with a single id.  This {@link MultiFileView} represents a view
     * of everything that is shared with that id.
     */
    private class MultiFileView extends AbstractFileView {
        
        /*
         * A note about locking:
         *  All write locking is performed by FileViewManagerImpl.
         *  This works because there are no public methods in this class
         *  that are mutable.
         */
        
        /** All views this is backed off of. */
        private final List<FileView> backingViews = new ArrayList<FileView>();
        
        private volatile long totalFileSize = 0;
        
        private final String name;
        
        MultiFileView(String name) {
            super(FileViewManagerImpl.this.library);
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
        @Override
        public long getNumBytes() {
            return totalFileSize;
        }

        @Override
        public void addListener(EventListener<FileViewChangeEvent> listener) {
            multicaster.addListener(this, listener);
        }

        @Override
        public Lock getReadLock() {
            return rwLock.readLock();
        }

        @Override
        public Iterator<FileDesc> iterator() {
            rwLock.readLock().lock();
            try {
                return new FileViewIterator(library, new IntSet(getInternalIndexes()));
            } finally {
                rwLock.readLock().unlock();
            }
        }

        @Override
        public Iterable<FileDesc> pausableIterable() {
            return new Iterable<FileDesc>() {
                @Override
                public Iterator<FileDesc> iterator() {
                    return MultiFileView.this.iterator();
                }
            };
        }

        @Override
        public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
            return multicaster.removeListener(this, listener);
        }
        
        /**
         * Instructs the view to clear all its items.
         * This is typically because the library was cleared.
         * All collections are still backing collections.
         */
        void clear() {
            getInternalIndexes().clear();
            totalFileSize = 0;
        }
        
        /**
         * Removes a backing {@link FileView}. Not every FileDesc in the backing
         * view will necessarily be removed. This is because the FileDesc may exist
         * in another view that this is backed by.
         * 
         * @return A list of {@link FileDesc}s that were removed from this view.
         */
        List<FileDesc> removeBackingView(FileView view) {
            if (backingViews.remove(view)) {
                return validateItems();
            } else {
                return Collections.emptyList();
            }
        }

        /**
         * Adds a new backing {@link FileView}. Not every FileDesc in the backing
         * view will necessarily be added. This is because some FileDescs may
         * already exist due to other backing views.
         * 
         * @return A list of {@link FileDesc}s that were added.
         */
        List<FileDesc> addNewBackingView(FileView view) {
            if(!backingViews.contains(view)) {
                backingViews.add(view);
            
                // lock the backing view in order to iterate through its
                // indexes.
                List<FileDesc> added = new ArrayList<FileDesc>(view.size());
                view.getReadLock().lock();
                try {
                    IntSetIterator iter = ((AbstractFileView)view).getInternalIndexes().iterator();
                    while(iter.hasNext()) {
                        int i = iter.next();
                        FileDesc fd = library.getFileDescForIndex(i);
                        if(isFileAddable(fd) && getInternalIndexes().add(i)) {
                            added.add(fd);
                            totalFileSize += fd.getFileSize();
                        }
                    }
                } finally {
                    view.getReadLock().unlock();
                }
                
                return added;
            } else {
                // we already had this backing view -- no need to redo it.
                return Collections.emptyList();
            }
            
        }

        /**
         * Notification that a backing {@link FileView} has been cleared.
         * 
         * @return A list of {@link FileDesc}s that were removed from this view due
         *         to being removed from the backing view.
         */
        List<FileDesc> fileViewCleared(FileView fileView) {
            if(backingViews.contains(fileView)) {
                return validateItems();
            } else {
                return Collections.emptyList();
            }
        }

        /**
         * Notification that a {@link FileDesc} was removed from a backing {@link FileView}.
         * If force is true, this will remove the FileDesc even if other backing
         * views contained it.
         * 
         * @return true if the file used to exist in this view (and is now removed).
         *         false if it did not exist in this view or still exists.
         */
        boolean fileRemovedFromView(FileDesc fileDesc, FileView fileView, boolean force) {
            if(!force) {
                for(FileView view : backingViews) {
                    if(view.contains(fileDesc)) {
                        return false;
                    }
                }
            }
            getInternalIndexes().remove(fileDesc.getIndex());
            totalFileSize -= fileDesc.getFileSize();
            return true;
        }

        /**
         * Notification that a {@link FileDesc} was adding to a backing
         * {@link FileView}.
         * 
         * @return true if the {@link FileDesc} was succesfully added to this view.
         *         false if the FileDesc already existed in the view.
         */
        boolean fileAddedFromView(FileDesc fileDesc, FileView fileView) {
            boolean added = getInternalIndexes().add(fileDesc.getIndex());
            if(added) {
                totalFileSize += fileDesc.getFileSize();
            }
            return added;
        }
        
        /**
         * Calculates what should be in this list based on the current
         * views in {@link #backingViews}.  This will return a list
         * of {@link FileDesc} that is every removed item.
         * This method does not expect items to be added that were
         * not already contained.
         */
        private List<FileDesc> validateItems() {
            // Calculate the current FDs in the set.
            IntSet newItems = new IntSet();
            for(FileView view : backingViews) {
                view.getReadLock().lock();
                try {
                    newItems.addAll(((AbstractFileView)view).getInternalIndexes());                
                } finally {
                    view.getReadLock().unlock();
                }
            }
            
            library.filterIndexes(newItems, new Predicate<FileDesc>() {
                @Override
                public boolean apply(FileDesc t) {
                    return isFileAddable(t);
                }
            });
            
            // Calculate the FDs that were removed.
            List<FileDesc> removedFds;
            IntSet indexes = getInternalIndexes();
            indexes.removeAll(newItems);
            if(indexes.size() == 0) {
                removedFds = Collections.emptyList();
            } else {
                removedFds = new ArrayList<FileDesc>(indexes.size());
                IntSetIterator iter = indexes.iterator();
                while(iter.hasNext()) {
                    FileDesc fd = library.getFileDescForIndex(iter.next());
                    if(fd != null) {
                        removedFds.add(fd);
                        totalFileSize -= fd.getFileSize();
                    }                
                }
            }
            
            // Set the current FDs & return the removed ones.
            indexes.clear();
            indexes.addAll(newItems);
            return removedFds;
        }
    }    
}
