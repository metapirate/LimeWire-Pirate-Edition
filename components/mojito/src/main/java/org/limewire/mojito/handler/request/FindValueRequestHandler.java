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

package org.limewire.mojito.handler.request;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;


/**
 * Handles incoming FIND_VALUE requests.
 */
public class FindValueRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(FindValueRequestHandler.class);
    
    /**
     * Delegate to the FIND_NODE request handler
     */
    private final FindNodeRequestHandler findNodeDelegate;
    
    public FindValueRequestHandler(Context context, 
            FindNodeRequestHandler findNodeDelegate) {
        super(context);
        
        this.findNodeDelegate = findNodeDelegate;
    }
    
    @Override
    protected void request(RequestMessage message) throws IOException {
        FindValueRequest request = (FindValueRequest)message;
        
        KUID lookupId = request.getLookupID();
        DHTValueType valueType = request.getDHTValueType();
        
        Map<KUID, DHTValueEntity> bag = null;
        float requestLoad = 0f;
        
        Database database = context.getDatabase();
        synchronized (database) {
            bag = database.get(lookupId);
            requestLoad = database.getRequestLoad(lookupId, true);
        }
        
        // The keys and values we'll return
        Collection<KUID> availableKeys = Collections.emptySet();
        Collection<DHTValueEntity> valuesToReturn = Collections.emptySet();
        
        // The keys the remote Node is requesting
        Collection<KUID> requestedSecondaryKeys = request.getSecondaryKeys();
        
        if (bag != null && !bag.isEmpty()) {
            availableKeys = new HashSet<KUID>();
            valuesToReturn = new HashSet<DHTValueEntity>();
            
            Collection<? extends DHTValueEntity> filtered 
                = DatabaseUtils.filter(valueType, bag.values());
            
            if (requestedSecondaryKeys.isEmpty()) {
                if (valuesToReturn.isEmpty()
                        && filtered.size() == 1) {
                    valuesToReturn.addAll(filtered);
                } else {
                    for (DHTValueEntity entity : filtered) {
                        availableKeys.add(entity.getSecondaryKey());
                    }
                }
            } else {
                // Send all requested values back.
                // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
                for (DHTValueEntity entity : filtered) {
                    KUID secondaryKey = entity.getSecondaryKey();
                    if (requestedSecondaryKeys.contains(secondaryKey)) {
                        valuesToReturn.add(entity);
                    }
                }
            }
        }
        
        if (valuesToReturn.isEmpty() 
                && availableKeys.isEmpty()) {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("No values for " + lookupId 
                    + ", returning Contacts instead to " + request.getContact());
            }
            
            // OK, send Contacts instead!
            findNodeDelegate.handleRequest(message);
            
        } else {
            
            if (LOG.isInfoEnabled()) {
                LOG.info("Return " + CollectionUtils.toString(valuesToReturn) + " and " 
                        + CollectionUtils.toString(availableKeys) + " for " 
                        + lookupId + " to " + request.getContact());
            }
            
            FindValueResponse response = context.getMessageHelper()
                .createFindValueResponse(request, requestLoad, 
                        valuesToReturn, availableKeys);
            context.getMessageDispatcher().send(request.getContact(), response);
        }
    }
}
