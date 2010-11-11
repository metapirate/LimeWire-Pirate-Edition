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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Grows up to a fixed predefined size and 
 * starts removing the eldest entry for each insertion. See 
 * also access-order and insertion-order mode of LinkedHashMap!
 */
public class FixedSizeHashMap<K, V> extends LinkedHashMap<K, V> implements Serializable {
    
    private static final long serialVersionUID = 8502617259787609782L;
    
    protected final int maxSize;

    public FixedSizeHashMap(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(int initialCapacity, float loadFactor, 
            boolean accessOrder, int maxSize) {
        super(initialCapacity, loadFactor, accessOrder);
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(int initialCapacity, float loadFactor, int maxSize) {
        super(initialCapacity, loadFactor);
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(int initialCapacity, int maxSize) {
        super(initialCapacity);
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(Map<? extends K, ? extends V> m, int maxSize) {
        super(m);
        this.maxSize = maxSize;
    }

    /**
     * Returns the max size of this Map.
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Returns whether or not this Map is full.
     */
    public boolean isFull() {
        return size() >= maxSize;
    }
    
    /*
     * Remove the eldest entry if the Map is full
     */
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
