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
import java.util.Map;

import org.limewire.mojito.util.ArrayUtils;
import org.limewire.mojito.util.FixedSizeHashMap;

/**
 * Specifies the type of a DHT value. You can use the existing values (BINARY, 
 * LIME, or TEXT) and, or you can define your own type. You can use existing, 
 * your own and the "ANY" type when looking up nodes. However, ANY can not
 * be used an actual type (only for looking up nodes).
 */
public final class DHTValueType implements Comparable<DHTValueType>, Serializable {
    
    private static final long serialVersionUID = -3662336008253896020L;
    
    private static final String UNKNOWN_NAME = "UNKNOWN";
    
    private static final Map<Integer, DHTValueType> TYPES 
        = new FixedSizeHashMap<Integer, DHTValueType>(16, 0.75f, true, 254);
    
    /**
     * An arbitrary type of value.
     */
    public static final DHTValueType BINARY = DHTValueType.valueOf("Binary", 0x00000000);
    
    /**
     * LIME and all deviations of LIME like LiMe or lime are reserved
     * for Lime Wire LLC.
     */
    public static final DHTValueType LIME = DHTValueType.valueOf("LimeWire", "LIME");
    
    /**
     * Type for UTF-8 encoded Strings.
     */
    public static final DHTValueType TEXT = DHTValueType.valueOf("UTF-8 Encoded String", "TEXT");
    
    /**
     * A value that is used for testing purposes.
     */
    public static final DHTValueType TEST = DHTValueType.valueOf("Test Value", "TEST");
    
    /**
     * The ANY type is reserved for requesting purposes. You may not
     * use it as an actual value type.
     */
    public static final DHTValueType ANY = DHTValueType.valueOf("Any Type", "****");
    
    /** The Name of the value type. */
    private final String name;
    
    /** The type code of the value. */
    private final int type;
    
    private DHTValueType(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    
    public int toInt() {
        return type;
    }
    
    public int compareTo(DHTValueType o) {
        return type - o.type;
    }

    @Override
    public int hashCode() {
        return type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValueType)) {
            return false;
        }
        
        return compareTo((DHTValueType)o)==0;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (name.equals(UNKNOWN_NAME)) {
            buffer.append(ArrayUtils.toString(type)).append("/").append(name);
        } else {
            buffer.append(name);
        }
        buffer.append(" (0x").append(Long.toHexString((type & 0xFFFFFFFFL))).append(")");
        return buffer.toString();
    }
    
    public static DHTValueType[] values() {
        synchronized (TYPES) {
            return TYPES.values().toArray(new DHTValueType[0]);
        }
    }
    
    public static DHTValueType valueOf(int type) {
        return valueOf(UNKNOWN_NAME, type);
    }
    
    public static DHTValueType valueOf(String type) {
        return valueOf(UNKNOWN_NAME, type);
    }
    
    public static DHTValueType valueOf(String name, String type) {
        return valueOf(name, ArrayUtils.toInteger(type));
    }
    
    public static DHTValueType valueOf(String name, int type) {
        Integer key = Integer.valueOf(type);
        synchronized (TYPES) {
            DHTValueType valueType = TYPES.get(key);
            if (valueType == null 
                    || isBetterName(valueType, name)) {
                valueType = new DHTValueType(name, type);
                TYPES.put(key, valueType);
            }
            return valueType;
        }
    }
    
    /**
     * Check the cache and replace this instance with the cached instance
     * if one exists. The main goal is to pre-initialize the DHTValueType
     * Map.
     */
    private Object readResolve() {
        Integer key = Integer.valueOf(type);
        DHTValueType valueType = null;
        synchronized (TYPES) {
            valueType = TYPES.get(key);
            if (valueType == null || isBetterName(valueType, name)) {
                valueType = this;
                TYPES.put(key, valueType);
            }
        }
        return valueType;
    }
    
    /**
     * Returns true if the given name is a better than DHTValueType's
     * current name.
     */
    private static boolean isBetterName(DHTValueType valueType, String name) {
        return valueType.name.equals(UNKNOWN_NAME) && !name.equals(UNKNOWN_NAME);
    }
}
