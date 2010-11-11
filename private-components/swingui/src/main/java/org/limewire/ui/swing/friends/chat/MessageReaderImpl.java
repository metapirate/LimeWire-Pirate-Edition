package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.MessageReader;
import org.limewire.listener.EventBroadcaster;

class MessageReaderImpl implements MessageReader {
    private final ChatFriend chatFriend;
    
    private final EventBroadcaster<ChatMessageEvent> messageList;
    private final EventBroadcaster<ChatStateEvent> chatStateList;

    MessageReaderImpl(ChatFriend chatFriend,
                      EventBroadcaster<ChatMessageEvent> messageList,
                      EventBroadcaster<ChatStateEvent> chatStateList) {
        this.chatFriend = chatFriend;
        this.messageList = messageList;
        this.chatStateList = chatStateList;
    }

    @Override
    public void readMessage(final String message) {
        if (message != null) {
            final Message msg = newMessage(message, Message.Type.RECEIVED);
            messageList.broadcast(new ChatMessageEvent(msg));
        }
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(chatFriend.getName(), chatFriend.getID(), type, message);
    }

    @Override
    public void newChatState(ChatState chatState) {
        chatStateList.broadcast(new ChatStateEvent(chatFriend.getID(), chatState));
    }
    
    @Override
    public void error(String errorMessage) {
        ErrorMessage errMsg = new ErrorMessage(chatFriend.getID(), 
            errorMessage, Message.Type.SERVER);
        messageList.broadcast(new ChatMessageEvent(errMsg));
    }
}
