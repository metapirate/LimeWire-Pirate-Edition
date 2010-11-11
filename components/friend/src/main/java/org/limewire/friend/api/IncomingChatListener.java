package org.limewire.friend.api;


/**
 * A listener style class that is notified of new incoming chats
 */
public interface IncomingChatListener {

    /**
     * called when a new incoming chat is received
     * @param writer the <code>MessageWriter</code> that can be used to send messages
     * @return the <code>MessageReader</code> that will be called by the xmpp container when
     * incoming messages are received
     */
    public MessageReader incomingChat(MessageWriter writer);
}
