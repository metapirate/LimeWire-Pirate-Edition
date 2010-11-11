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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.limewire.mojito.settings.KademliaSettings;

/**
 * Miscellaneous utilities for Collections.
 */
public final class CollectionUtils {
    
    private CollectionUtils() {}
    
    /**
     * Returns the given Collection as formatted String
     */
    public static String toString(Collection<?> c) {
        StringBuilder buffer = new StringBuilder();
        
        Iterator it = c.iterator();
        for(int i = 0; it.hasNext(); i++) {
            buffer.append(i).append(": ").append(it.next()).append('\n');
        }
        
        // Delete the last \n
        if(buffer.length() > 1) {
            buffer.setLength(buffer.length()-1);
        }
        return buffer.toString();
    }
    
    /**
     * Returns an iterator that returns up to max number of elements from the
     * Collection
     */
    private static <T> Iterator<T> iterator(final Collection<T> c, final int count) {
        return new Iterator<T>() {
            
            private final Iterator<T> it = c.iterator();
            
            private int item = 0;
            
            public boolean hasNext() {
                if (item >= count) {
                    return false;
                }
                return it.hasNext();
            }

            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                item++;
                return it.next();
            }

            public void remove() {
                it.remove();
                item--;
            }
        };
    }
    
    /**
     * Returns a sub-view of the given Collection that will only return 
     * the first K elements
     */
    public static <T> Collection<T> getCollection(Collection<T> c) {
        return getCollection(c, KademliaSettings.REPLICATION_PARAMETER.getValue());
    }
    
    /**
     * Returns a sub-view of the given Collection that will only return 
     * the first count elements
     */
    public static <T> Collection<T> getCollection(final Collection<T> c, final int count) {
        return new AbstractCollection<T>() {
            @Override
            public Iterator<T> iterator() {
                return CollectionUtils.iterator(c, count);
            }

            @Override
            public int size() {
                return Math.min(c.size(), count);
            }
        };
    }
}
