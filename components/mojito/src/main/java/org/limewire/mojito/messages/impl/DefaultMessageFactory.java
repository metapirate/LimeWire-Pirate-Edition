/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.messages.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import org.limewire.io.ByteBufferInputStream;
import org.limewire.io.ByteBufferOutputStream;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.io.MessageInputStream;
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
import org.limewire.mojito.messages.DHTMessage.OpCode;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;
import org.limewire.security.SecurityToken;


/**
 * The default implementation of the MessageFactory.
 */
public class DefaultMessageFactory implements MessageFactory {

    protected final Context context;
    
    public DefaultMessageFactory(Context context) {
        this.context = context;
    }
    
    public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
            throws MessageFormatException, IOException {
        
        MessageInputStream in = null;
        
        try {
            in = new MessageInputStream(new ByteBufferInputStream(data), context.getMACCalculatorRepositoryManager());
            
            // --- GNUTELLA HEADER ---
            MessageID messageId = in.readMessageID();
            int func = in.readUnsignedByte();
            if (func != DHTMessage.F_DHT_MESSAGE) {
                throw new MessageFormatException("Unknown function ID: " + func);
            }
            
            Version msgVersion = in.readVersion();
            //byte[] length = in.readBytes(4); // Little-Endian!
            in.skip(4);
            
            // --- CONTINUTE WITH MOJITO HEADER ---
            OpCode opcode = in.readOpCode();
            
            switch(opcode) {
                case PING_REQUEST:
                    return new PingRequestImpl(context, src, messageId, msgVersion, in);
                case PING_RESPONSE:
                    return new PingResponseImpl(context, src, messageId, msgVersion, in);
                case FIND_NODE_REQUEST:
                    return new FindNodeRequestImpl(context, src, messageId, msgVersion, in);
                case FIND_NODE_RESPONSE:
                    return new FindNodeResponseImpl(context, src, messageId, msgVersion, in);
                case FIND_VALUE_REQUEST:
                    return new FindValueRequestImpl(context, src, messageId, msgVersion, in);
                case FIND_VALUE_RESPONSE:
                    return new FindValueResponseImpl(context, src, messageId, msgVersion, in);
                case STORE_REQUEST:
                    return new StoreRequestImpl(context, src, messageId, msgVersion, in);
                case STORE_RESPONSE:
                    return new StoreResponseImpl(context, src, messageId, msgVersion, in);
                case STATS_REQUEST:
                    return new StatsRequestImpl(context, src, messageId, msgVersion, in);
                case STATS_RESPONSE:
                    return new StatsResponseImpl(context, src, messageId, msgVersion, in);
                default:
                    throw new IOException("Unhandled OpCode " + opcode);
            }
        } catch (IllegalArgumentException err) {
            String msg = (src != null) ? src.toString() : null;
            throw new MessageFormatException(msg, err);
        } catch (IOException err) {
            String msg = (src != null) ? src.toString() : null;
            throw new MessageFormatException(msg, err);
        } finally {
            if (in != null) { 
                try { in.close(); } catch (IOException ignore) {}
            }
        }
    }
    
    public SecurityToken createSecurityToken(Contact dst) {
        return context.getSecurityTokenHelper().createSecurityToken(dst);
    }
    
    public MessageID createMessageID(SocketAddress dst) {
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            throw new IllegalArgumentException(dst + " is an invalid SocketAddress");
        }
        
        return DefaultMessageID.createWithSocketAddress(dst, context.getMACCalculatorRepositoryManager());
    }
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) 
            throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream(640, true);
        message.write(out);
        out.close();
        return ((ByteBuffer)out.getBuffer().flip()).order(ByteOrder.BIG_ENDIAN);
    }

    public FindNodeRequest createFindNodeRequest(Contact contact, SocketAddress dst, KUID lookupId) {
        return new FindNodeRequestImpl(context, contact, createMessageID(dst), lookupId);
    }

    public FindNodeResponse createFindNodeResponse(Contact contact, Contact dst, 
            MessageID messageId, Collection<? extends Contact> nodes) {
        return new FindNodeResponseImpl(context, contact, messageId, createSecurityToken(dst), nodes);
    }

    public FindValueRequest createFindValueRequest(Contact contact, SocketAddress dst, 
            KUID lookupId, Collection<KUID> keys, DHTValueType valueType) {
        return new FindValueRequestImpl(context, contact, createMessageID(dst), lookupId, keys, valueType);
    }

    public FindValueResponse createFindValueResponse(Contact contact, Contact dst, 
            MessageID messageId, float requestLoad, 
            Collection<? extends DHTValueEntity> entities, Collection<KUID> secondaryKeys) {
        return new FindValueResponseImpl(context, contact, messageId, requestLoad, entities, secondaryKeys);
    }

    public PingRequest createPingRequest(Contact contact, SocketAddress dst) {
        return new PingRequestImpl(context, contact, createMessageID(dst));
    }

    public PingResponse createPingResponse(Contact contact, Contact dst, 
            MessageID messageId, SocketAddress externalAddress, BigInteger estimatedSize) {
        return new PingResponseImpl(context, contact, messageId, externalAddress, estimatedSize);
    }

    public StatsRequest createStatsRequest(Contact contact, SocketAddress dst, StatisticType stats) {
        return new StatsRequestImpl(context, contact, createMessageID(dst), stats);
    }

    public StatsResponse createStatsResponse(Contact contact, Contact dst, 
            MessageID messageId, byte[] statistics) {
        return new StatsResponseImpl(context, contact, messageId, statistics);
    }

    public StoreRequest createStoreRequest(Contact contact, SocketAddress dst, 
            SecurityToken securityToken, Collection<? extends DHTValueEntity> values) {
        return new StoreRequestImpl(context, contact, createMessageID(dst), securityToken, values);
    }

    public StoreResponse createStoreResponse(Contact contact, Contact dst, 
            MessageID messageId, Collection<StoreStatusCode> status) {
        return new StoreResponseImpl(context, contact, messageId, status);
    }
}
