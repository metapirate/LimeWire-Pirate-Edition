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
import java.util.Map;
import java.util.Set;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.security.SecurityToken;

/**
 * Implements FIND_NODE response specific features.
 */
public class FindNodeResponseHandler 
        extends LookupResponseHandler<FindNodeResult> {
    
    public FindNodeResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId);
    }
    
    public FindNodeResponseHandler(Context context, Contact forcedContact, KUID lookupId) {
        super(context, lookupId);
        addForcedContact(forcedContact);
    }
    
    public FindNodeResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        super(context, lookupId);
        setResultSetSize(resultSetSize);
    }
    
    public FindNodeResponseHandler(Context context, Contact forcedContact, 
            KUID lookupId, int resultSetSize) {
        super(context, lookupId);
        addForcedContact(forcedContact);
        setResultSetSize(resultSetSize);
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
    
    @Override
    protected void finishLookup() {
        long time = getElapsedTime();
        int routeTableFailureCount = getRouteTableFailureCount();
        int currentHop = getCurrentHop();
        
        Map<Contact, SecurityToken> path = getPath();
        Collection<Contact> collisions = getCollisions();
        Set<KUID> queried = getQueried();
        
        FindNodeResult result = new FindNodeResult(getLookupID(), path, 
                collisions, queried, time, currentHop, routeTableFailureCount);
        
        // We can use the result from a Node lookup to estimate the DHT size
        context.updateEstimatedSize(path.keySet());
        
        setReturnValue(result);
    }
    
    /**
     * Returns a Collection of Contacts that did collide with the
     * local Node ID.
     */
    public Collection<Contact> getCollisions() {
        return collisions;
    }
    
    @Override
    protected int getDefaultParallelism() {
        return LookupSettings.FIND_NODE_PARALLEL_LOOKUPS.getValue();
    }
    
    @Override
    protected boolean isTimeout(long time) {
        long lookupTimeout = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
        return lookupTimeout > 0L && time >= lookupTimeout;
    }
    
    @Override
    protected LookupRequest createLookupRequest(Contact node) {
        return context.getMessageHelper().createFindNodeRequest(
                node.getContactAddress(), lookupId);
    }

    @Override
    protected boolean nextStep(ResponseMessage message) throws IOException {
        if (!(message instanceof FindNodeResponse))
            throw new IllegalArgumentException("this is find node handler");
        return handleNodeResponse((FindNodeResponse)message);
    }
}
