package org.limewire.ui.swing.friends.chat;

import org.limewire.listener.DefaultSourceTypeEvent;
import org.limewire.friend.api.ChatState;

/**
 * event for updated chat state.
 */
public class ChatStateEvent extends DefaultSourceTypeEvent<String, ChatState> {
    
    public ChatStateEvent(String friendId, ChatState event) {
        super(friendId, event);
    }
}
