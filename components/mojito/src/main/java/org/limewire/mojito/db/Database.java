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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.limewire.mojito.KUID;


/**
 * Defines an interface to implement databases. 
 * <p>
 * Mojito ships with an in-memory database but you may use this
 * interface to implement a custom <code>Database</code> that is
 * built on top of BDBJE, for example.
 */
public interface Database extends Serializable {
    
    /**
     * Sets the <code>DatabaseSecurityConstraint</code>. Use null to reset 
     * the default constraint.
     * 
     * @param securityConstraint the security constraint 
     */
    public void setDatabaseSecurityConstraint(
            DatabaseSecurityConstraint securityConstraint);
    
    /**
     * Adds or removes the given <code>DHTValue</code> depending on
     * whether or not it's empty. 
     * 
     * @param entity DHTValue to store (add or remove)
     * @return whether or not the given DHTValue was added or removed
     */
    public boolean store(DHTValueEntity entity);
    
    /**
     * Adds the given DHTValueEntity to the Database
     */
    //public boolean add(DHTValueEntity entity);
    
    /**
     * Removes the given <code>DHTValue</code> from the Database.
     * 
     * @param primaryKey key of key-value to remove
     * @param secondaryKey value of key-value to remove
     * @return previous value associated with specified key, or null 
     * if there was no mapping for the <code>secondaryKey</code>.
     */
    public DHTValueEntity remove(KUID primaryKey, KUID secondaryKey);
    
    /**
     * Returns whether or not the given <code>DHTValue</code> is stored in our
     * Database.
     */
    public boolean contains(KUID primaryKey, KUID secondaryKey);
    
    /**
     * Returns a <code>DHTValueBag</code> for the given ValueID, or null if no bag exists.
     * 
     * @param primaryKey the <code>KUID</code> of the value to lookup in the database
     */
    public Map<KUID, DHTValueEntity> get(KUID primaryKey);
    
    /**
     * Returns the load factor for a <code>KUID</code> Will increment the load value
     * before returning if <code>incrementLoad</code> is set to <code>true</code>.
     * 
     * @param primaryKey key of the Node to get the load value
     * @param incrementLoad whether or not to increase the exponential moving average 
     * (EMA, http://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average)
     * 
     */
    public float getRequestLoad(KUID primaryKey, boolean incrementLoad);
    
    /**
     * Returns all <code>Key</code>s.
     */
    public Set<KUID> keySet();
    
    /**
     * Returns a collection of <code>DHTValueEntity</code>.
     */
    public Collection<DHTValueEntity> values();
    
    /**
     * Returns the number of <code>Key</code>s in the Database.
     */
    public int getKeyCount();
    
    /**
     * Returns the number of Values in the Database
     * which is greater or equal to key count.
     */
    public int getValueCount();
    
    /**
     * Clears the Database.
     */
    public void clear();
}
