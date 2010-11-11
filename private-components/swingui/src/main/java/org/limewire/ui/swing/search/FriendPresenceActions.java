package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;

/**
 * This interface describes methods that can be invoked from the FromWidget.
 */
public interface FriendPresenceActions {
    
    /** Chats with a particular friend. */
    void chatWith(Friend person);
    
    /**
     * Spawns a tab for viewing a single friend's library.
     * The friend must not be anonymous.
     */
    void viewFriendLibrary(Friend person);
    
    /**
     * Spawns a tab for viewing one or more presences.
     */
    void viewLibrariesOf(Collection<FriendPresence> people);

    /** Spawns the 'All Friends' tab for viewing all friend's libraries. 
     * @param forceRefresh Refreshes the list of available files if the panel already exists in a tab 
     * and updates are available.
     */
    void browseAllFriends(boolean forceRefresh);
}
