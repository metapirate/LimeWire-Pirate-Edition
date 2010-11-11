package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.ClassfulNetworkCounter;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;

/**
 * Passive Nodes (Ultrapeers) use this slightly extended version 
 * of the <code>RouteTable</code>. It maintains an internal mapping of DHT 
 * enabled leaves that are currently connected to the Ultrapeer (Gnutella 
 * connections). 
 */
@SuppressWarnings("serial")
class PassiveDHTNodeRouteTable implements RouteTable {
    
    private static final Log LOG = LogFactory.getLog(PassiveDHTNodeRouteTable.class);
    
    /**
     * MojitoDHT instance
     */
    private final MojitoDHT dht;
    
    /**
     * The actual RouteTable
     */
    private final RouteTable delegate;
    
    /**
     * The Map storing the leaf nodes connected to this ultrapeer. The mapping is
     * used to go from Gnutella <tt>IpPort</tt> to DHT <tt>RemoteContact</tt>. 
     */
    private final Map<SocketAddress, KUID> leafDHTNodes = new HashMap<SocketAddress, KUID>();
    
    public PassiveDHTNodeRouteTable(MojitoDHT dht) {
        assert (dht.isFirewalled()); // Must be firewalled
        
        this.dht = dht;
        delegate = dht.getRouteTable();
    }

    /**
     * Adds a DHT leaf node to the DHT routing table, setting a very high timestamp
     * to make sure it always gets contacted first for the first hop of lookups, and 
     * make sure it always gets returned first when selecting the Most Recently Seen
     * (MRS) nodes from the RT.
     * 
     * @param host the IP address of the remote host as a string
     * @param port the listening port for the remote host
     */
    public void addLeafDHTNode(String host, int port) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Pinging leaf: " + host + ": " + port);
        }
        
        final InetSocketAddress addr = new InetSocketAddress(host, port);
        DHTFuture<PingResult> future = dht.ping(addr);
        
        DHTFutureAdapter<PingResult> listener = new DHTFutureAdapter<PingResult>() {
            @Override
            protected void operationComplete(FutureEvent<PingResult> event) {
                switch (event.getType()) {
                    case SUCCESS:
                        handleFutureSuccess(event.getResult());
                        break;
                    case EXCEPTION:
                        handleExecutionException(event.getException());
                        break;
                }
            }

            private void handleFutureSuccess(PingResult result) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping succeeded to: " + result);
                }
                
                Contact node = result.getContact();
                synchronized (PassiveDHTNodeRouteTable.this) {
                    KUID previous = leafDHTNodes.put(addr, node.getNodeID());
                    
                    if (previous == null || !previous.equals(node.getNodeID())) {
                        // Add it as a priority Node
                        node.setTimeStamp(Contact.PRIORITY_CONTACT);
                        add(node);
                    }
                }
            }

            private void handleExecutionException(ExecutionException e) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping failed to: " + addr, e);
                }
            }
        };
        
        future.addFutureListener(listener);
    }
    
    /**
     * Removes this DHT leaf from our routing table and returns it.
     */
    public synchronized SocketAddress removeLeafDHTNode(String host, int port) {
        
        SocketAddress addr = new InetSocketAddress(host, port);
        KUID nodeId = leafDHTNodes.remove(addr);
        if(nodeId != null) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Removed leaf: " + host + ": " + port);
            }
            
            removeAndReplaceWithMRSCachedContact(nodeId);
            return addr;
        }
        return null;
    }
    
    /**
     * Removes the given node from the routing table and tries to replace
     * it with the Most Recently Seen (MRS) cached contact.
     * 
     * @param nodeId the KUID of the node to remove
     */
    private synchronized void removeAndReplaceWithMRSCachedContact(KUID nodeId) {
        Bucket bucket = getBucket(nodeId);
        boolean removed = bucket.removeActiveContact(nodeId);

        if (removed) {
            if (bucket.getCacheSize() > 0) {
                ClassfulNetworkCounter counter = bucket.getClassfulNetworkCounter();
                
                Contact mrs = null;
                while((mrs = bucket.getMostRecentlySeenCachedContact()) != null) {
                    removed = bucket.removeCachedContact(mrs.getNodeID());
                    assert (removed == true);
                    
                    if (counter == null || counter.isOkayToAdd(mrs)) {
                        bucket.addActiveContact(mrs);
                        break;
                    }
                }
            }
        } else {
            bucket.removeCachedContact(nodeId);
        }
    }
    
    /**
     * Returns whether or not this Ultrapeer has any
     * DHT enabled leaves
     */
    public synchronized boolean hasDHTLeaves() {
        return !leafDHTNodes.isEmpty();
    }
    
    /**
     * Returns the IP:Ports of this Ultrapeer's DHT enabled leaves
     * 
     * Hold a lock on 'this' when using the Iterator! 
     */
    public synchronized Set<SocketAddress> getDHTLeaves() {
        return Collections.unmodifiableSet(leafDHTNodes.keySet());
    }
    
    // --- ROUTE TABLE ---
    
    public synchronized void add(Contact node) {
        delegate.add(node);
    }

    public synchronized void addRouteTableListener(RouteTableListener l) {
        delegate.addRouteTableListener(l);
    }

    public synchronized void removeRouteTableListener(RouteTableListener l) {
        delegate.removeRouteTableListener(l);
    }
    
    public synchronized Contact get(KUID nodeId) {
        return delegate.get(nodeId);
    }

    public synchronized Collection<Contact> getActiveContacts() {
        return delegate.getActiveContacts();
    }

    public synchronized Bucket getBucket(KUID nodeId) {
        return delegate.getBucket(nodeId);
    }

    public synchronized Collection<Bucket> getBuckets() {
        return delegate.getBuckets();
    }

    public synchronized Collection<Contact> getCachedContacts() {
        return delegate.getCachedContacts();
    }

    public synchronized Collection<Contact> getContacts() {
        return delegate.getContacts();
    }

    public synchronized Contact getLocalNode() {
        return delegate.getLocalNode();
    }

    public synchronized Collection<KUID> getRefreshIDs(boolean bootstrapping) {
        return delegate.getRefreshIDs(bootstrapping);
    }

    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        delegate.handleFailure(nodeId, address);
    }

    public synchronized boolean isLocalNode(Contact node) {
        return delegate.isLocalNode(node);
    }

    public synchronized void purge(long elapsedTimeSinceLastContact) {
        delegate.purge(elapsedTimeSinceLastContact);
    }

    public synchronized void purge(PurgeMode first, PurgeMode... rest) {
        delegate.purge(first, rest);
    }
    
    public synchronized Contact select(KUID nodeId) {
        return delegate.select(nodeId);
    }
    
    public synchronized Collection<Contact> select(KUID nodeId, int count, SelectMode mode) {
        return delegate.select(nodeId, count, mode);
    }
    
    public synchronized void setContactPinger(ContactPinger pinger) {
        delegate.setContactPinger(pinger);
    }
    
    public synchronized void setNotifier(DHTExecutorService e) {
        delegate.setNotifier(e);
    }

    public synchronized int size() {
        return delegate.size();
    }
    
    public synchronized void clear() {
        delegate.clear();
    }
    
    @Override
    public synchronized String toString() {
        return "Passive RouteTable: " + delegate.toString();
    }
}
