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

package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.exceptions.DHTNoSuchElementException;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.security.SecurityToken;


/**
 * The GetValueResponseHandler retrieves DHTValues from a remote Node.
 */
public class GetValueResponseHandler extends AbstractResponseHandler<FindValueResult> {
        
    private static final Log LOG = LogFactory.getLog(GetValueResponseHandler.class);
    
    private final EntityKey lookupKey;
    
    public GetValueResponseHandler(Context context, 
            EntityKey lookupKey) {
        super(context);
        
        this.lookupKey = lookupKey;
    }
    
    @Override
    protected void start() throws DHTException {
        
        Contact node = lookupKey.getContact();
        KUID primaryKey = lookupKey.getPrimaryKey();
        KUID secondaryKey = lookupKey.getSecondaryKey();
        DHTValueType valueType = lookupKey.getDHTValueType();
        
        FindValueRequest request = context.getMessageHelper()
            .createFindValueRequest(node.getContactAddress(), 
                    primaryKey, Collections.singleton(secondaryKey), valueType);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("start looking for: " + request);
        }
        
        try {
            context.getMessageDispatcher().send(node, request, this);
        } catch (IOException err) {
            throw new DHTException(err);
        }
    }

    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        if (message instanceof FindValueResponse) {
            FindValueResponse response = (FindValueResponse)message;
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("received response: " + response);
            }
            
            // Make sure the DHTValueEntities have the expected
            // value type.
            Collection<? extends DHTValueEntity> entities 
                = DatabaseUtils.filter(lookupKey.getDHTValueType(), 
                        response.getDHTValueEntities());
            
            Map<? extends Contact, ? extends SecurityToken> path = Collections.emptyMap();
            Collection<EntityKey> entityKeys = Collections.emptySet();
            
            FindValueResult result = new FindValueResult(lookupKey, 
                    path, entities, entityKeys, time, 1);
            
            setReturnValue(result);
            
        // Imagine the following case: We do a lookup for a value 
        // on the 59th minute and start retrieving the values on 
        // the 60th minute. As values expire on the 60th minute 
        // it may no longer exists and the remote Node returns us
        // a Set of the k-closest Nodes instead.
        } else {
            setException(new DHTNoSuchElementException(message, lookupKey.toString()));
        }
    }
    
    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
        setException(new DHTBackendException(nodeId, dst, message, e));
    }
}
