package com.limegroup.gnutella.dht.messages;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.MessageFormatException;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.StatsRequest;
import org.limewire.mojito.messages.StatsResponse;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;


/**
 * Takes a true <code>MessageFactory</code> as an argument, 
 * delegates all requests to it and wraps the constructed instances of
 * <code>DHTMessage</code> into "Wire" Messages.
 */
public class MessageFactoryWire implements MessageFactory {
    
    private final MessageFactory delegate;

    public MessageFactoryWire(MessageFactory delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate is null");
        } else if (delegate instanceof MessageFactoryWire) {
            throw new IllegalArgumentException("Recursive delegation");
        }
        
        this.delegate = delegate;
    }
    
    public MessageID createMessageID(SocketAddress dst) {
        return delegate.createMessageID(dst);
    }

    public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
            throws MessageFormatException, IOException {
        
        DHTMessage msg = delegate.createMessage(src, data);
        
        if (msg instanceof PingRequest) {
            return new PingRequestWireImpl((PingRequest)msg);
        } else if (msg instanceof PingResponse) {
            return new PingResponseWireImpl((PingResponse)msg);
        } else if (msg instanceof FindNodeRequest) {
            return new FindNodeRequestWireImpl((FindNodeRequest)msg);
        } else if (msg instanceof FindNodeResponse) {
            return new FindNodeResponseWireImpl((FindNodeResponse)msg);
        } else if (msg instanceof FindValueRequest) {
            return new FindValueRequestWireImpl((FindValueRequest)msg);
        } else if (msg instanceof FindValueResponse) {
            return new FindValueResponseWireImpl((FindValueResponse)msg);
        } else if (msg instanceof StoreRequest) {
            return new StoreRequestWireImpl((StoreRequest)msg);
        } else if (msg instanceof StoreResponse) {
            return new StoreResponseWireImpl((StoreResponse)msg);
        } else if (msg instanceof StatsRequest) {
            return new StatsRequestWireImpl((StatsRequest)msg);
        } else if (msg instanceof StatsResponse) {
            return new StatsResponseWireImpl((StatsResponse)msg);
        }
        
        throw new IOException(msg.getClass() + " is unhandled");
    }
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) throws IOException {
        return delegate.writeMessage(dst, message);
    }
    
    public FindNodeRequest createFindNodeRequest(Contact src, SocketAddress dst, KUID lookupId) {
        return new FindNodeRequestWireImpl(
                delegate.createFindNodeRequest(src, dst, lookupId));
    }

    public FindNodeResponse createFindNodeResponse(Contact src, Contact dst, 
            MessageID messageId, Collection<? extends Contact> nodes) {
        return new FindNodeResponseWireImpl(
                delegate.createFindNodeResponse(src, dst, messageId, nodes));
    }

    public FindValueRequest createFindValueRequest(Contact src, SocketAddress dst, 
            KUID lookupId, Collection<KUID> keys, DHTValueType valueType) {
        return new FindValueRequestWireImpl(
                delegate.createFindValueRequest(src, dst, lookupId, keys, valueType));
    }

    public FindValueResponse createFindValueResponse(Contact src, Contact dst, 
            MessageID messageId, float requestLoad, Collection<? extends DHTValueEntity> entities, Collection<KUID> secondaryKeys) {
        return new FindValueResponseWireImpl(
                delegate.createFindValueResponse(src, dst, messageId, requestLoad, entities, secondaryKeys));
    }

    public PingRequest createPingRequest(Contact src, SocketAddress dst) {
        return new PingRequestWireImpl(
                delegate.createPingRequest(src, dst));
    }

    public PingResponse createPingResponse(Contact src, Contact dst, 
            MessageID messageId, SocketAddress externalAddress, BigInteger estimatedSize) {
        return new PingResponseWireImpl(
                delegate.createPingResponse(src, dst, messageId, externalAddress, estimatedSize));
    }

    public StatsRequest createStatsRequest(Contact src, SocketAddress dst, StatisticType stats) {
        return new StatsRequestWireImpl(
                delegate.createStatsRequest(src, dst, stats));
    }

    public StatsResponse createStatsResponse(Contact src, Contact dst, MessageID messageId, byte[] statistics) {
        return new StatsResponseWireImpl(
                delegate.createStatsResponse(src, dst, messageId, statistics));
    }

    public StoreRequest createStoreRequest(Contact src, SocketAddress dst, 
            SecurityToken securityToken, Collection<? extends DHTValueEntity> values) {
        return new StoreRequestWireImpl(
                delegate.createStoreRequest(src, dst, securityToken, values));
    }

    public StoreResponse createStoreResponse(Contact src, Contact dst, 
            MessageID messageId, Collection<StoreStatusCode> status) {
        return new StoreResponseWireImpl(
                delegate.createStoreResponse(src, dst, messageId, status));
    }
}
