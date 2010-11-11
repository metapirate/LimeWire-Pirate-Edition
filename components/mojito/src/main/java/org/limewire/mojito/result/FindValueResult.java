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
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.security.SecurityToken;


/**
 * The FindValueResult is fired when a FIND_VALUE lookup finishes.
 */
public class FindValueResult extends LookupResult {
    
    private final EntityKey lookupKey;
    
    private final Map<? extends Contact, ? extends SecurityToken> path;
    
    private final long time;
    
    private final int hop;
    
    private final Collection<? extends DHTValueEntity> entities;
    
    private final Collection<? extends EntityKey> entityKeys;
    
    public FindValueResult(EntityKey lookupKey,
            Map<? extends Contact, ? extends SecurityToken> path,
            Collection<? extends DHTValueEntity> entities,
            Collection<? extends EntityKey> entityKeys,
            long time, int hop) {
        super(lookupKey.getPrimaryKey());
        
        this.lookupKey = lookupKey;
        this.path = path;
        this.time = time;
        this.hop = hop;
        this.entities = new CopyOnWriteArrayList<DHTValueEntity>(entities);
        this.entityKeys = new CopyOnWriteArrayList<EntityKey>(entityKeys);
    }
    
    /**
     * Returns the lookup key that was used to get this value.
     */
    public EntityKey getLookupKey() {
        return lookupKey;
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
     * Returns the values that were found.
     */
    public Collection<? extends DHTValueEntity> getEntities() {
        return entities;
    }
    
    /**
     * Returns the EntityKeys that were found.
     */
    public Collection<? extends EntityKey> getEntityKeys() {
        return entityKeys;
    }

    /**
     * Returns the amount of time it took to find the DHTValue(s).
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Returns the number of hops it took to find the DHTValue(s).
     */
    public int getHop() {
        return hop;
    }
    
    /**
     * Returns true if the lookup was successful.
     */
    public boolean isSuccess() {
        return !entities.isEmpty() || !entityKeys.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getLookupID()).append(" (time=").append(time)
            .append("ms, hop=").append(hop).append(")\n");

        if(!isSuccess()) {
            buffer.append("No values found!");
            return buffer.toString();
        }
        
        buffer.append(CollectionUtils.toString(entities));
        buffer.append(CollectionUtils.toString(entityKeys));
        return buffer.toString();
    }
}
