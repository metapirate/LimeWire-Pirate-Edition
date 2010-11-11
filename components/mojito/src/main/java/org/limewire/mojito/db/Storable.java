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

import org.limewire.mojito.KUID;
import org.limewire.mojito.result.StoreResult;

/**
 * A key-value pair ({@link KUID}, {@link DHTValue}) that can be stored in the DHT.
 */
public class Storable {
    
    private final KUID primaryKey;
    
    private final DHTValue value;
    
    private long publishTime;
    
    private int locationCount;
    
    public Storable(KUID primaryKey, DHTValue value) {
        this.primaryKey = primaryKey;
        this.value = value;
    }
    
    /**
     * Returns the primary key of the <code>Storable</code> value.
     */
    public KUID getPrimaryKey() {
        return primaryKey;
    }
    
    /**
     * Returns the value.
     */
    public DHTValue getValue() {
        return value;
    }
    
    /**
     * Returns the time when this value was stored.
     */
    public synchronized long getPublishTime() {
        return publishTime;
    }
    
    /**
     * Sets the time when this value was stored.
     */
    public synchronized void setPublishTime(long publishTime) {
        if (publishTime < 0L) {
            throw new IllegalArgumentException("PublishTime is negative: " + publishTime);
        }
        
        this.publishTime = publishTime;
    }
    
    /**
     * Returns the location count.
     */
    public synchronized int getLocationCount() {
        return locationCount;
    }
    
    /**
     * Sets the location count.
     */
    public synchronized void setLocationCount(int locationCount) {
        if (locationCount < 0) {
            throw new IllegalArgumentException("LocationCount is negative: " + locationCount);
        }
        
        this.locationCount = locationCount;
    }
    
    /**
     * Called by <code>StorablePublisher</code>.
     */
    protected synchronized void handleStoreResult(StoreResult result) {
        setPublishTime(System.currentTimeMillis());
        setLocationCount(result.getLocations().size());
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        synchronized (this) {
            builder.append("PrimaryKey=").append(getPrimaryKey()).append("\n");
            builder.append("Value=").append(getValue()).append("\n");
            builder.append("PublishTime=").append(getPublishTime()).append("\n");
            builder.append("LocationCount=").append(getLocationCount()).append("\n");
        }
        return builder.toString();
    }
}
