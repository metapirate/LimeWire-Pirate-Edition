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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.result.StoreResult;

/**
 * Manages multiple instances of a <code>StorableModel</code>.
 */
public class StorableModelManager {
    
    private static final Log LOG = LogFactory.getLog(StorableModelManager.class);
    
    private final Map<DHTValueType, StorableModel> models 
        = Collections.synchronizedMap(new HashMap<DHTValueType, StorableModel>());
    
    /**
     * Registers a <code>StorableModel</code> under the given <code>DHTValueType</code>.
     */
    public StorableModel addStorableModel(DHTValueType valueType, StorableModel model) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        if (model == null) {
            throw new NullPointerException("StorableModel is null");
        }
        
        return models.put(valueType, model);
    }
    
    /**
     * Removes and returns a <code>StorableModel</code> that is registered under the
     * given <code>DHTValueType</code>.
     */
    public StorableModel removeStorableModel(DHTValueType valueType) {
        if (valueType == null) {
            throw new NullPointerException("DHTValueType is null");
        }
        
        return models.remove(valueType);
    }
    
    /**
     * Returns all <code>Storables</code>.
     */
    Collection<Storable> getStorables() {
        List<Storable> values = new ArrayList<Storable>();
        synchronized (models) {
            for (StorableModel model : models.values()) {
                Collection<Storable> storables = model.getStorables();
                
                if (storables == null) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(model + " returned a null Collection");
                    }
                    continue;
                }
                
                for (Storable storable : storables) {
                    if (storable == null) {
                        if (LOG.isErrorEnabled()) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error(model + " returned a Collection with a null element");
                            }
                        }
                        
                        // The StorableModel implementation is incorrect!
                        // Do not continue with adding the remaining non 
                        // null elements!
                        break;
                    }
                    
                    values.add(storable);
                }
            }
        }
        return values;
    }
    
    /**
     * Notifies a StorableModel about the result of a STORE
     * operation.
     */
    void handleStoreResult(Storable value, StoreResult result) {
        DHTValueType type = value.getValue().getValueType();
        StorableModel model = models.get(type);
        if (model != null) {
            model.handleStoreResult(value, result);
        }
    }
    
    /**
     * Notifies all <code>StorableModels</code> that the local
     * Contact's contact information changed.
     */
    public void handleContactChange(Context context) {
        synchronized (models) {
            for (StorableModel model : models.values()) {
                model.handleContactChange();
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        synchronized (models) {
            for (DHTValueType type : models.keySet()) {
                StorableModel model = models.get(type);
                buffer.append(type).append(":\n");
                buffer.append(model).append("\n\n");
            }
        }
        return buffer.toString();
    }
}
