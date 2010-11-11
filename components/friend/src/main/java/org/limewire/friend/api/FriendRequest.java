package org.limewire.friend.api;


/**
 * Contains the information necessary to display an incoming friend request
 * to the user and return the user's decision to the XMPP connection.
 */
public class FriendRequest {

    private final String friendUsername;
    private final FriendRequestDecisionHandler decisionHandler;

    public FriendRequest(String friendUsername,
            FriendRequestDecisionHandler decisionHandler) {
        this.friendUsername = friendUsername;
        this.decisionHandler = decisionHandler;
    }

    public String getFriendUsername() {
        return friendUsername;
    }
    
    public FriendRequestDecisionHandler getDecisionHandler() {
        return decisionHandler;
    }
}
