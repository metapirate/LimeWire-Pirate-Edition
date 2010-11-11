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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.ContactUtils;


/**
 * Handles incoming FIND_NODE requests.
 */
public class FindNodeRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(FindNodeRequestHandler.class);
    
    public FindNodeRequestHandler(Context context) {
        super(context);
    }

    /**
     * This method returns a list of nodes along with a SecurityToken generated for this node. 
     * The SecurityToken will can then be used by the querying node to store data at this node.
     * 
     * If the local node is passive (e.g. firewalled), it returns a list of Most Recently Seen 
     * nodes instead of returning the closest nodes to the lookup key. The reason for this is that 
     * passive nodes do not have accurate routing tables.
     * 
     * @param message the RequestMessage for this lookup
     * @throws IOException
     */
    @Override
    protected void request(RequestMessage message) throws IOException {
        // Cast to LookupRequest because FindValueRequestHandler
        // is delegating requests to this class!
        LookupRequest request = (LookupRequest)message;

        KUID lookupId = request.getLookupID();
        Contact node = request.getContact();
        
        Collection<Contact> nodes = Collections.emptyList();
        if (!context.isBootstrapping()) {
            if(context.isFirewalled()) {
                nodes = ContactUtils.sort(
                            context.getRouteTable().getContacts(), 
                            KademliaSettings.REPLICATION_PARAMETER.getValue());
                
                // If the external port is not set then make sure
                // we're not in the list!
                if (context.getExternalPort() == 0) {
                    nodes.remove(context.getLocalNode());
                }
                
            } else {
                nodes = context.getRouteTable().select(lookupId, 
                        KademliaSettings.REPLICATION_PARAMETER.getValue(), SelectMode.ALIVE);
            }
        }
        
        if (LOG.isTraceEnabled()) {
            if (!nodes.isEmpty()) {
                LOG.trace("Sending back: " + CollectionUtils.toString(nodes) + " to: " + node);
            } else {
                LOG.trace("Sending back an empty list to: " + node);
            }
        }
                
        FindNodeResponse response = context.getMessageHelper()
                    .createFindNodeResponse(request, nodes);
        
        context.getMessageDispatcher().send(node, response);
    }
}
