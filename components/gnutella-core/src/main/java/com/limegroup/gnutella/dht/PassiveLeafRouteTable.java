package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.ClassfulNetworkCounter;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.FixedSizeHashMap;

class PassiveLeafRouteTable implements RouteTable {
    
    private static final long serialVersionUID = 2378400850935282184L;

    private final Bucket bucket;
    
    private final Contact localNode;
    
    public PassiveLeafRouteTable(Vendor vendor, Version version) {
        localNode = ContactFactory.createLocalContact(vendor, version, true);
        bucket = new BucketImpl(this, KademliaSettings.REPLICATION_PARAMETER.getValue());
    }
    
    public synchronized void add(Contact node) {
        if (isLocalNode(node)) {
            return;
        }
        
        if (node.isFirewalled()) {
            return;
        }
        
        ClassfulNetworkCounter counter = bucket.getClassfulNetworkCounter();
        if (counter == null || counter.isOkayToAdd(node)) {
            bucket.addActiveContact(node);
        }
    }

    public synchronized void addRouteTableListener(RouteTableListener l) {
    }

    public synchronized void removeRouteTableListener(RouteTableListener l) {
    }
    
    public synchronized void clear() {
        bucket.clear();
    }

    public synchronized Contact get(KUID nodeId) {
        if (nodeId.equals(localNode.getNodeID())) {
            return localNode;
        }
        
        return bucket.get(nodeId);
    }

    public synchronized Collection<Contact> getActiveContacts() {
        List<Contact> nodes = new ArrayList<Contact>(bucket.getActiveContacts());
        nodes.add(localNode);
        return nodes;
    }

    public synchronized Bucket getBucket(KUID nodeId) {
        return bucket;
    }

    public synchronized Collection<Bucket> getBuckets() {
        return Collections.singleton(bucket);
    }

    public synchronized Collection<Contact> getCachedContacts() {
        return bucket.getCachedContacts();
    }

    public synchronized Collection<Contact> getContacts() {
        return bucket.getActiveContacts();
    }

    public synchronized Contact getLocalNode() {
        return localNode;
    }

