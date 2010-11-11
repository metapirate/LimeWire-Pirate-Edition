package org.limewire.friend.api;

/**
 * Allows you to add and remove known and available friends.
 */
public interface MutableFriendManager extends FriendManager {
    /**
     * Adds a known friend.
     */
    void addKnownFriend(Friend friend);
    /**
     * Removes a known friend.
     * 
     * Called on disconnect from a friend network or when a friend is
     * deleted from the list of friends.
     * 
     * @param delete if true the friend is deleted from the list of friends
     */
    void removeKnownFriend(Friend friend, boolean delete);
    /**
     * Adds an available (online) friend. 
     */
    void addAvailableFriend(Friend friend);
    /**
     * Removes an available (online) friend, making it thus offline. 
     */
    void removeAvailableFriend(Friend friend);
}
