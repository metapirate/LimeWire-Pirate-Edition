package com.limegroup.gnutella.library;

import java.util.Collection;

import org.limewire.listener.SourcedEvent;
import org.limewire.util.StringUtils;

/** A change event for a {@link SharedFileCollection}. */
public class SharedFileCollectionChangeEvent implements SourcedEvent<SharedFileCollection> {
    
    public static enum Type {
        COLLECTION_ADDED, COLLECTION_REMOVED, FRIEND_ADDED, FRIEND_REMOVED, FRIEND_IDS_CHANGED, NAME_CHANGED;
    }
    
    private final Type type;
    private final SharedFileCollection list;
    private final String shareId;
    private final Collection<String> newIds;
    private final Collection<String> oldIds;
    private final String newName;
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list) {
        assert type == Type.COLLECTION_ADDED || type == Type.COLLECTION_REMOVED;        
        this.type = type;
        this.list = list;
        this.shareId = null;
        this.newIds = null;
        this.oldIds = null;
        this.newName = null;
    }
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list, String idOrName) {
        assert type == Type.FRIEND_ADDED || type == Type.FRIEND_REMOVED || type == Type.NAME_CHANGED;
        this.type = type;
        this.list = list;
        this.shareId = type == Type.NAME_CHANGED ? null : idOrName;
        this.newIds = null;
        this.oldIds = null;
        this.newName = type == Type.NAME_CHANGED ? idOrName : null;
    }
    
    public SharedFileCollectionChangeEvent(Type type, SharedFileCollection list, Collection<String> oldIds, Collection<String> newIds) {
        assert type == Type.FRIEND_IDS_CHANGED;
        this.type = type;
        this.list = list;
        this.shareId = null;
        this.newIds = newIds;
        this.oldIds = oldIds;
        this.newName = null;
    }

    public Type getType() {
        return type;
    }

    public String getFriendId() {
        return shareId;
    }

    @Override
    public SharedFileCollection getSource() {
        return list;
    }
    
    public Collection<String> getNewFriendIds() {
        return newIds;
    }
    
    public Collection<String> getOldFriendIds() {
        return oldIds;
    }

    public String getNewName() {
        return newName;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
