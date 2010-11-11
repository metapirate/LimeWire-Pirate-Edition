package org.limewire.friend.api;

import java.util.Collection;

import org.limewire.concurrent.ListeningFuture;

/**
 * Represents a connection to a service that provides means to see friends
 * online and interact with them through LimeWire.
 */
public interface FriendConnection {
    
    public FriendConnectionConfiguration getConfiguration();

    /**
     * Logs a user into the server.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will wrap a FriendException if an error occurs.
     */
    public ListeningFuture<Void> login();
    
    /**
     * Logs a user out of the server.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will wrap a FriendException if an error occurs.
     */
    public ListeningFuture<Void> logout();

    /**
     * @return true if this connection is logged in
     */
    public boolean isLoggedIn();
    
    /**
     * @return true if this connection is now logging in
     */
    public boolean isLoggingIn();

    /**
     * @return If this connection supports the <code>#setMode(FriendPresence.Mode)</code> method
     */
    boolean supportsMode();

    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status).
     * @param mode the new mode to set
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will wrap a FriendException
     * if there is an error sending the mode message
     * @throws UnsupportedOperationException if <code>#supportsMode()</code> is false
     */
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode);

    /**
     * @return if this connection supports the <code>#addFriend(String, String)</code>
     * and <code>#removeFriend(String)</code> methods.
     */
    boolean supportsAddRemoveFriend();

    /**
     * Adds a new friend who is not a friend yet to the list of friends.
     * @param id cannot be null
     * @param name can be null
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * @throws The ExecutionException will be to an FriendException
     * if there is an error sending the add friend message
     * @throws UnsupportedOperationException if <code>#supportsAddRemoveFriend()</code> returns false
     */
    public ListeningFuture<Void> addNewFriend(String id, String name);
    
    /**
     * Remove a user from the friend list.
     * @param id cannot be null
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * @throws The ExecutionException will be to an FriendException
     * if there is an error sending the removeFriend message
     * @throws UnsupportedOperationException if <code>#supportsAddRemoveFriend()</code> returns false
     */
    public ListeningFuture<Void> removeFriend(String id);

    /**
     * Returns the user belonging to <code>id</code>. <code>id</code>
     * is the user's email address.
     * 
     * @return null if id is not registered on this connection
     */
    public Friend getFriend(String id);

    /**
     * @return a copy of the current Collection of Users. Does NOT stay up to
     * date with changes.
     */
    public Collection<Friend> getFriends();
}