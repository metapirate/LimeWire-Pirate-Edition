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
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An implementation of FindNodeRequest.
 */
public class FindNodeRequestImpl extends AbstractLookupRequest
        implements FindNodeRequest {

    public FindNodeRequestImpl(Context context, 
            Contact contact, MessageID messageId, KUID lookupId) {
        super(context, OpCode.FIND_NODE_REQUEST, 
                contact, messageId, Version.ZERO, lookupId);
    }
    
    public FindNodeRequestImpl(Context context, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_NODE_REQUEST, src, messageId, msgVersion, in);
    }

    @Override
    public String toString() {
        return "FindNodeRequest: " + lookupId+ " from "+getContact();
    }
}
