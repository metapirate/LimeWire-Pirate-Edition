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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.mojito.db.impl.DefaultDHTValueFactory;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

/**
 * Manages multiple instances of a {@link DHTValueFactory}.
 */
public class DHTValueFactoryManager {
    
    public static final DHTValueFactory defaultFactory = new DefaultDHTValueFactory();

    private final Map<DHTValueType, DHTValueFactory> factories 
        = Collections.synchronizedMap(new HashMap<DHTValueType, DHTValueFactory>());
    
    /**
     * Adds a new <code>DHTValueFactory</code>.
     */
    public DHTValueFactory addValueFactory(DHTValueType valueType, DHTValueFactory factory) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        if (factory == null) {
            throw new NullPointerException("DHTValueFactory is null");
        }
        
        return factories.put(valueType, factory);
    }
    
    /**
     * Removes a <code>DHTValueFactory</code> that is registered under the given 
     * <code>DHTValueType</code>.
     */
    public DHTValueFactory removeValueFactory(DHTValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        return factories.remove(valueType);
    }
    
    /**
     * Returns a <code>DHTValueFactory</code> for the given 
     * <code>DHTValueType</code> or the <code>defaultFactory</code> if none 
     * exists.
     */
    public DHTValueFactory getValueFactory(DHTValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        DHTValueFactory factory = factories.get(valueType);
        
        if (factory != null) {
            return factory;
        }
        
        return defaultFactory;
    }
    
    /**<p>
     * Creates a <code>DHTValue</code> from the given arguments. This
     * method takes care of empty values by creating a <code>DHTValue</code> 
     * via the default {@link DHTValueFactory}.
     * </p>
     * <code>createDHTValue</code> throws a {@link DHTValueException} for 
     * null <code>DHTValueType</code> or a null <code>Version</code>. 
     */
    public DHTValue createDHTValue(DHTValueType valueType, 
            Version version, byte[] value) throws DHTValueException {
        
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        if (version == null) {
            throw new NullPointerException("Version is null");
        }
        
        if (value == null || value.length == 0) {
            return defaultFactory.createDHTValue(valueType, version, value);
        }
        
        return getValueFactory(valueType).createDHTValue(valueType, version, value);
    }
}
