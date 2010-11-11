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

import java.util.Collection;
import java.util.Map.Entry;

import org.limewire.mojito.Context;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.EntryImpl;
import org.limewire.security.SecurityToken;

/**
 * Manages storing values along a path.
 */
public class StoreManager extends AbstractManager<StoreResult> {
    
    public StoreManager(Context context) {
        super(context);
    }
    
    /**
     * Stores a collection of <code>DHTValueEntity</code>s on the DHT. All 
     * <code>DHTValueEntity</code>s must have the same valueId.
     */
    public DHTFuture<StoreResult> store(Collection<? extends DHTValueEntity> values) {
        StoreProcess task = new StoreProcess(context, values);
        StoreFuture future = new StoreFuture(task);
        
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * Stores a collection of <code>DHTValueEntity</code> at the given 
     * <code>Contact</code>.
     */
    public DHTFuture<StoreResult> store(Contact node, SecurityToken securityToken, 
            Collection<? extends DHTValueEntity> values) {
        
        Entry<Contact, SecurityToken> entry 
            = new EntryImpl<Contact, SecurityToken>(node, securityToken);
        StoreProcess task = new StoreProcess(context, entry, values);
        
        StoreFuture future = new StoreFuture(task);
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * A store specific implementation of DHTFuture.
     */
    private class StoreFuture extends DHTFutureTask<StoreResult> {
        
        public StoreFuture(DHTTask<StoreResult> task) {
            super(context, task);
        }
    }
}
