package org.limewire.ui.swing.friends.chat;

import java.util.Map;

import javax.swing.JComponent;

import org.limewire.ui.swing.components.Disposable;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendEvent;

/**
 * Interface for chat window ui object.  Intended to be a way
 * to get information about and control what happens to the
 * chat window, other than by way of keyboard input and
 * received instant messages.
 *<p>
 * Make sure that methods here are called
 * from within the EDT.
 *
 */
interface Conversation extends Disposable {

    /**
     * Updates the display of all the messages.
     */
    public void displayMessages();


    /**
     * @return the {@link ChatFriend} associated with this conversation
     */
    public ChatFriend getChatFriend();

    /**
     * @return a read-only map of file ID to file offer message.
     * It is a snapshot of all the file offer messages
     *
     */
    public Map<String, MessageFileOffer> getFileOfferMessages();

    /**
     * Called to indicate a new feature addition/removal.
     * 
     * @param feature the feature being updated
     * @param action whether the feature is added or removed.
     */
    public void featureUpdate(Feature feature, FeatureEvent.Type action);
    
    /**
     * Add a new {@link Message} to this conversation.
     * 
     * @param message being added
     */
    public void newChatMessage(Message message);
    
    /**
     * Called to indicate a new chat state in this conversation.
     * 
     * @param chatState being added.
     */
    public void newChatState(ChatState chatState);
    
    /**
     * Update availability of friend associated with this conversation.
     * 
     * @param update type of update.
     */
    public void friendAvailableUpdate(FriendEvent.Type update);


    /**
     * Return as a swing JComponent.
     * 
     * @return JComponent
     */
    public JComponent asComponent();
    
    // TODO: create a way to get/search for any message(s)
}
