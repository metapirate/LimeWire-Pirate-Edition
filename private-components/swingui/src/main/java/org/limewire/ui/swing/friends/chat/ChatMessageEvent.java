package org.limewire.ui.swing.friends.chat;

import org.limewire.listener.DefaultDataEvent;

/**
 * event for new chat message.
 */
public class ChatMessageEvent extends DefaultDataEvent<Message> {
    
    @SuppressWarnings("unused")
    private static volatile boolean hasChatted;
    
    public ChatMessageEvent(Message message) {
        super(message);
        switch (message.getType()) {
            case SENT:
            case RECEIVED:
                hasChatted = true;
        }
    }
}
