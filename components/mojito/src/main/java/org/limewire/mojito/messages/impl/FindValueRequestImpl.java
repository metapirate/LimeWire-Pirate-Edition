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
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An implementation of FindValueRequest.
 */
public class FindValueRequestImpl extends AbstractLookupRequest
        implements FindValueRequest {
    
    private final Collection<KUID> secondaryKeys;
    
    private final DHTValueType valueType;
    
    public FindValueRequestImpl(Context context, 
            Contact contact, MessageID messageId, 
            KUID lookupId, Collection<KUID> secondaryKeys, DHTValueType valueType) {
        super(context, OpCode.FIND_VALUE_REQUEST, 
                contact, messageId, Version.ZERO, lookupId);
        
        this.secondaryKeys = secondaryKeys;
        this.valueType = valueType;
    }
    
    public FindValueRequestImpl(Context context, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_VALUE_REQUEST, src, messageId, msgVersion, in);
        
        this.secondaryKeys = in.readKUIDs();
        this.valueType = in.readValueType();
    }
    
    public Collection<KUID> getSecondaryKeys() {
        return secondaryKeys;
    }
    
    public DHTValueType getDHTValueType() {
        return valueType;
    }
    
    @Override
    protected void writeBody(MessageOutputStream out) throws IOException {
        super.writeBody(out);
        out.writeKUIDs(getSecondaryKeys());
        out.writeDHTValueType(getDHTValueType());
    }

    @Override
    public String toString() {
        return "FindValueRequest: " + lookupId;
    }
}
