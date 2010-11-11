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

import java.io.Serializable;
import java.util.Map;

import org.limewire.mojito.util.FixedSizeHashMap;

/**
 * A 16 bit <code>int</code> value and a String which may or may
 * not describe the <code>StatusCode</code>.
 */
public class StatusCode implements Serializable, Comparable<StatusCode> {
    
    private static final long serialVersionUID = 948952689527998250L;
    
    private static final Map<Integer, StatusCode> CODES 
        = new FixedSizeHashMap<Integer, StatusCode>(16, 0.75f, true, 32);
    
    private final int code;
    
    private final String description;
    
    private StatusCode(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Returns the actual status code.
     */
    public int shortValue() {
        return code;
    }
    
    /**
     * Returns the description of the StatusCode.
     */
    public String getDescription() {
        return description;
    }
    
    public int compareTo(StatusCode other) {
        return code - other.code;
    }
    
    @Override
    public int hashCode() {
        return code;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof StatusCode)) {
            return false;
        }
        
        return compareTo((StatusCode)o) == 0;
    }
    
    @Override
    public String toString() {
        return code + ": " + description;
    }
    
    /**
     * Returns a canonical version of status code.
     *
     * @param description must not be null
     */
    public static StatusCode valueOf(int code, String description) {
        if (description == null) {
            throw new NullPointerException("description must not be null, code: " + code);
        }
        Integer key = Integer.valueOf(code & 0xFFFF);
        StatusCode statusCode = null;
        synchronized (CODES) {
            statusCode = CODES.get(key);
            if (statusCode == null || !statusCode.description.equals(description)) {
                statusCode = new StatusCode(code, description);
                CODES.put(key, statusCode);
            }
        }
        return statusCode;
    }
}
