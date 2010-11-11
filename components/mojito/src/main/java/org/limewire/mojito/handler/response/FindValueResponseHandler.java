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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.security.SecurityToken;

/**
 * Implements FIND_VALUE response specific features.
 */
public class FindValueResponseHandler extends LookupResponseHandler<FindValueResult> {
    
    private static final Log LOG = LogFactory.getLog(FindValueResponseHandler.class);

    /** Whether or not this is an exhaustive lookup. */
    private boolean exchaustive = false;
    
    /** The key we're looking for */
    private final EntityKey lookupKey;
    
    /** Collection of EntityKeys */
    private final Collection<EntityKey> entityKeys
        = new ArrayList<EntityKey>();

    /** Collection of DHTValueEntities */
    private final Collection<DHTValueEntity> entities 
        = new ArrayList<DHTValueEntity>();
    
    public FindValueResponseHandler(Context context, EntityKey lookupKey) {
        super(context, lookupKey.getPrimaryKey());
        this.lookupKey = lookupKey;
        
        setExhaustive(LookupSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        super.response(message, time);
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.timeout(nodeId, dst, message, time);
    }
    
    @Override
    protected boolean lookup(Contact node) throws IOException {
        if (super.lookup(node)) {
            return true;
        }
        return false;
    }
    
    /**
     * Sets whether or not this is an exhaustive lookup
     * (works only with FIND_VALUE lookups)
     */
    public void setExhaustive(boolean exchaustive) {
        this.exchaustive = exchaustive;
    }

    @Override
    protected void finishLookup() {
        long time = getElapsedTime();
        int currentHop = getCurrentHop();
        
        Map<Contact, SecurityToken> path = getPath();
        Collection<DHTValueEntity> entities = getDHTValueEntities();
        Collection<EntityKey> entityKeys = getEntityKeys();
        
        setReturnValue(new FindValueResult(
                lookupKey, path, entities, entityKeys, time, currentHop));
    }
    
    /**
     * Returns the type of value we're looking for
     */
    public EntityKey getLookupKey() {
        return lookupKey;
    }
    
    /**
     * Returns all DHTValueEntities that were found
     */
    public Collection<DHTValueEntity> getDHTValueEntities() {
        return entities;
    }
    
    /**
     * Returns all EntityKeys that were found
     */
    public Collection<EntityKey> getEntityKeys() {
        return entityKeys;
    }
    
    @Override
    protected boolean nextStep(ResponseMessage message) throws IOException {
        if (message instanceof FindNodeResponse) {
            return handleNodeResponse((FindNodeResponse)message);
        }
        
        if (!(message instanceof FindValueResponse)) {
            throw new IllegalArgumentException("this is a find value handler");
        }
        
        FindValueResponse response = (FindValueResponse)message;
        
        if (!extractDataFromResponse(response)) {
            return false;
        }
        
        addToResponsePath(response);
        
        // Terminate the FIND_VALUE lookup if it isn't
        // an exhaustive lookup
        if (!exchaustive) {
            killActiveSearches();
            return false;
        }
        
        // Continue otherwise...
        return true;
    }
    
    @Override
    protected int getDefaultParallelism() {
        return LookupSettings.FIND_VALUE_PARALLEL_LOOKUPS.getValue();
    }
    
    @Override
    protected boolean isTimeout(long time) {
        long lookupTimeout = LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
        return lookupTimeout > 0L && time >= lookupTimeout;
    }
    
    @Override
    protected LookupRequest createLookupRequest(Contact node) {
        Collection<KUID> noKeys = Collections.emptySet();
        return context.getMessageHelper().createFindValueRequest(
                node.getContactAddress(), lookupId, noKeys, lookupKey.getDHTValueType());
    }
    
    private boolean extractDataFromResponse(FindValueResponse response) {
        
        Contact sender = response.getContact();
        
        Collection<KUID> availableSecondaryKeys = response.getSecondaryKeys();
        Collection<? extends DHTValueEntity> entities = response.getDHTValueEntities();
        
        // No keys and no values? In other words the remote Node sent us
        // a FindValueResponse even though it doesn't have a value for
        // the given KUID!? Continue with the lookup if so...!
        if (availableSecondaryKeys.isEmpty() && entities.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(sender + " returned neither keys nor values for " + lookupId);
            }
            
            // Continue with the lookup...
            return false;
        }
        
        Collection<? extends DHTValueEntity> filtered 
            = DatabaseUtils.filter(lookupKey.getDHTValueType(), entities);
    
        // The filtered Set is empty and the unfiltered isn't?
        // The remote Node send us unrequested Value(s)!
        // Continue with the lookup if so...!
        if (filtered.isEmpty() && !entities.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(sender + " returned unrequested types of values for " + lookupId);
            }
            
            // Continue with the lookup...
            return false;
        }
        
        this.entities.addAll(filtered);
        
        for (KUID secondaryKey : availableSecondaryKeys) {
            EntityKey key = EntityKey.createEntityKey(
                    sender, lookupId, secondaryKey, lookupKey.getDHTValueType());
            
            this.entityKeys.add(key);
        }
        
        return true;
    }
}
