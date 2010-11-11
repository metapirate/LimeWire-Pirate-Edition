package com.limegroup.gnutella.rudp.messages;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.SynMessage.Role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class LimeRUDPMessageFactory implements RUDPMessageFactory {
    private final RUDPMessageFactory delegate;

    @Inject
    public LimeRUDPMessageFactory(@Named("delegate") RUDPMessageFactory delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate is null");
        } else if (delegate instanceof LimeRUDPMessageFactory) {
            throw new IllegalArgumentException("Recursive delegation");
        }
        
        this.delegate = delegate;
    }
    
    /** Returns the delegate factory. */
    RUDPMessageFactory getDelegate() {
        return delegate;
    }

    public RUDPMessage createMessage(ByteBuffer... data) throws MessageFormatException {
        RUDPMessage msg = delegate.createMessage(data);
        
        if (msg instanceof AckMessage) {
            return new LimeAckMessageImpl((AckMessage)msg);
        } else if (msg instanceof DataMessage) {
            return new LimeDataMessageImpl((DataMessage)msg);
        } else if (msg instanceof FinMessage) {
            return new LimeFinMessageImpl((FinMessage)msg);
        } else if (msg instanceof KeepAliveMessage) {
            return new LimeKeepAliveMessageImpl((KeepAliveMessage)msg);
        } else if (msg instanceof SynMessage) {
            return new LimeSynMessageImpl((SynMessage)msg);
        }
        
        throw new IllegalArgumentException(msg.getClass() + " is unhandled");
    }

    public AckMessage createAckMessage(byte connectionID, long sequenceNumber, long windowStart, int windowSpace) {
        return new LimeAckMessageImpl(
                delegate.createAckMessage(connectionID, sequenceNumber, windowStart, windowSpace));
    }

    public DataMessage createDataMessage(byte connectionID, long sequenceNumber, ByteBuffer chunk) {
        return new LimeDataMessageImpl(
                delegate.createDataMessage(connectionID, sequenceNumber, chunk));
    }

    public FinMessage createFinMessage(byte connectionID, long sequenceNumber, byte reasonCode) {
        return new LimeFinMessageImpl(
                delegate.createFinMessage(connectionID, sequenceNumber, reasonCode));
    }

    public KeepAliveMessage createKeepAliveMessage(byte connectionID, long windowStart, int windowSpace) {
        return new LimeKeepAliveMessageImpl(
                delegate.createKeepAliveMessage(connectionID, windowStart, windowSpace));
    }

    public SynMessage createSynMessage(byte connectionID, Role role) {
        return new LimeSynMessageImpl(
                delegate.createSynMessage(connectionID, role));
    }

    public SynMessage createSynMessage(byte connectionID, byte theirConnectionID, Role role) {
        return new LimeSynMessageImpl(
                delegate.createSynMessage(connectionID, theirConnectionID, role));
    }
}
