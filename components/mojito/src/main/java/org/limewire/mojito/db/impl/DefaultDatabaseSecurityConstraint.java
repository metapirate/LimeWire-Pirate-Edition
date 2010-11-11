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

package org.limewire.mojito.db.impl;

import java.util.Collection;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseSecurityConstraint;
import org.limewire.mojito.settings.DatabaseSettings;

/**
 * Determines when it is acceptable to store a {@link DHTValueEntity} in a {@link Database}.
 */
public class DefaultDatabaseSecurityConstraint implements DatabaseSecurityConstraint {
    
    private static final long serialVersionUID = 4513377023367562179L;

    public boolean allowStore(Database database, Map<KUID, DHTValueEntity> bag, DHTValueEntity entity) {
        
        // Allow as many local values as you want!
        if (entity.isLocalValue()) {
            return true;
        }
        
        // TODO allow as many signed values as you want?
        
        int maxDatabaseSize = DatabaseSettings.MAX_DATABASE_SIZE.getValue();
        int maxValuesPerKey = DatabaseSettings.MAX_VALUES_PER_KEY.getValue();
        
        // Limit the number of keys
        if (bag == null) {
            return (maxDatabaseSize < 0 || database.getKeyCount() < maxDatabaseSize);
        }
        
        // Limit the number of values per key
        DHTValueEntity existing = bag.get(entity.getSecondaryKey());
        if (existing == null) {
            // Allow to store if there's enough free space
            if (maxValuesPerKey < 0 || bag.size() < maxValuesPerKey) {
                return true;
            }
            
            // Prioritize values that were stored by non-firewalled Nodes
            if (entity.getCreator().isFirewalled()) {
                return false;
            }
            
            // Get the oldest firewalled value from the Bag, remove it and
            // allow the Database to store the new value
            DHTValueEntity firewalled = getOldestFirewalledValue(bag.values());
            if (firewalled != null) {
                database.remove(firewalled.getPrimaryKey(), 
                        firewalled.getSecondaryKey());
                return true;
            }
            
            return false;
        }
        
        return allowReplace(database, bag, existing, entity);
    }
    
    /**
     * Returns <code>true</code> if it's OK to replace the existing value with
     * the new value.
     */
    private boolean allowReplace(Database database, Map<KUID, DHTValueEntity> bag, 
            DHTValueEntity existing, DHTValueEntity entity) {
        
        // Non-local values cannot replace local values
        if (existing.isLocalValue() && !entity.isLocalValue()) {
            return false;
        }
        
        // Non-direct values cannot replace direct values
        if (existing.isDirect() && !entity.isDirect()) {
            return false;
        }
        
        // It's not possible to remove a value indirectly
        if (!entity.isDirect() && entity.getValue().size() == 0) {
            return false;
        }
        
        // TODO signed values cannot be replaced?
        
        return true;
    }
    
    private DHTValueEntity getOldestFirewalledValue(Collection<DHTValueEntity> entities) {
        DHTValueEntity oldest = null;
        
        for (DHTValueEntity entity : entities) {
            if (!entity.isLocalValue() 
                    && entity.getCreator().isFirewalled()) {
                if (oldest == null || entity.getCreationTime() < oldest.getCreationTime()) {
                    oldest = entity;
                }
            }
        }
        return oldest;
    }
}
