package org.limewire.friend.api;

/**
 * Describes an interface for receiving the user's decision about
 * whether to accept or decline an incoming friend request. 
 */
public interface FriendRequestDecisionHandler {
    public void handleDecision(String friendUsername, boolean accepted);
}
