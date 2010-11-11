package org.limewire.friend.api;



/**
 * Called by the user of the xmpp container to send a chat message
 */
public interface MessageWriter {

    /**
     * Sends a message to the <code>Presence</code>; blocking call.
     * @param message
     * @throws FriendException
     */
    public void writeMessage(String message) throws FriendException;

    /**
     * If necessary, sends a message indicating the new
     * chat state
     *
     * @param chatState
     * @throws FriendException
     */
    public void setChatState(ChatState chatState) throws FriendException;
}
