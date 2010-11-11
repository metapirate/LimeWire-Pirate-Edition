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
 
package org.limewire.mojito.routing.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.TrieUtils;
import org.limewire.collection.Trie.Cursor;
import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.ClassfulNetworkCounter;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.mojito.util.FixedSizeHashMap;


/**
 * An implementation of Bucket
 */
class BucketNode implements Bucket {
    
    private static final long serialVersionUID = -4116522147032657308L;
    
    private final RouteTable routeTable;
    
    private final KUID bucketId;
    
    private final int depth;
    
    private final PatriciaTrie<KUID, Contact> nodeTrie;
    
    private transient Map<KUID, Contact> cache;
    
    private transient ClassfulNetworkCounter counter;
    
    private long timeStamp = 0L;
    
    public BucketNode(RouteTable routeTable, KUID bucketId, int depth) {
        this.routeTable = routeTable;
        this.bucketId = bucketId;
        this.depth = depth;
        
        nodeTrie = new PatriciaTrie<KUID, Contact>(KUID.KEY_ANALYZER);
        init();
    }
    
    private void init() {
        cache = Collections.emptyMap();
        counter = new ClassfulNetworkCounter(this);
    }
    
    void postInit() {
        for (Contact node : nodeTrie.values()) {
            counter.incrementAndGet(node);
        }
    }
    
    public KUID getBucketID() {
        return bucketId;
    }
    
    public RouteTable getRouteTable() {
        return routeTable;
    }
    
    public boolean isLocalNode(Contact node) {
        return routeTable.isLocalNode(node);
    }
    
    public ClassfulNetworkCounter getClassfulNetworkCounter() {
        return counter;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public void touch() {
        timeStamp = System.currentTimeMillis();
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public void addActiveContact(Contact node) {
        checkNodeID(node);
        assert (isActiveFull() == false);
        
        Contact existing = nodeTrie.put(node.getNodeID(), node);
        assert (existing == null);
        
        if(node.isAlive()) {
            touch();
        }
        
        counter.incrementAndGet(node);
    }
    
    public Contact addCachedContact(Contact node) {
        checkNodeID(node);
        if (cache == Collections.EMPTY_MAP) {
            int maxSize = RouteTableSettings.MAX_CACHE_SIZE.getValue();
            cache = new FixedSizeHashMap<KUID, Contact>(maxSize/2, 0.75f, true, maxSize);
        }
        
        if (!isCacheFull()) {
            Contact existing = cache.put(node.getNodeID(), node);
            assert (existing == null);
        } else {
            Contact lrs = getLeastRecentlySeenCachedContact();
            if (!lrs.isAlive() || (!lrs.hasBeenRecentlyAlive() && node.isAlive())) {
                Contact c = cache.remove(lrs.getNodeID());
                assert (c == lrs);
                cache.put(node.getNodeID(), node);
                return c;
            }
        }
        
        return null;
    }
    
    public Contact updateContact(Contact node) {
        checkNodeID(node);
        
        KUID nodeId = node.getNodeID();
        if (containsActiveContact(nodeId)) {
            Contact current = nodeTrie.put(nodeId, node);
            assert (current != null);

            // Remove the old Network
            counter.decrementAndGet(current);

            // And add the new Network
            counter.incrementAndGet(node);
            return current;
        } else if (containsCachedContact(nodeId)) {
            return cache.put(nodeId, node);
        }
        
        throw new IllegalStateException(node + " is not in this Bucket " + toString());
    }
    
    // TODO: Disable/Delete when finished with testing!
    private void checkNodeID(Contact node) {
        if (depth <= 0) {
            // This is the ROOT Bucket!
            return;
        }
        
        int bitIndex = bucketId.bitIndex(node.getNodeID());
        if (bitIndex < 0) {
            return;
        }
        
        assert (bitIndex >= depth) : "Wrong Bucket";
    }
    
    public Contact get(KUID nodeId) {
        Contact node = getActiveContact(nodeId);
        if (node == null) {
            node = getCachedContact(nodeId);
        }
        return node;
    }
    
    public Contact getActiveContact(KUID nodeId) {
        return nodeTrie.get(nodeId);
    }
    
    public Contact getCachedContact(KUID nodeId) {
        return cache.get(nodeId);
    }
    
    public Contact select(KUID nodeId) {
        return nodeTrie.select(nodeId);
    }
    
    public Collection<Contact> select(KUID nodeId, int count) {
        return TrieUtils.select(nodeTrie, nodeId, count);
    }
    
    public boolean remove(KUID nodeId) {
        if (removeActiveContact(nodeId)) {
            return true;
        } else {
            return removeCachedContact(nodeId);
        }
    }
    
    public boolean removeActiveContact(KUID nodeId) {
        Contact node = nodeTrie.remove(nodeId);
        if (node != null) {
            int old = counter.get(node);
            int now = counter.decrementAndGet(node);
            assert (now < old) : now + " < " + old + ", " + nodeId + ", " + node + this;
            return true;
        }
        return false;
    }
    
    public boolean removeCachedContact(KUID nodeId) {
        if (cache.remove(nodeId) != null) {
            if (cache.isEmpty()) {
                cache = Collections.emptyMap();
            }
            return true;
        } else {
            return false;
        }
    }
    
    public boolean contains(KUID nodeId) {
        if (containsActiveContact(nodeId)) {
            return true;
        } else {
            return containsCachedContact(nodeId);
        }
    }
    
    public boolean containsActiveContact(KUID nodeId) {
        return nodeTrie.containsKey(nodeId);
    }
    
    public boolean containsCachedContact(KUID nodeId) {
        return cache.containsKey(nodeId);
    }
    
    public boolean isActiveFull() {
        return nodeTrie.size() >= getMaxActiveSize();
    }
    
    public boolean isCacheFull() {
        return !cache.isEmpty() && ((FixedSizeHashMap<KUID, Contact>)cache).isFull();
    }
    
    public boolean isInSmallestSubtree() {
        int commonPrefixLength = routeTable.getLocalNode().getNodeID().getCommonPrefixLength(bucketId);
        int localNodeDepth = routeTable.getBucket(routeTable.getLocalNode().getNodeID()).getDepth();
        return (localNodeDepth - 1) == commonPrefixLength;
    }
    
    public boolean isTooDeep() {
        int commonPrefixLength = routeTable.getLocalNode().getNodeID().getCommonPrefixLength(bucketId);        
        return (depth - commonPrefixLength) >= RouteTableSettings.DEPTH_LIMIT.getValue();
    }

    public Collection<Contact> getActiveContacts() {
        return nodeTrie.values();
    }
    
    public Collection<Contact> getCachedContacts() {
        return cache.values();
    }
    
    public Contact getLeastRecentlySeenActiveContact() {
        final Contact[] leastRecentlySeen = new Contact[]{ null };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public SelectStatus select(Map.Entry<? extends KUID, ? extends Contact> entry) {
                Contact node = entry.getValue();
                Contact lrs = leastRecentlySeen[0];
                if (lrs == null || node.getTimeStamp() < lrs.getTimeStamp()) {
                    leastRecentlySeen[0] = node;
                }
                return SelectStatus.CONTINUE;
            }
        });
        return leastRecentlySeen[0];
    }
    
