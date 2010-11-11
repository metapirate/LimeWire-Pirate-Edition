package org.limewire.core.api.library;

import java.util.List;

import ca.odell.glazedlists.EventList;

/** The core API analog to {@link com.limegroup.gnutella.library.SharedFileCollection}. */
public interface SharedFileList extends LocalFileList {
    
    /** Returns all friend IDs this list is shared with. */
    EventList<String> getFriendIds();
    
    /** Adds a new friend id to share this list with. */
    void addFriend(String friendId);
    
    /** Removes a friend id from the list of friends this is shared with. */
    void removeFriend(String friendId);
    
    /** Sets the current friend share list. */
    void setFriendList(List<String> friendIdList);
    
    /** Returns the name of this collection. */
    String getCollectionName();
    
    /** Sets a new name for this collection. */
    void setCollectionName(String name);
    
    /** Returns true if name changing is allowed for this collection. */
    boolean isNameChangeAllowed();
    
    /** Returns true if this is a public share list. */
    boolean isPublic();
    
    /** Returns a unique identifier for this list. */
    int getId();

}
