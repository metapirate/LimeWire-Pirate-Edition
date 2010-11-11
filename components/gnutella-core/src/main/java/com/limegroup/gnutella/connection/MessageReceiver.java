package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Notifications & information about asynchronous message processing.
 */
public interface MessageReceiver {
    
    /**
     * Notification that a message is available for processing.
     */
    public void processReadMessage(Message m) throws IOException;
    
    /**
     * The soft-max this message-receiver uses for creating messages.
     */
    public byte getSoftMax();
    
    /**
     * The network this message-receiver uses for creating messages.
     */
    public Network getNetwork();
    
    /**
     * Notification that the stream is closed.
     */
    public void messagingClosed();
}
        