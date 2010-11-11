package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

/** Simple interfaces that allows a callback of 'sent' messages. */
public interface SentMessageHandler {
    
    public void processSentMessage(Message m);
    
}