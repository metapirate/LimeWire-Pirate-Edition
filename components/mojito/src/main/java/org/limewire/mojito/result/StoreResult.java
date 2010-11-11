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

package org.limewire.mojito.result;

import java.util.Collection;
import java.util.Map;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.CollectionUtils;


/**
 * The StoreResult is fired when a STORE request has finished.
 */
public class StoreResult implements Result {
    
    private final Map<Contact, Collection<StoreStatusCode>> locations;
    
    private final Collection<? extends DHTValueEntity> values;

    public StoreResult(Map<Contact, Collection<StoreStatusCode>> locations, 
            Collection<? extends DHTValueEntity> values) {
        
        this.locations = locations;
        this.values = values;
    }
    
    /**
     * Returns a Collection Nodes where the DHTValue(s) were
     * stored.
     */
    public Collection<? extends Contact> getLocations() {
        return locations.keySet();
    }
    
    /**
     * Returns a Collection of DHTValue(s) that were stored.
     */
    public Collection<? extends DHTValueEntity> getValues() {
        return values;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("VALUES").append("\n");
        buffer.append(CollectionUtils.toString(getValues()));
        
        buffer.append("LOCATIONS:").append("\n");
        buffer.append(CollectionUtils.toString(getLocations()));
        return buffer.toString();
    }
}