    public Contact getMostRecentlySeenActiveContact() {
        final Contact[] mostRecentlySeen = new Contact[]{ null };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public SelectStatus select(Map.Entry<? extends KUID, ? extends Contact> entry) {
                Contact node = entry.getValue();
                Contact mrs = mostRecentlySeen[0];
                if (mrs == null || node.getTimeStamp() > mrs.getTimeStamp()) {
                    mostRecentlySeen[0] = node;
                }
                return SelectStatus.CONTINUE;
            }
        });
        return mostRecentlySeen[0];
    }
    
    public void purge() {
        for (Iterator<Contact> it = nodeTrie.values().iterator(); it.hasNext(); ) {
            Contact node = it.next();
            if(!node.isAlive() && !isLocalNode(node)) {
                it.remove();
            }
        }
        
        if(!isActiveFull() && !cache.isEmpty()) {
            // The cache Map is in LRS order. Add the Contacts to a List and 
            // iterate it backwards so that we get the elements in MRS order.
            List<Contact> contacts = new ArrayList<Contact>(getCachedContacts());
            for(int i = contacts.size()-1; i>=0 && !isActiveFull(); i--) {
                Contact node = contacts.get(i);
                if(node.isAlive()) {
                    nodeTrie.put(node.getNodeID(), node);
                }
                
                boolean removed = removeCachedContact(node.getNodeID());
                assert (removed);
            }
        }
    }

    // O(1)
    public Contact getLeastRecentlySeenCachedContact() {
        if (getCachedContacts().isEmpty()) {
            return null;
        }
        
        return getCachedContacts().iterator().next();
    }
    
    // O(n)
    public Contact getMostRecentlySeenCachedContact() {
        Contact node = null;
        for(Contact n : getCachedContacts()) {
            node = n;
        }
        return node;
    }
    
    public List<Bucket> split() {

        assert (getCachedContacts().isEmpty() == true);
        
        Bucket left = new BucketNode(routeTable, bucketId, depth+1);
        Bucket right = new BucketNode(routeTable, bucketId.set(depth), depth+1);
        
        for (Contact node : getActiveContacts()) {
            KUID nodeId = node.getNodeID();
            if (!nodeId.isBitSet(depth)) {
                left.addActiveContact(node);
            } else {
                right.addActiveContact(node);
            }
        }
        
        assert ((left.size() + right.size()) == size()) : "left: " + left + ", right: " + right + ", old: " + nodeTrie;
        return Arrays.asList(left, right);
    }
    
    public int size() {
        return getActiveSize() + getCacheSize();
    }
    
    public int getActiveSize() {
        return nodeTrie.size();
    }
   
    public int getMaxActiveSize() {
        return KademliaSettings.REPLICATION_PARAMETER.getValue();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public boolean isRefreshRequired() {
        if ((System.currentTimeMillis() - getTimeStamp()) 
                >= RouteTableSettings.BUCKET_REFRESH_PERIOD.getValue()) {
            return true;
        }
        return false;
    }
    
    public void clear() {
        nodeTrie.clear();
        cache = Collections.emptyMap();
    }
    
    @Override
    public int hashCode() {
        return bucketId.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BucketNode)) {
            return false;
        }
        
        BucketNode other = (BucketNode)o;
        return bucketId.equals(other.bucketId)
                && depth == other.depth;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(bucketId).append(" (depth=").append(getDepth())
            .append(", active=").append(getActiveSize())
            .append(", cache=").append(getCacheSize()).append(")\n");
        
        Iterator<Contact> it = getActiveContacts().iterator();
        for(int i = 0; it.hasNext(); i++) {
            buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
        }
        
        if (!getCachedContacts().isEmpty()) {
            buffer.append("---\n");
            it = getCachedContacts().iterator();
            for(int i = 0; it.hasNext(); i++) {
                buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
            }
        }
        
        return buffer.toString();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
}
