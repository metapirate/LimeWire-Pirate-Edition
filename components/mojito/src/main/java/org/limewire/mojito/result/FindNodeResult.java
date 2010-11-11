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
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;


/**
 * The FindNodeResult is fired when a FIND_NODE lookup
 * finishes.
 */
public class FindNodeResult extends LookupResult {
    
    private final Map<? extends Contact, ? extends SecurityToken> path;
    
    private final Collection<? extends Contact> collisions;
    
    private final Set<KUID> queried;
    
    private final long time;
    
    private final int hop;
    
    private final int routeTableFailureCount;
    
    public FindNodeResult(KUID lookupId, 
            Map<? extends Contact, ? extends SecurityToken> path, 
            Collection<? extends Contact> collisions,
            Set<KUID> queried,
            long time, int hop, int routeTableFailureCount) {
    	super(lookupId);
        this.path = path;
        this.collisions = new CopyOnWriteArrayList<Contact>(collisions);
        this.queried = new CopyOnWriteArraySet<KUID>(queried);
        this.time = time;
        this.hop = hop;
        this.routeTableFailureCount = routeTableFailureCount;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getPath()
     */
    public Collection<? extends Contact> getPath() {
        return path.keySet();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getSecurityToken(org.limewire.mojito.routing.Contact)
     */
    public SecurityToken getSecurityToken(Contact node) {
        return path.get(node);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getEntryPath()
     */
    public Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> getEntryPath() {
        return path.entrySet();
    }
    
    /**
     * Returns the number of Nodes that were in our RouteTable
     * and that failed to respond.
     */
    public int getRouteTableFailureCount() {
        return routeTableFailureCount;
    }
    
    /**
     * Returns a Collection of Contacts that collide with our local Node ID.
     */
    public Collection<? extends Contact> getCollisions() {
        return collisions;
    }
    
    /**
     * Returns all KUIDs that were queried during the lookup.
     */
    public Set<KUID> getQueried() {
        return queried;
    }
    
    /**
     * Returns the amount of time it took to find the
     * k-closest Nodes.
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Returns the hop at which the lookup terminated.
     */
    public int getHop() {
        return hop;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getLookupID()).append(" (time=").append(time).append("ms, hop=").append(hop).append(")\n");
        int i = 0;
        for (Entry<? extends Contact, ? extends SecurityToken> entry : path.entrySet()) {
            buffer.append(i++).append(": ").append(entry.getKey())
                .append(", token=").append(entry.getValue()).append("\n");
        }
        
        if (!collisions.isEmpty()) {
            buffer.append("Collisions:\n");
            i = 0;
            for (Contact node : collisions) {
                buffer.append(i++).append(": ").append(node).append("\n");
            }
        }
        
        return buffer.toString();
    }
}
