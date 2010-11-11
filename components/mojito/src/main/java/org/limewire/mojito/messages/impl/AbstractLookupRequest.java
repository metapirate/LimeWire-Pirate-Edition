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

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An abstract class for LookupRequest implementations
 */
abstract class AbstractLookupRequest extends AbstractRequestMessage
        implements LookupRequest {

    protected final KUID lookupId;
    
    public AbstractLookupRequest(Context context, 
            OpCode opcode, Contact contact, 
            MessageID messageId, Version msgVersion, KUID lookupId) {
        super(context, opcode, contact, messageId, msgVersion);
        
        this.lookupId = lookupId;
    }
    
    public AbstractLookupRequest(Context context, 
            OpCode opcode, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        super(context, opcode, src, messageId, msgVersion, in);
        
        switch(opcode) {
            case FIND_NODE_REQUEST:
            case FIND_VALUE_REQUEST:
                lookupId = in.readKUID();
                break;
            default:
                throw new IOException("Unknown opcode for lookup request: " + opcode);
        }
    }

    public KUID getLookupID() {
        return lookupId;
    }

    @Override
    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeKUID(lookupId);
    }
}
