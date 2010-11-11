package org.limewire.friend.api;

import java.util.Map;

import org.limewire.listener.EventListener;

/** 
 * A Friend.
 */
public interface Friend {

    public static final String P2P_FRIEND_ID = "_@_GNUTELLA_@_";

    /**
     * Returns the ID of the friend.  This can be any form of unique ID.
     * For example, an friend can be in the form of <code>friend@host.com</code>,
     * whereas a Gnutella Friend can be the clientGUID.
     */
    public String getId();

    /**
     * Return the friendly given name to the friend, can be null.
     * For example, a friend can be the alias of the friend,
     * where a Gnutella friend can be the IP address.
     * */
    public String getName();
    
    /** Returns the best possible name this can be rendered with.
     *  
     *  NOTE: must not return null. 
     */
    public String getRenderName();
    
    /** If getRenderName returns something other than email, will return subString using the first ' ' delimeter*/
    public String getFirstName();

    /** Sets a new name for this Friend. */
    void setName(String name);
    
    /**
     * Returns true if this is an anonymous friend.
     * For example, an XMPP friend is not anonymous -- it is identified
     * by an email address and is permanent.  A Gnutella Friend is anonymous,
     * in that their existence is temporary and no long-lasting relationship
     * exists.
     * 
     * Callers can use this to determine if data based on this friend is
     * permanent or not.
     */
    boolean isAnonymous();
    
    /** Returns the {@link Network} that this is a friend on. */
    Network getNetwork();

    /**
     * Allows to register a listener for presence changes of this friend
     */
    void addPresenceListener(EventListener<PresenceEvent> presenceListener);

    /**
     * Used to initiate a new chat.
     * @param reader the <code>MessageReader</code> used to process incoming messages
     * @return the <code>MessageWriter</code> used to send outgoing messages
     */
    MessageWriter createChat(MessageReader reader);

    /**
     * Used to register a listener for new incoming chats.  If a chat listener is already set,
     * it is necessary to remove it prior to setting a new one. Does nothing if chat
     * listener is already set.
     *
     * @param listener the <code>IncomingChatListener</code> to be used
     */
    void setChatListenerIfNecessary(IncomingChatListener listener);

    /**
     * Used for removing the existing listener set in {@link #setChatListenerIfNecessary}
     * for new incoming chats.  Does nothing if there is no chat listener set.
     */
    void removeChatListener();

    /**
     * The active presence is the presence currently
     * chatting with (sending msgs to) this client's presence.
     *
     * @return presence the active presence.  null if there is no active presence
     */
    FriendPresence getActivePresence();

    /**
     * @return true if this friend has an associated active presence
     * (presence this friend is currently chatting with)
     */
    boolean hasActivePresence();

    /**
     * Returns whether or not this friend is signed in to chat.
     * @return true if this friend is signed in with at least 1 presence
     */
    boolean isSignedIn();

    /**
     * Returns a map of all {@link FriendPresence FriendPresences} for this
     * Friend. Keys are the identifier of the presence, as defined by
     * {@link FriendPresence#getPresenceId()}.
     */
    Map<String, FriendPresence> getPresences();

    /**
     * Returns whether the current login is subscribed to this friend.
     * This information is in the roster packet.
     * <p>
     * For instance, if a friend sends the current login a friend
     * add request, and the current login accepts, this method
     * will return true.
     * <p>
     * In the following roster packet, my-mutually-accepted-friend is subscribed,
     * and friend-i-rejected-previously and friend-i-requested-but-has-not-responded
     * are not subscribed.
     * <xmp>
     * <iq to="limebuddytest@gmail.com/WuXLh6tmNLC3320061" id="0Qj6D-15" type="result">
     *   <query xmlns="jabber:iq:roster">
     *     <item jid="my-mutually-accepted-friend@gmail.com" subscription="both" name="Lime Friend">
     *     <item jid="friend-i-rejected-previously@gmail.com" subscription="none"/>
     *     <item jid="friend-i-requested-but-has-not-responded@gmail.com" subscription="none"/>
     *   </query>
     * </iq>
     * </xmp>
     * @return true if the roster entry for this friend has
     * a subscription attribute equal to "both" or "to"
     * Returns false otherwise ("from" or "none")
     */
    boolean isSubscribed();
}
