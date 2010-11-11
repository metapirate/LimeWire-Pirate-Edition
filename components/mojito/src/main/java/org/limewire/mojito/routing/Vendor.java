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

package org.limewire.mojito.routing;

import java.io.Serializable;

import org.limewire.mojito.util.ArrayUtils;

/**
 * Vendor is a four byte value in big-endian byte order where
 * each byte is chosen from the human readable ASCII character 
 * space.
 */
public class Vendor implements Serializable, Comparable<Vendor> {
    
    private static final long serialVersionUID = 1607453128714814318L;
    
    /** 
     * An array of cached Vendors. Make it bigger if necessary.
     */
    private static final Vendor[] VENDORS = new Vendor[10];
    
    public static final Vendor UNKNOWN = new Vendor(0);
    
    public static final int LENGTH = 4;
    
    private final int vendorId;
    
    private Vendor(int vendorId) {
        this.vendorId = vendorId;
    }
    
    /**
     * Returns the vendor ID as an integer.
     */
    public int intValue() {
        return vendorId;
    }
    
    @Override
    public int hashCode() {
        return vendorId;
    }
    
    public int compareTo(Vendor o) {
        return vendorId - o.vendorId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Vendor)) {
            return false;
        }
        
        return vendorId == ((Vendor)o).vendorId;
    }
    
    @Override
    public String toString() {
        return ArrayUtils.toString(vendorId);
    }
    
    /**
     * Returns a Vendor object for the given vendor ID.
     */
    public static synchronized Vendor valueOf(int vendorId) {
        int index = (vendorId & Integer.MAX_VALUE) % VENDORS.length;
        Vendor vendor = VENDORS[index];
        if (vendor == null || vendor.vendorId != vendorId) {
            vendor = new Vendor(vendorId);
            VENDORS[index] = vendor;
        }
        return vendor;
    }
    
    /**
     * Returns a Vendor object for the given vendor ID.
     */
    public static Vendor valueOf(String vendorId) {
        return valueOf(ArrayUtils.toInteger(vendorId));
    }
    
    /**
     * Check the cache and replace this instance with the cached instance
     * if one exists. The main goal is to pre-initialize the VENDORS
     * array.
     */
    private Object readResolve() {
        synchronized (getClass()) {
            int index = (vendorId & Integer.MAX_VALUE) % VENDORS.length;
            Vendor vendor = VENDORS[index];
            if (vendor == null || vendor.vendorId != vendorId) {
                vendor = this;
                VENDORS[index] = vendor;
            }
            return vendor;
        }
    }
}
