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

package org.limewire.mojito.exceptions;

import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.RequestMessage;


/**
 * An abstract base class for Exceptions that are triggered
 * by requests.
 */
abstract class DHTRequestException extends DHTException {
    
    private final KUID nodeId;
    
    private final SocketAddress address;
    
    private final RequestMessage request;
    
    private final long time;
    
    public DHTRequestException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time, Throwable cause) {
        super(cause);
        
        this.nodeId = nodeId;
        this.address = address;
        this.request = request;
        this.time = time;
    }
    
    public DHTRequestException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time, String message) {
        super(message);
        
        this.nodeId = nodeId;
        this.address = address;
        this.request = request;
        this.time = time;
    }
    
    /**
     * Returns the ID of the Node (can be null)
     */
    public KUID getNodeID() {
        return nodeId;
    }
    
    /**
     * Returns the SocketAddress of the Node
     */
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    /**
     * Returns the RequestMessage
     */
    public RequestMessage getRequestMessage() {
        return request;
    }
    
    /**
     * Returns the time how long it took between sending
     * and throwing the exception (the cause could be a
     * timeout for example)
     */
    public long getTime() {
        return time;
    }
}
