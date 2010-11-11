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
 
package org.limewire.mojito.messages;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Collection;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.messages.StatsRequest.StatisticType;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.messages.impl.DefaultMessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;


/**
 * The MessageHelper class simplifies the construction of DHTMessages.
 * Except for some rare cases you want to use the MessageHelper instead of
 * the MessageFactory
 */
public class MessageHelper {

    protected final Context context;

    private MessageFactory factory;

    public MessageHelper(Context context) {
        this.context = context;
        factory = new DefaultMessageFactory(context);
    }

    public void setMessageFactory(MessageFactory factory) {
        if (factory == null) {
            factory = new DefaultMessageFactory(context);
        }
        this.factory = factory;
    }

    public MessageFactory getMessageFactory() {
        return factory;
    }
    
    protected Contact getLocalNode() {
        return context.getLocalNode();
    }
    
    protected BigInteger getEstimatedSize() {
        return context.size();
    }

    public PingRequest createPingRequest(SocketAddress dst) {
        return factory.createPingRequest(getLocalNode(), dst);
    }

    public PingResponse createPingResponse(RequestMessage request, SocketAddress externalAddress) {
        if (context.getContactAddress().equals(externalAddress)) {
            throw new IllegalArgumentException("Cannot tell other Node that its external address is the same as yours!");
        }
        
        return factory.createPingResponse(getLocalNode(), request.getContact(), 
                request.getMessageID(), externalAddress, getEstimatedSize());
    }

    public FindNodeRequest createFindNodeRequest(SocketAddress dst, KUID lookupId) {
        return factory.createFindNodeRequest(getLocalNode(), dst, lookupId);
    }

    public FindNodeResponse createFindNodeResponse(RequestMessage request, 
            Collection<? extends Contact> nodes) {
        return factory.createFindNodeResponse(getLocalNode(), request.getContact(), 
                request.getMessageID(), nodes);
    }

    public FindValueRequest createFindValueRequest(SocketAddress dst, KUID lookupId, 
            Collection<KUID> keys, DHTValueType valueType) {
        return factory.createFindValueRequest(getLocalNode(), dst, lookupId, keys, valueType);
    }

    public FindValueResponse createFindValueResponse(RequestMessage request, 
            float requestLoad, Collection<? extends DHTValueEntity> values, Collection<KUID> keys) {
        
        return factory.createFindValueResponse(getLocalNode(), request.getContact(), 
                request.getMessageID(), requestLoad, values, keys);
    }

    public StoreRequest createStoreRequest(SocketAddress dst, SecurityToken securityToken, 
            Collection<? extends DHTValueEntity> values) {
        
        return factory.createStoreRequest(getLocalNode(), dst, securityToken, values);
    }

    public StoreResponse createStoreResponse(RequestMessage request, 
            Collection<StoreStatusCode> status) {
        
        return factory.createStoreResponse(getLocalNode(), request.getContact(), 
                request.getMessageID(), status);
    }

    public StatsRequest createStatsRequest(SocketAddress dst, StatisticType request) {
        return factory.createStatsRequest(getLocalNode(), dst, request);
    }

    public StatsResponse createStatsResponse(RequestMessage request, byte[] statistics) {
        return factory.createStatsResponse(getLocalNode(), request.getContact(), 
                request.getMessageID(), statistics);
    }
}
