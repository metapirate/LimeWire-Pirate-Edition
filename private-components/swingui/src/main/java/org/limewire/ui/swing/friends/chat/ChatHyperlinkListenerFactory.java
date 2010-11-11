package org.limewire.ui.swing.friends.chat;

/**
 * Factory interface for {@link ChatHyperlinkListener}.
 */
interface ChatHyperlinkListenerFactory {

    ChatHyperlinkListener create(Conversation chat);
}
