package org.limewire.ui.swing.friends.chat;

import java.beans.PropertyChangeListener;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;

public interface ChatFriend {

    /**
     * @return User object used by this chat friend
     */
    Friend getFriend();

    /**
     * @return the user id corresponding to this chat friend.  This is typically the id
     * used by this chat friend to sign on.
     */
    String getID();

    /**
     * @return The display String identifying this chat friend
     */
    String getName();

    /**
     * @return the status message
     */
    String getStatus();

    /**
     * @return the presence status ("available", "away", etc)
     */
    FriendPresence.Mode getMode();

    /**
     * @return true if this chat has been marked as started, but has
     * not been stopped.
     */
    boolean isChatting();

    /**
     * @return true if any presences of the user are signed in thru LimeWire
     */
    boolean isSignedInToLimewire();

    /**
     * @return true if this chat friend is currently signed in
     */
    boolean isSignedIn();

    /**
     * If not yet started, marks the current chat as started.
     */
    void startChat();

    /**
     * If chat is currently started, marks the chat as stopped.
     */
    void stopChat();

    /**
     * Gets the time at which the chat started.
     * For example, normally a chat can be considered started upon the first sign
     * of communication between the current connection and this chat user.
     *
     * @return start chat time in milliseconds
     */
    long getChatStartTime();

    /**
     *
     * @return whether or not this chat user has received messages that have
     * yet to be displayed in the chat window
     */
    boolean hasUnviewedMessages();

    /**
     * Returns true if has unviewedMessages and should be displayed in a flash state.
     */
    boolean isFlashState();
    
    /**
     * Set whether or not this chat user has received messages that have yet to be displayed
     * in the chat window.
     *
     * @param hasMessages true if this chat user has received messages not yet displayed
     */
    void setHasUnviewedMessages(boolean hasMessages);

    /**
     * Creates and wires together the necessary objects for
     * sending and receiving messages.
     *
     * @param reader the chat implementation calls into the {@link MessageReader} upon
     *        receiving messages and updates in chat state
     * @return messageWriter {@link MessageWriter} implementation on which 
     * methods are called to send messages and update chat state.
     */
    MessageWriter createChat(MessageReader reader);

    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Updates the state of this chatFriend based on its underlying attributes, for instance 
     * the mode and status of the current active presence
     */
    void update();
}
