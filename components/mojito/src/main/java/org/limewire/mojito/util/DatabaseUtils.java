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

package org.limewire.mojito.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.KademliaSettings;


/**
 * Miscellaneous utilities for the Database.
 */
public class DatabaseUtils {
    
    private DatabaseUtils() {}
    
    /**
     * Returns the expiration time of the given DHTValue.
     */
    public static long getExpirationTime(RouteTable routeTable, DHTValueEntity entity) {
        KUID primaryKey = entity.getPrimaryKey();
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        Collection<Contact> nodes = routeTable.select(primaryKey, k, SelectMode.ALL);
        
        long creationTime = entity.getCreationTime();
        long expirationTime = DatabaseSettings.VALUE_EXPIRATION_TIME.getValue();
        
        // If there are less than k Nodes or the local Node is member
        // of the k-closest Nodes then use the default expiration time
        if (nodes.size() < k || nodes.contains(routeTable.getLocalNode())) {
            return creationTime + expirationTime;
            
        // The value expires inversely proportional otherwise by using
        // the xor distance
        } else {
            KUID valueBucketId = routeTable.getBucket(primaryKey).getBucketID();
            KUID localBucketId = routeTable.getBucket(routeTable.getLocalNode().getNodeID()).getBucketID();
            KUID xor = localBucketId.xor(valueBucketId);
            
            int lowestSetBit = xor.toBigInteger().getLowestSetBit();
            float ratio = 0.0f;
            if (lowestSetBit >= 0) {
                ratio = (float)(KUID.LENGTH_IN_BITS - lowestSetBit) / (float)KUID.LENGTH_IN_BITS;
            }
            
            return creationTime + (long)(expirationTime - (expirationTime * ratio));
        }
    }
    
    /**
     * Returns whether or not the given DHTValue has expired.
     */
    public static boolean isExpired(RouteTable routeTable, DHTValueEntity entity) {
        return System.currentTimeMillis() >= getExpirationTime(routeTable, entity);
    }
    
    public static boolean isPublishingRequired(Storable storable) {
        return isPublishingRequired(storable.getPublishTime(), storable.getLocationCount());
    }
    
    public static boolean isPublishingRequired(long publishingTime, int locationCount) {
        if (publishingTime <= 0L || locationCount <= 0) {
            return true;
        }
        
        long t = ((locationCount 
                * DatabaseSettings.VALUE_REPUBLISH_INTERVAL.getValue()) 
                    / KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        // Do never republish more than every X minutes
        long nextPublishTime = Math.max(t, 
                DatabaseSettings.MIN_VALUE_REPUBLISH_INTERVAL.getValue());
        
        long time = publishingTime + nextPublishTime;
        
        return System.currentTimeMillis() >= time;
    }

    public static boolean isDHTValueType(DHTValueType valueType, DHTValueEntity entity) {
        return valueType.equals(DHTValueType.ANY) 
                || valueType.equals(entity.getValue().getValueType());
    }

    public static Collection<? extends DHTValueEntity> filter(DHTValueType valueType, 
            Collection<? extends DHTValueEntity> entities) {
        
        if (valueType.equals(DHTValueType.ANY)) {
            return entities;
        }
        
        List<DHTValueEntity> filtered = new ArrayList<DHTValueEntity>(entities.size());
        for (DHTValueEntity entity : entities) {
            if (isDHTValueType(valueType, entity)) {
                filtered.add(entity);
            }
        }
        return filtered;
    }
    
    public static DHTValueEntity getFirstEntityFor(DHTValueType valueType, 
            Collection<? extends DHTValueEntity> entities) {
        for (DHTValueEntity entity : entities) {
            if (isDHTValueType(valueType, entity)) {
                return entity;
            }
        }
        return null;
    }
}
