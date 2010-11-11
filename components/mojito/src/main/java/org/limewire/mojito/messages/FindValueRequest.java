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
 
package org.limewire.mojito.messages;

import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueType;


/**
 * Defines an interface for a <code>OpCode.FIND_VALUE_REQUEST</code>. 
 */
public interface FindValueRequest extends LookupRequest {
    
    /**
     * Returns a Collection of KUIDs the remote Node
     * is looking for.
     */
    public Collection<KUID> getSecondaryKeys();
    
    /**
     * Returns the type of the value the remote Node
     * is looking for.
     */
    public DHTValueType getDHTValueType();
}
