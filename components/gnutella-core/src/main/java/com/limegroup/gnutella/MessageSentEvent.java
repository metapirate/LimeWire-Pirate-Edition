package com.limegroup.gnutella;

import org.limewire.listener.DataEvent;
import org.limewire.listener.SourcedEvent;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.Message;

/**
 * Event to notify interested parties of messages that have been sent over
 * a Gnutella connection.
 * <p>
 * Note that messages can also be dropped and never be sent, so code shouldn't
 * solely rely on this event, unless it is clear that the type of message
 * will never be dropped. 
 */
public class MessageSentEvent implements SourcedEvent<RoutedConnection>, DataEvent<Message> {

    private final RoutedConnection routedConnection;
    private final Message message;

    public MessageSentEvent(RoutedConnection routedConnection, Message message) {
        this.routedConnection = routedConnection;
        this.message = message;
    }
    
    @Override
    public RoutedConnection getSource() {
        return routedConnection;
    }

    @Override
    public Message getData() {
        return message;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
