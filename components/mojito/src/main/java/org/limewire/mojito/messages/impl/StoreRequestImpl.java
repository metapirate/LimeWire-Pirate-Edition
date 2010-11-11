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
import java.net.SocketAddress;
import java.util.Collection;

import org.limewire.mojito.Context;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;
import org.limewire.security.SecurityToken;


/**
 * An implementation of StoreRequest.
 */
public class StoreRequestImpl extends AbstractRequestMessage implements StoreRequest {

    private final SecurityToken securityToken;
    
    private final Collection<? extends DHTValueEntity> values;
    
    public StoreRequestImpl(Context context, 
            Contact contact, MessageID messageId,
            SecurityToken securityToken, Collection<? extends DHTValueEntity> values) {
        super(context, OpCode.STORE_REQUEST, contact, messageId, Version.ZERO);

        this.securityToken = securityToken;
        this.values = values;
    }
    
    public StoreRequestImpl(Context context, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        super(context, OpCode.STORE_REQUEST, src, messageId, msgVersion, in);
        
        this.securityToken = in.readSecurityToken();
        this.values = in.readDHTValueEntities(getContact(), 
                context.getDHTValueFactoryManager());
    }
    
    public SecurityToken getSecurityToken() {
        return securityToken;
    }

    public Collection<? extends DHTValueEntity> getDHTValueEntities() {
        return values;
    }

    @Override
    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeSecurityToken(securityToken);
        out.writeDHTValueEntities(values);
    }

    @Override
    public String toString() {
        return "StoreRequest: " + values;
    }
}
