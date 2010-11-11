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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;


/**
 * Handles incoming store requests as 
 * sent by other Nodes. It performs some probability tests to
 * make sure the request makes sense (i.e. if the Key is close
 * to us and so on).
 */
public class StoreRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreRequestHandler.class);
    
    public StoreRequestHandler(Context context) {
        super(context);
    }
    
    @Override
    public void request(RequestMessage message) throws IOException {
        
        StoreRequest request = (StoreRequest)message;
        
        SecurityToken securityToken = request.getSecurityToken();
        
        if (securityToken == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " does not provide a SecurityToken");
            }
            return;
        }
        
        Contact src = request.getContact();
        TokenData expectedToken = context.getSecurityTokenHelper().createTokenData(src);
        if (!securityToken.isFor(expectedToken)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " send us an invalid SecurityToken " + securityToken);
            }
            return;
        }
        
        Collection<? extends DHTValueEntity> values = request.getDHTValueEntities();
        
        List<StoreStatusCode> status 
            = new ArrayList<StoreStatusCode>(values.size());
        
        Database database = context.getDatabase();
        
        for (DHTValueEntity entity : values) {
            
            if (database.store(entity)) {
                status.add(new StoreStatusCode(entity, StoreResponse.OK));
            } else {
                status.add(new StoreStatusCode(entity, StoreResponse.ERROR));
            }
        }
        
        StoreResponse response 
            = context.getMessageHelper().createStoreResponse(request, status);
        context.getMessageDispatcher().send(request.getContact(), response);
    }
}
