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

package org.limewire.mojito.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.handler.response.FindValueResponseHandler;
import org.limewire.mojito.result.FindValueResult;


/**
 * Manages lookups for values.
 */
public class FindValueManager extends AbstractManager<FindValueResult> {
    
    private final Map<EntityKey, FindValueFuture> futureMap = 
        Collections.synchronizedMap(new HashMap<EntityKey, FindValueFuture>());
    
    public FindValueManager(Context context) {
        super(context);
    }

    public void init() {
        futureMap.clear();
    }
    
    /**
     * Starts a lookup for the given KUID.
     */
    public DHTFuture<FindValueResult> lookup(EntityKey entityKey) {
        return lookup(entityKey, -1);
    }
    
    /**
     * Starts a lookup for the given KUID and expects 'count' 
     * number of results.
     */
    private DHTFuture<FindValueResult> lookup(EntityKey entityKey, int count) {
        
        FindValueFuture future = null;
        synchronized(futureMap) {
            future = futureMap.get(entityKey);
            if (future == null) {
                FindValueResponseHandler handler 
                    = new FindValueResponseHandler(context, entityKey);
                
                future = new FindValueFuture(entityKey, handler);
                futureMap.put(entityKey, future);
                context.getDHTExecutorService().execute(future);
            }
        }
        
        return future;
    }
    
    /**
     * The DHTFuture for FIND_VALUE.
     */
    private class FindValueFuture extends DHTFutureTask<FindValueResult> {

        private final EntityKey entityKey;
        
        private final DHTTask<FindValueResult> handler;
        
        public FindValueFuture(EntityKey entityKey, DHTTask<FindValueResult> handler) {
            super(context, handler);
            this.entityKey = entityKey;
            this.handler = handler;
        }

        @Override
        protected void done0() {
            futureMap.remove(entityKey);
        }

        @Override
        public String toString() {
            return "FindValueFuture: " + entityKey + ", " + handler;
        }
    }
}
