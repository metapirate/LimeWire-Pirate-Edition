package org.limewire.ui.swing.friends.chat;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Closes a chat message with a given friend.
 */
public class CloseChatMessage extends AbstractAction {

    private final Provider<ChatFrame> chatFrame;
    private final Provider<ConversationPanel> conversationPanel;
    
    @Inject
    public CloseChatMessage(Provider<ChatFrame> chatFrame,
            Provider<ConversationPanel> conversationPanel) {
        this.chatFrame = chatFrame;
        this.conversationPanel = conversationPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ChatFriend id = conversationPanel.get().getCurrentConversationFriend();
        if(id != null) {
            conversationPanel.get().removeConversation(id);
            chatFrame.get().closeConversation(id);
        }
    }
}
