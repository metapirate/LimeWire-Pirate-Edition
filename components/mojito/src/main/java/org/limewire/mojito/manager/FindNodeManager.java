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
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.result.FindNodeResult;


/**
 * FindNodeManager manages lookups for Nodes.
 */
public class FindNodeManager extends AbstractManager<FindNodeResult> {

    private final Map<KUID, FindNodeFuture> futureMap = 
        Collections.synchronizedMap(new HashMap<KUID, FindNodeFuture>());
    
    public FindNodeManager(Context context) {
        super(context);
    }

    public void init() {
        futureMap.clear();
    }
    
    /**
     * Starts a lookup for the given KUID.
     */
    public DHTFuture<FindNodeResult> lookup(KUID lookupId) {
        return lookup(lookupId, -1);
    }
    
    /**
     * Starts a lookup for the given KUID and expects 'count' 
     * number of results.
     */
    private DHTFuture<FindNodeResult> lookup(KUID lookupId, int count) {
        FindNodeFuture future = null;
        synchronized(futureMap) {
            future = futureMap.get(lookupId);
            if (future == null) {
                FindNodeResponseHandler handler 
                    = createFindNodeResponseHandler(context, lookupId, count);
                future = new FindNodeFuture(lookupId, handler);
                
                futureMap.put(lookupId, future);
                context.getDHTExecutorService().execute(future);
            }
        }
        
        return future;
    }
    
    /**
     * Creates and returns a FindNodeResponseHandler.
     */
    protected FindNodeResponseHandler createFindNodeResponseHandler(
            Context context, KUID lookupId, int count) {
        return new FindNodeResponseHandler(context, lookupId, count);
    }
    
    /**
     * The DHTFuture for FIND_NODE.
     */
    private class FindNodeFuture extends DHTFutureTask<FindNodeResult> {

        private final KUID lookupId;
        
        private final DHTTask<FindNodeResult> handler;
        
        public FindNodeFuture(KUID lookupId, DHTTask<FindNodeResult> handler) {
            super(context, handler);
            this.lookupId = lookupId;
            this.handler = handler;
        }

        @Override
        protected void done0() {
            futureMap.remove(lookupId);
        }

        @Override
        public String toString() {
            return "FindNodeFuture: " + lookupId + ", " + handler;
        }
    }
}
