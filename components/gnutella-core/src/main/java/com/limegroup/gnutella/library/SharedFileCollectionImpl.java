package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.URNSettings;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.hashing.AudioHashingUtils;
import com.limegroup.gnutella.library.SharedFileCollectionChangeEvent.Type;
import com.limegroup.gnutella.tigertree.HashTreeCache;


/**
 * A collection of FileDescs containing files that may be shared with one or more people.
 */
class SharedFileCollectionImpl extends AbstractFileCollection implements SharedFileCollection {
    
    private final int collectionId;    
    private final Provider<LibraryFileData> data;
    private final HashTreeCache treeCache;    
    private final EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster;
    private final List<String> defaultFriendIds;
    private final boolean publicCollection;
    private final CategoryManager categoryManager;
    private final UrnCache urnCache;

    @Inject
    public SharedFileCollectionImpl(Provider<LibraryFileData> data, LibraryImpl managedList, 
                                    SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster,
                                    EventBroadcaster<SharedFileCollectionChangeEvent> sharedCollectionBroadcaster,
                                    CategoryManager categoryManager, UrnCache urnCache,
                                    @Assisted int id, HashTreeCache treeCache,
                                    @Assisted boolean publicCollection,
                                    @Assisted String... defaultFriendIds) {
        super(managedList, multicaster);
        this.collectionId = id;
        this.data = data;
        this.treeCache = treeCache;
        this.sharedBroadcaster = sharedCollectionBroadcaster;
        this.publicCollection = publicCollection;
        this.categoryManager = categoryManager;
        this.urnCache = urnCache;
        if(defaultFriendIds.length == 0) {
            this.defaultFriendIds = Collections.emptyList();
        } else {
            this.defaultFriendIds = Collections.unmodifiableList(Arrays.asList(defaultFriendIds));
        }
    }
    
    @Override
    public int getId() {
        return collectionId;
    }
    
    public String getName() {
        return data.get().getNameForCollection(collectionId);
    }
    
