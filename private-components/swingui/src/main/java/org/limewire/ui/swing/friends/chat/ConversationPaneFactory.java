package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.MessageWriter;

/**
 * Creates a new UI Component for displaying a conversation with a given friend.
 */
interface ConversationPaneFactory {
    ConversationPane create(MessageWriter writer, ChatFriend chatFriend);
}
