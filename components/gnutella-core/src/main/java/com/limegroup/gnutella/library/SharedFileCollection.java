package com.limegroup.gnutella.library;

import java.util.List;

/** A {@link SmartFileCollection} that is shared with one of more people. */
public interface SharedFileCollection extends FileCollection {
    
    /** Sets a new name for this collection. */
    void setName(String name);
    
    /** Returns the unique id of this collection. */
    int getId();
    
    /** Returns the current list of people this collection is shared with. */
    List<String> getFriendList();
    
    /** Adds a new person to the list of people the collection is shared with. */
    void addFriend(String friendId);
    
    /**
     * Removes a person from the list of people this collection is shared with.
     * Returns true if the person was removed.
     */
    boolean removeFriend(String friendId);
    
    /** Sets the new list of people this collection should be shared with. */
    void setFriendList(List<String> friendIds);

    /**
     * Returns true if this is a public collection. 
     */
    boolean isPublic();

}