    public void setName(String name) {
        if(data.get().setNameForCollection(collectionId, name)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.NAME_CHANGED, this, name));
        }
    }
    
    @Override
    public void addFriend(String id) {
        if(data.get().addFriendToCollection(collectionId, id)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_ADDED, this, id));
        }
    }
    
    @Override
    public boolean removeFriend(String id) {
        if(data.get().removeFriendFromCollection(collectionId, id)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_REMOVED, this, id));
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public List<String> getFriendList() {
        List<String> cached = data.get().getFriendsForCollection(collectionId);
        if(defaultFriendIds.isEmpty()) {
            return cached;
        } else if(cached.isEmpty()) {
            return defaultFriendIds;
        } else {
            List<String> friends = new ArrayList<String>(cached.size() + defaultFriendIds.size());
            friends.addAll(defaultFriendIds);
            friends.addAll(cached);
            return friends;
        }
    }
    
    @Override
    public void setFriendList(List<String> ids) {
        List<String> oldIds = data.get().setFriendsForCollection(collectionId, ids);
        if(oldIds != null) { // if it changed, broadcast the change.
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_IDS_CHANGED, this, oldIds, ids));
        }
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this) + ", name: " + getName();
    }
    
    @Override
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        if(super.addFileDescImpl(fileDesc)) {
            // if no root, calculate one and propagate it.
            if(fileDesc.getTTROOTUrn() == null) {
                // Schedule all additions for having a hash tree root.
                URN root = treeCache.getOrScheduleHashTreeRoot(fileDesc);
                if(root != null) {
                	for(FileDesc fd : library.getFileDescsMatching(fileDesc.getSHA1Urn())) {
                	    fd.addUrn(root);
                	}
                }
            } 
            // if this file already has a SHA1, try creating the nms1.
            // we want to ensure the SHA1 and FD are valid for this list
            // so if they don't exist, we will wait till after the SHA1
            // has been calculated. the nms1 and sha1 use the same thread
            // and calculating the SHA1 is more important. 
            // if no SHA1 exists yet, we're guarenteed to recieve
            // a FILE_META_CHANGED event
            if(fileDesc.getSHA1Urn() != null) {
                calculateNonMetaDataHash(fileDesc);
            }
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    protected void fileMetaChanged(final FileDesc fileDesc) {
        super.fileMetaChanged(fileDesc);
        
        // if this FileDesc still exists in this list, try creating NMS1.
        // this will only be called on a new library load or if a file
        // was added directly to the shared list. 
        if(contains(fileDesc)) {
            calculateNonMetaDataHash(fileDesc);
        }
    }
    
    /**
     * Attempts to calculate the NMS1 for this FileDesc if we're
     * allowing NMS1 and the FileDesc doesn't already contain it 
     * and it can be created for this file type.
     */
    private void calculateNonMetaDataHash(final FileDesc fileDesc) {
        if(URNSettings.USE_NON_METADATA_HASH.get() && 
            fileDesc.getNMS1Urn() == null &&
            AudioHashingUtils.canCreateNonMetaDataSHA1(fileDesc.getFile())) {
                ListeningFuture<URN> urnFuture = urnCache.calculateAndCacheNMS1(fileDesc.getFile());
                urnFuture.addFutureListener(new EventListener<FutureEvent<URN>>(){
                    @Override
                    public void handleEvent(FutureEvent<URN> event) {
                        URN urn = event.getResult();
                        if(urn != null && urn.isNMS1()) {
                            for(FileDesc fd : library.getFileDescsMatching(fileDesc.getSHA1Urn())) {
                                fd.addUrn(urn);
                            }
                        }
                    }
                });
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        addPendingManagedFiles();
    }
    
    /**
     * This method initializes the friend file list.  It adds the files
     * that are shared with the friend represented by this list.  This
     * is necessary because friend file lists are populated/unpopulated when
     * needed, not upon startup.
     */
    protected void addPendingManagedFiles() {
        // add files from the MASTER list which are for the current friend
        // normally we would not want to lock the master list while adding
        // items internally... but it's OK here because we're guaranteed
        // that nothing is listening to this list, since this will happen
        // immediately after construction.
        library.getReadLock().lock();
        try {
            for (FileDesc fd : library) {
                if(isPending(fd.getFile(), fd)) {
                    add(fd);
                }
            }
        } finally {
            library.getReadLock().unlock();
        }
    }
    
    /**
     * Returns false if it's an {@link IncompleteFileDesc}.
     */
    @Override
    protected boolean isFileDescAllowed(FileDesc fileDesc) {
        if (fileDesc instanceof IncompleteFileDesc) {
            return false;
        } else {
	        return isFileAllowed(fileDesc.getFile());
	    }
    }
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return data.get().isFileInCollection(file, collectionId);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        data.get().setFileInCollection(file, collectionId, added);      
    }
    
    @Override
    protected boolean clearImpl() {
        data.get().setFilesInCollection(this, collectionId, false);
        return super.clearImpl();
    }
    
    @Override
    void dispose() {
        super.dispose();
        data.get().removeCollection(collectionId);
    }
    
    @Override
    protected void fireAddEvent(FileDesc fileDesc) {
        super.fireAddEvent(fileDesc);
    }

    @Override
    protected void fireRemoveEvent(FileDesc fileDesc) {
        super.fireRemoveEvent(fileDesc);
    }

    @Override
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        super.fireChangeEvent(oldFileDesc, newFileDesc);
    }

    @Override
    public boolean isFileAllowed(File file) {
        if(!library.isFileAllowed(file)) {
            return false;
        }
        
        if(isPublic()) {
            Category category = categoryManager.getCategoryForFile(file);
            if(category == Category.DOCUMENT && !LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return library.isDirectoryAllowed(folder);
    }

    @Override
    public boolean isPublic() {
        return publicCollection;
    }
}
