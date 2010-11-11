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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntHashMap;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.mojito.util.ContactUtils;

/**
 * Keeps track of the number of Contacts
 * that have the same Class C Network address in a Bucket.
 */
public class ClassfulNetworkCounter implements Serializable {
    
    private static final long serialVersionUID = -6603762323364585225L;

    private static final Log LOG = LogFactory.getLog(ClassfulNetworkCounter.class);
    
    private final IntHashMap<AtomicInteger> nodesPerNetwork = new IntHashMap<AtomicInteger>();
    
    private final Bucket bucket;
    
    public ClassfulNetworkCounter(Bucket bucket) {
        this.bucket = bucket;
    }
    
    /**
     * Returns the Bucket of this ClassfulNetworkCounter.
     */
    public Bucket getBucket() {
        return bucket;
    }
    
    /**
     * Returns the current number of Contacts that
     * are from the same Class C Network.
     */
    public synchronized int get(Contact node) {
        if (bucket.isLocalNode(node)) {
            return 0;
        }
        
        if (!ContactUtils.isIPv4Address(node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has not an IPv4 Address");
            }
            return 0;
        }
        
        int masked = ContactUtils.getClassC(node);
        AtomicInteger counter = nodesPerNetwork.get(masked);
        if (counter != null) {
            return counter.get();
        }
        
        return 0;
    }
    
    /**
     * Increments and returns the current number of Contacts 
     * that are from the same Class C Network as the given
     * Contact.
     */
    public synchronized int incrementAndGet(Contact node) {
        if (bucket.isLocalNode(node)) {
            return 0;
        }
        
        if (!ContactUtils.isIPv4Address(node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has not an IPv4 Address");
            }
            return 0;
        }
        
        int masked = ContactUtils.getClassC(node);
        AtomicInteger counter = nodesPerNetwork.get(masked);
        if (counter == null) {
            
            assert (nodesPerNetwork.size() < bucket.getMaxActiveSize())
                : nodesPerNetwork.size() + " < " + bucket.getMaxActiveSize()
                + node + ", " + nodesPerNetwork + ", " + bucket;
            
            counter = new AtomicInteger(0);
            nodesPerNetwork.put(masked, counter);
        }
        
        return counter.incrementAndGet();
    }
    
    /**
     * Decrements and returns the current number of Contacts 
     * that are from the same Class C Network as the given
     * Contact.
     */
    public synchronized int decrementAndGet(Contact node) {
        if (bucket.isLocalNode(node)) {
            return 0;
        }
        
        if (!ContactUtils.isIPv4Address(node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has not an IPv4 Address");
            }
            return 0;
        }
        
        int masked = ContactUtils.getClassC(node);
        AtomicInteger counter = nodesPerNetwork.get(masked);
        if (counter != null) {
            int count = counter.decrementAndGet();
            if (count <= 0) {
                nodesPerNetwork.remove(masked);
                assert (!nodesPerNetwork.containsKey(masked));
            }
            return count;
        }
        return 0;
    }
    
    /**
     * Returns the number of Elements.
     */
    public synchronized int size() {
        return nodesPerNetwork.size();
    }
    
    /**
     * Clears the ClassfulNetworkCounter.
     */
    public synchronized void clear() {
        nodesPerNetwork.clear();
    }
    
    /**
     * Returns true if it's Okay to add the given Contact to the Bucket. 
     * 
     * @see RouteTableSettings#MAX_CONTACTS_PER_NETWORK_CLASS_RATIO
     */
    public synchronized boolean isOkayToAdd(Contact node) {
        // We rely on the fact that get() returns 0 for
        // the local Node and for non IPv4 Contacts!
        float count = get(node);
        if (count == 0.0f) {
            return true;
        }
        
        assert (!bucket.isLocalNode(node));
        assert (ContactUtils.isIPv4Address(node));
        
        // If it's 1.0f then allow
        float maxRatio = RouteTableSettings.MAX_CONTACTS_PER_NETWORK_CLASS_RATIO.getValue();
        if (maxRatio >= 1.0f) {
            return true;
        }
        
        float k = bucket.getMaxActiveSize();
        float ratio = count/k;
        return (ratio < maxRatio);
    }
    
    @Override
    public synchronized String toString() {
        return nodesPerNetwork.toString();// + ", " + bucket;
    }
}
