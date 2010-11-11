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
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.RouteTableSettings;


/**
 * An abstract base class for request Messages
 */
abstract class AbstractRequestMessage extends AbstractDHTMessage
        implements RequestMessage {

    public AbstractRequestMessage(Context context, 
            OpCode opcode, Contact contact, MessageID messageId, Version msgVersion) {
        super(context, opcode, contact, messageId, msgVersion);
    }

    public AbstractRequestMessage(Context context, OpCode opcode, 
            SocketAddress src, MessageID messageId, Version msgVersion, MessageInputStream in) 
            throws IOException {
        super(context, opcode, src, messageId, msgVersion, in);
    }
    
    @Override
    protected final Contact createContact(SocketAddress src, Vendor vendor, Version version,
            KUID nodeId, SocketAddress contactAddress, int instanceId, int flags) {
        if (RouteTableSettings.INCOMING_REQUESTS_UNKNOWN.getValue())
            return ContactFactory.createUnknownContact(src, vendor, version, nodeId, contactAddress, instanceId, flags);
        else
            return ContactFactory.createLiveContact(src, vendor, version, nodeId, contactAddress, instanceId, flags);
    }
}
