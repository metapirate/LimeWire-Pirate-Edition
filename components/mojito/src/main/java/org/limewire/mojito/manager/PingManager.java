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

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.handler.response.PingResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler.PingIterator;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.ContactUtils;

/**
 * Takes care of concurrent Pings and makes sure
 * a single Node cannot be pinged multiple times in parallel.
 */
public class PingManager extends AbstractManager<PingResult> {
    
    private final Map<SocketAddress, PingFuture> futureMap 
        = Collections.synchronizedMap(new HashMap<SocketAddress, PingFuture>());
    
    public PingManager(Context context) {
        super(context);
    }
    
    public void init() {
        futureMap.clear();
    }
    
    /**
     * Sends a ping to the remote Host.
     */
    public DHTFuture<PingResult> ping(SocketAddress host) {
        PingIterator pinger = new PingIteratorFactory.SocketAddressPinger(host);
        return ping(null, host, pinger);
    }

    public DHTFuture<PingResult> pingAddresses(Set<? extends SocketAddress> hosts) {
        PingIterator pinger = new PingIteratorFactory.SocketAddressPinger(hosts);
        return ping(null, null, pinger);
    }
    
    /**
     * Sends a ping to the remote Node.
     */
    public DHTFuture<PingResult> ping(Contact node) {
        PingIterator pinger = new PingIteratorFactory.ContactPinger(node);
        return ping(null, node.getContactAddress(), pinger);
    }
    
    /**
     * Sends a ping to the remote Node.
     */
    public DHTFuture<PingResult> ping(KUID nodeId, SocketAddress address) {
        PingIterator pinger = new PingIteratorFactory.EntryPinger(nodeId, address);
        return ping(null, address, pinger);
    }
    
    /**
     * Sends a ping to the remote Node.
     */
    public DHTFuture<PingResult> ping(Set<? extends Contact> nodes) {
        PingIterator pinger = new PingIteratorFactory.ContactPinger(nodes);
        return ping(null, null, pinger);
    }
    
    /**
     * Sends a special ping to the given Node to test if there
     * is a Node ID collision.
     */
    public DHTFuture<PingResult> collisionPing(Contact node) {
        return collisionPing(node.getContactAddress(), Collections.singleton(node));
    }
    
    public DHTFuture<PingResult> collisionPing(Set<? extends Contact> nodes) {
        return collisionPing(null, nodes);
    }
    
    /**
     * Sends a special ping to the given Node to test if there
     * is a Node ID collision.
     */
    private DHTFuture<PingResult> collisionPing(SocketAddress key, Set<? extends Contact> nodes) {
        Contact sender = ContactUtils.createCollisionPingSender(context.getLocalNode());
        PingIterator pinger = new PingIteratorFactory.CollisionPinger(context, sender, nodes);
        return ping(sender, key, pinger);
    }
    
    /**
     * Sends a ping to the remote Node.
     * 
     * @param sender the local Node
     * @param key the remote Node's address
     * @param pinger sends ping requests
     */
    private DHTFuture<PingResult> ping(Contact sender, SocketAddress key, PingIterator pinger) {
        PingFuture future = null;
        synchronized (futureMap) {
            future = (key != null ? futureMap.get(key) : null);

            if (future == null) {
                PingResponseHandler handler = new PingResponseHandler(context, sender, pinger);

                future = new PingFuture(key, handler);
                if (key != null) {
                    futureMap.put(key, future);
                }
                
                context.getDHTExecutorService().execute(future);
            }
        }
        
        return future;
    }
    
    /**
     * A ping specific implementation of DHTFuture. 
     */
    private class PingFuture extends DHTFutureTask<PingResult> {

        private final SocketAddress key;
        
        public PingFuture(SocketAddress key, DHTTask<PingResult> handler) {
            super(context, handler);
            this.key = key;
        }
        
        @Override
        protected void done0() {
            if (key != null) {
                futureMap.remove(key);
            }
        }
    }
}