    public synchronized Collection<KUID> getRefreshIDs(boolean bootstrapping) {
        return Collections.emptySet();
    }

    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        bucket.remove(nodeId);
    }

    public synchronized boolean isLocalNode(Contact node) {
        return localNode.equals(node);
    }

    public synchronized void purge(long elapsedTimeSinceLastContact) {
    }

    public synchronized void purge(PurgeMode first, PurgeMode... rest) {
    }
    
    public synchronized Collection<Contact> select(KUID nodeId, int count, SelectMode mode) {
        Collection<Contact> nodes = bucket.select(nodeId, count);
        if (nodes.size() >= count || mode == SelectMode.ALIVE) {
            return nodes;
        }
        
        nodes = new ArrayList<Contact>(nodes);
        nodes.add(localNode);
        return nodes;
    }

    public synchronized Contact select(KUID nodeId) {
        Contact c = bucket.select(nodeId);
        if (c != null) {
            return c;
        }
        return localNode;
    }

    public synchronized void setContactPinger(ContactPinger pinger) {
    }
    
    public void setNotifier(DHTExecutorService e){}

    public synchronized int size() {
        return bucket.size() + 1;
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getLocalNode()).append("\n");
        buffer.append(bucket);
        return buffer.toString();
    }
    
    private static class BucketImpl implements Bucket {
        
        private static final long serialVersionUID = 7625655390844705296L;
        
        private final RouteTable routeTable;
        
        private final int k;
        
        private final Map<KUID, Contact> map;
        
        private final ClassfulNetworkCounter counter;
        
        public BucketImpl(RouteTable routeTable, int k) {
            this.routeTable = routeTable;
            this.k = k;
            
            counter = new ClassfulNetworkCounter(this);
            
            map = new FixedSizeHashMap<KUID, Contact>(k, 0.75f, true, k) {
                private static final long serialVersionUID = 4026436727356877846L;

                @Override
                protected boolean removeEldestEntry(Entry<KUID, Contact> eldest) {
                    if (super.removeEldestEntry(eldest)) {
                        getClassfulNetworkCounter().decrementAndGet(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            };
        }
        
        public ClassfulNetworkCounter getClassfulNetworkCounter() {
            return counter;
        }

        public boolean isLocalNode(Contact node) {
            return routeTable.isLocalNode(node);
        }
        
        public void addActiveContact(Contact node) {
            updateContact(node);
        }

        public Contact updateContact(Contact node) {
            Contact existing = map.remove(node.getNodeID());
            if (existing != null) {
                getClassfulNetworkCounter().decrementAndGet(existing);
            }
            
            Contact previous = map.put(node.getNodeID(), node);
            assert (previous == null);
            getClassfulNetworkCounter().incrementAndGet(node);
            
            return existing;
        }
        
        public Contact addCachedContact(Contact node) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            map.clear();
        }

        public boolean contains(KUID nodeId) {
            return map.containsKey(nodeId);
        }

        public boolean containsActiveContact(KUID nodeId) {
            return contains(nodeId);
        }

        public boolean containsCachedContact(KUID nodeId) {
            return false;
        }

        public Contact get(KUID nodeId) {
            return map.get(nodeId);
        }

        public Contact getActiveContact(KUID nodeId) {
            return get(nodeId);
        }

        public Collection<Contact> getActiveContacts() {
            return new ArrayList<Contact>(map.values());
        }

        public Collection<Contact> getCachedContacts() {
            return Collections.emptySet();
        }

        public int getActiveSize() {
            return map.size();
        }
        
        public int getCacheSize() {
            return getCachedContacts().size();
        }
        
        public KUID getBucketID() {
            return KUID.MINIMUM;
        }

        public Contact getCachedContact(KUID nodeId) {
            return null;
        }
        
        public int getDepth() {
            return 0;
        }

        public Contact getLeastRecentlySeenActiveContact() {
            Contact lrs = null;
            for (Contact c : map.values()) {
                lrs = c;
            }
            return lrs;
        }

        public Contact getLeastRecentlySeenCachedContact() {
            return null;
        }

        public Contact getMostRecentlySeenActiveContact() {
            for (Contact c : map.values()) {
                return c;
            }
            return null;
        }

        public Contact getMostRecentlySeenCachedContact() {
            return null;
        }

        public long getTimeStamp() {
            return Long.MAX_VALUE;
        }

        public boolean isActiveFull() {
            return false;
        }

        public int getMaxActiveSize() {
            return k;
        }

        public boolean isCacheFull() {
            return false;
        }

        public boolean isRefreshRequired() {
            return false;
        }

        public boolean isTooDeep() {
            return false;
        }

        public void purge() {
        }

        public boolean remove(KUID nodeId) {
            Contact node = map.remove(nodeId);
            if (node != null) {
                getClassfulNetworkCounter().decrementAndGet(node);
                return true;
            }
            return false;
        }

        public boolean removeActiveContact(KUID nodeId) {
            return remove(nodeId);
        }

        public boolean removeCachedContact(KUID nodeId) {
            return false;
        }

        public Collection<Contact> select(KUID nodeId, int count) {
            Contact[] nodes = new Contact[Math.min(count, map.size())];
            int index = 0;
            for (Contact c : map.values()) {
                if (index >= nodes.length) {
                    break;
                }
                
                nodes[index++] = c;
            }
            return Arrays.asList(nodes);
        }

        public Contact select(KUID nodeId) {
            for (Contact c : map.values()) {
                return c;
            }
            return null;
        }

        public int size() {
            return getActiveSize() + getCacheSize();
        }

        public List<Bucket> split() {
            throw new UnsupportedOperationException();
        }

        public void touch() {
        }

        @Override
        public String toString() {
            return CollectionUtils.toString(map.values());
        }

        public boolean isInSmallestSubtree() {
            return false;
        }
    }
}
