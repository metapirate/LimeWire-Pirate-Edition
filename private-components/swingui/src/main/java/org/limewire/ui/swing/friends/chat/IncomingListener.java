package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.MessageWriter;

/**
 * Listens for incoming messages.
 */
public interface IncomingListener {

    public void incomingChat(ChatFriend chatFriend, MessageWriter messageWriter);
}
