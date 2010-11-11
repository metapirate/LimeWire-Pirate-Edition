package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.util.StringUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.SharedFileCollection;

/**
 * A representation of a {@link SharedFileCollection} for use with the
 * {@link SharedFileList} API.
 */
class SharedFileListImpl extends LocalFileListImpl implements SharedFileList {

    private final SharedFileCollection coreCollection;
    private final EventList<String> friendList = GlazedListsFactory.threadSafeList(new BasicEventList<String>());
    private final EventList<String> readOnlyFriendList = GlazedListsFactory.readOnlyList(friendList);

    public SharedFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory,
            SharedFileCollection coreFileCollection) {
        super(new BasicEventList<LocalFileItem>(), coreLocalFileItemFactory);
        this.coreCollection = coreFileCollection;
        this.coreCollection.addListener(newEventListener());
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    @Override
    public int getId() {
        return coreCollection.getId();
    }

    @Override
    protected SharedFileCollection getCoreCollection() {
        return coreCollection;
    }

    @Override
    public void addFriend(String friendId) {
        coreCollection.addFriend(friendId);
    }

    @Override
    public EventList<String> getFriendIds() {
        return readOnlyFriendList;
    }

    @Override
    public String getCollectionName() {
        // TODO: do better.
        if(coreCollection.getId() == 0) {
            return "Public Shared"; // TODO: i18n?
        } else {
            return coreCollection.getName();
        }
    }

    @Override
    public boolean isNameChangeAllowed() {
        return coreCollection.getId() != 0; // TODO: Do better.
    }

    @Override
    public void removeFriend(String friendId) {
        coreCollection.removeFriend(friendId);
    }
    
    @Override
    public void setFriendList(List<String> friendIdList) {
        coreCollection.setFriendList(friendIdList);
    }

    @Override
    public void setCollectionName(String name) {
        coreCollection.setName(name);
    }

    boolean friendRemoved(String friendId) {
        return friendList.remove(friendId);
    }

    void friendsSet(Collection<String> newFriendIds) {
       friendList.getReadWriteLock().writeLock().lock();
       try {
           friendList.clear();
           friendList.addAll(newFriendIds);
       } finally {
           friendList.getReadWriteLock().writeLock().unlock();
       }
    }

    void friendAdded(String friendId) {
        friendList.add(friendId);
    }

    @Override
    public boolean isPublic() {
        return coreCollection.isPublic();
    }

}