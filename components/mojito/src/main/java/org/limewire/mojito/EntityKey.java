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

package org.limewire.mojito;

import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;

/**
 * An EntityKey specifies the exact location of a DHTValueEntity.
 */
public class EntityKey {
    
    /**
     * The Contact that has the Entity.
     */
    private final Contact node;
    
    /**
     * The primary key of the entity.
     */
    private final KUID primaryKey;
    
    /**
     * The secondary key of the entity.
     */
    private final KUID secondaryKey;
    
    /**
     * The type of the entity.
     */
    private final DHTValueType valueType;
    
    private final int hashCode;
    
    /**
     * Creates and returns an EntityKey that will do a full lookup
     * for the value.
     */
    public static EntityKey createEntityKey(KUID primaryKey, DHTValueType valueType) {
        
        if (primaryKey == null) {
            throw new NullPointerException("PrimaryKey is null");
        }
        
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        return new EntityKey(null, primaryKey, null, valueType);
    }
    
    /**
     * Creates and returns a new EntityKey with a known source
     * which will look only at the given Node for the value.
     */
    public static EntityKey createEntityKey(Contact node, KUID primaryKey, 
            KUID secondaryKey, DHTValueType valueType) {
        
        if (node == null) {
            throw new NullPointerException("Contact is null");
        }
        
        if (primaryKey == null) {
            throw new NullPointerException("PrimaryKey is null");
        }
        
        if (secondaryKey == null) {
            throw new NullPointerException("SecondaryKey is null");
        }
        
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        return new EntityKey(node, primaryKey, secondaryKey, valueType);
    }
    
    private EntityKey(Contact node, KUID primaryKey, 
            KUID secondaryKey, DHTValueType valueType) {
        
        this.node = node;
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.valueType = valueType;
        
        int hashCode = primaryKey.hashCode() 
                     ^ valueType.hashCode();
        
        if (secondaryKey != null) {
            hashCode ^= secondaryKey.hashCode();
        }
        
        this.hashCode = hashCode;
    }
    
    /**
     * Returns true if this is a lookup key.
     */
    public boolean isLookupKey() {
        return secondaryKey == null;
    }
    
    /**
     * Returns the node that has the DHTValueEntity.
     */
    public Contact getContact() {
        return node;
    }
    
    /**
     * Returns the primary key of the DHTValueEntity.
     */
    public KUID getPrimaryKey() {
        return primaryKey;
    }
    
    /**
     * Returns the secondary key of the DHTValueEntity.
     */
    public KUID getSecondaryKey() {
        return secondaryKey;
    }
    
    /**
     * Returns the type of the DHTValueEntity.
     */
    public DHTValueType getDHTValueType() {
        return valueType;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof EntityKey)) {
            return false;
        }
        
        EntityKey other = (EntityKey)o;
        
        if (isLookupKey() != other.isLookupKey()) {
            return false;
        }
        
        if (isLookupKey()) {
            return primaryKey.equals(other.primaryKey)
                && valueType.equals(other.valueType);
        } else {
            return primaryKey.equals(other.primaryKey)
                && secondaryKey.equals(other.secondaryKey)
                && valueType.equals(other.valueType);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Contact: ").append(getContact()).append("\n");
        buffer.append("PrimaryKey: ").append(getPrimaryKey()).append("\n");
        buffer.append("SecondaryKey: ").append(getSecondaryKey()).append("\n");
        buffer.append("DHTValueType: ").append(getDHTValueType()).append("\n");
        buffer.append("IsLookupKey: ").append(isLookupKey()).append("\n");
        return buffer.toString();
    }
}
