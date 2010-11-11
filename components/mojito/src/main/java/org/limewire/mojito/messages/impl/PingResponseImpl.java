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
import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An implementation of PingResponse (Pong).
 */
public class PingResponseImpl extends AbstractResponseMessage
        implements PingResponse {

    private final SocketAddress externalAddress;
    
    private final BigInteger estimatedSize;

    public PingResponseImpl(Context context, 
	    Contact contact, MessageID messageId, 
	    SocketAddress externalAddress, BigInteger estimatedSize) {
        super(context, OpCode.PING_RESPONSE, contact, messageId, Version.ZERO);

        this.externalAddress = externalAddress;
        this.estimatedSize = estimatedSize;
    }

    public PingResponseImpl(Context context, SocketAddress src, 
            MessageID messageId, Version msgVersion, MessageInputStream in) throws IOException {
        super(context, OpCode.PING_RESPONSE, src, messageId, msgVersion, in);
        
        this.externalAddress = in.readSocketAddress();
        this.estimatedSize = in.readDHTSize();
    }
    
    /** My external address */
    public SocketAddress getExternalAddress() {
        return externalAddress;
    }

    public BigInteger getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeSocketAddress(externalAddress);
        out.writeDHTSize(estimatedSize);
    }

    @Override
    public String toString() {
        return "PingResponse: externalAddress=" + externalAddress + ", estimatedSize=" + estimatedSize;
    }
}
