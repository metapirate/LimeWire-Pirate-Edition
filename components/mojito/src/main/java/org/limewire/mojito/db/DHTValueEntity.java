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

package org.limewire.mojito.db;

import java.io.Serializable;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;

/**
 * A <code>DHTValueEntity</code> is a row in a {@link Database}. 
 */
public class DHTValueEntity implements Serializable {
    
    private static final long serialVersionUID = 2007158043378144871L;

    /**
     * The creator of the value.
     */
    private final Contact creator;
    
    /**
     * The sender of the value (store forward).
     */
    private final Contact sender;
    
    /**
     * The (primary) key of the value.
     */
    private final KUID primaryKey;
    
    /**
     * The secondary key of the value.
     */
    private final KUID secondaryKey;
    
    /**
     * The actual value.
     */
    private final DHTValue value;
    
    /**
     * The time when this value was created (local time).
     */
    private final long creationTime = System.currentTimeMillis();
    
    /**
     * Flag for whether or not this is a local entity
     * (i.e. Sender and Creator are both the local Node).
     */
    private final boolean local;
    
    /**
     * The hash code of this entity.
     */
    private final int hashCode;
    
    /**
     * Creates and returns a <code>DHTValueEntity</code> from a <code>Storable</code>.
     */
    public static DHTValueEntity createFromStorable(MojitoDHT mojitoDHT, Storable storable) {
        return new DHTValueEntity(mojitoDHT.getLocalNode(), mojitoDHT.getLocalNode(), 
                storable.getPrimaryKey(), storable.getValue(), true);
    }

    /**
     * Creates and returns a <code>DHTValueEntity</code> for the given primary 
     * key and value.
     */
    public static DHTValueEntity createFromValue(MojitoDHT mojitoDHT, KUID primaryKey, DHTValue value) {
        return new DHTValueEntity(mojitoDHT.getLocalNode(), mojitoDHT.getLocalNode(), 
                primaryKey, value, true);
    }
    
    /**
     * Creates and returns a <code>DHTValueEntity</code> from arguments that were created.
     */
    public static DHTValueEntity createFromRemote(Contact creator, Contact sender, 
            KUID primaryKey, DHTValue value) {
        return new DHTValueEntity(creator, sender, primaryKey, value, false);
    }
    
    /**
     * Constructor to create DHTValueEntities. It's package private
     * for testing purposes. Use the factory methods!
     */
    DHTValueEntity(Contact creator, Contact sender, 
            KUID primaryKey, DHTValue value, boolean local) {
        this.creator = creator;
        this.sender = sender;
        this.primaryKey = primaryKey;
        this.secondaryKey = creator.getNodeID();
        this.value = value;
        this.local = local;
        
        this.hashCode = 17*primaryKey.hashCode() + secondaryKey.hashCode();
    }
    
    /**
     * Returns the creator of this value.
     */
    public Contact getCreator() {
        return creator;
    }
    
    /**
     * Returns the sender of this value.
     */
    public Contact getSender() {
        return sender;
    }
    
    /**
     * Returns the primary key of this value.
     */
    public KUID getPrimaryKey() {
        return primaryKey;
    }
    
    /**
     * Returns the secondary key of this value.
     */
    public KUID getSecondaryKey() {
        return secondaryKey;
    }
    
    /**
     * Returns the value.
     */
    public DHTValue getValue() {
        return value;
    }
   
    /**
     * Returns the creation time.
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /*public void handleStoreResult(StoreResult result) {
        // DO NOTHING
    }*/
    
    /**
     * Returns <code>true</code> if this entity was sent by
     * the creator of the value. In other words
     * if the creator and sender are equal.
     */
    public boolean isDirect() {
        return creator.equals(sender);
    }
    
    public boolean isLocalValue() {
        return local;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValueEntity)) {
            return false;
        }
        
        DHTValueEntity other = (DHTValueEntity)o;
        return primaryKey.equals(other.getPrimaryKey())
                    && secondaryKey.equals(other.getSecondaryKey());
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Creator: ").append(getCreator()).append("\n");
        buffer.append("Sender: ").append(getSender()).append("\n");
        buffer.append("Primary Key: ").append(getPrimaryKey()).append("\n");
        buffer.append("Secondary Key: ").append(getSecondaryKey()).append("\n");
        buffer.append("Creation time: ").append(getCreationTime()).append("\n");
        buffer.append("---\n").append(getValue()).append("\n");
        return buffer.toString();
    }
}
