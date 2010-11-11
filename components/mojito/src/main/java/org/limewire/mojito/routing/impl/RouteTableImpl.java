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
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.Trie.Cursor;
import org.limewire.concurrent.FutureEvent;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.ClassfulNetworkCounter;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent.EventType;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.ExceptionUtils;
import org.limewire.service.ErrorService;


/**
 * A PatriciaTrie based RouteTable implementation for the Mojito DHT.
 * This is the reference implementation.
 */
public class RouteTableImpl implements RouteTable {
    
    private static final long serialVersionUID = -7351267868357880369L;

    private static final Log LOG = LogFactory.getLog(RouteTableImpl.class);
    
    /**
     * Trie of Buckets and the Buckets are a Trie of Contacts.
     */
    private final PatriciaTrie<KUID, Bucket> bucketTrie;
    
    /**
     * A counter for consecutive failures.
     */
    private int consecutiveFailures = 0;
    
    /**
     * A reference to the ContactPinger.
     */
    private transient ContactPinger pinger;
    
    /**
     * The local Node.
     */
    private Contact localNode;
    
    /**
     * A list of RouteTableListeners.
     */
    private transient volatile List<RouteTableListener> listeners 
        = new CopyOnWriteArrayList<RouteTableListener>();
    
    /** 
     * Executor where to offload notifications to RouteTableListeners.
     */
    private transient volatile DHTExecutorService notifier;
    
    /**
     * Create a new RouteTable and generates a new random Node ID
     * for the local Node.
     */
    public RouteTableImpl() {
        this(KUID.createRandomID());
    }
    
    /**
     * Create a new RouteTable and uses the given Node ID
     * for the local Node.
     */
    public RouteTableImpl(byte[] nodeId) {
        this(KUID.createWithBytes(nodeId));
    }
    
    /**
     * Create a new RouteTable and uses the given Node ID
     * for the local Node.
     */
    public RouteTableImpl(String nodeId) {
        this(KUID.createWithHexString(nodeId));
    }
    
    /**
     * Create a new RouteTable and uses the given Node ID
     * for the local Node.
     */
    public RouteTableImpl(KUID nodeId) {
        localNode = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, nodeId, 0, false);
        bucketTrie = new PatriciaTrie<KUID, Bucket>(KUID.KEY_ANALYZER);
        init();
    }
    
    /**
     * Initializes the RouteTable.
     */
    private void init() {
        KUID bucketId = KUID.MINIMUM;
        Bucket bucket = new BucketNode(this, bucketId, 0);
        bucketTrie.put(bucketId, bucket);
        
        addContactToBucket(bucket, localNode);
        
        consecutiveFailures = 0;
    }
    
    private void readObject(ObjectInputStream in) 
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        listeners = new CopyOnWriteArrayList<RouteTableListener>();
        
        // Post-Init the Buckets
        for (Bucket bucket : bucketTrie.values()) {
            ((BucketNode)bucket).postInit();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#setContactPinger(com.limegroup.mojito.routing.RouteTable.ContactPinger)
     */
    public void setContactPinger(ContactPinger pinger) {
        this.pinger = pinger;
    }
    
    public void setNotifier(DHTExecutorService executor) {
        this.notifier = executor;
    }

    /**
     * Adds a RouteTableListener.
     * <p>
     * Implementation Note: The listener(s) is not called from a 
     * separate event Thread! That means processor intensive tasks
     * that are performed straight in the listener(s) can slowdown 
     * the processing throughput significantly. Offload intensive
     * tasks to separate Threads in necessary!
     * 
     * @param l the RouteTableListener instance to add
     */
    public void addRouteTableListener(RouteTableListener l) {
        if (l == null) {
            throw new NullPointerException("RouteTableListener is null");
        }
        
        listeners.add(l);
    }
    
    /**
     * Removes a RouteTableListener.
     * 
     * @param l the RouteTableListener instance to remove
     */
    public void removeRouteTableListener(RouteTableListener l) {
        if (l == null) {
            throw new NullPointerException("RouteTableListener is null");
        }
        
        listeners.remove(l);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#add(com.limegroup.mojito.Contact)
     */
    public synchronized void add(Contact node) {
        
        if (localNode.equals(node)) {
            String msg = "Cannot add the local Node: " + node;
            
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            ErrorService.error(new IllegalArgumentException(msg));
            return;
        }
        
        // Don't add firewalled Nodes
        if (node.isFirewalled()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(node + " is firewalled");
            }
            return;
        }
        
        // Make sure we're not mixing IPv4 and IPv6 addresses in the
        // RouteTable! IPv6 to IPv4 might work if there's a 6to4 gateway
        // or whatsoever but the opposite direction doesn't. An immediate
        // idea is to mark IPv6 Nodes as firewalled if they're contacting
        // IPv4 Nodes but this would lead to problems in the IPv6 network
        // if some IPv6 Nodes don't have access to a 6to4 gateway...
        if (!ContactUtils.isSameAddressSpace(localNode, node)) {
            
            // Log as ERROR so that we're not missing this
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is from a different IP address space than " + localNode);
            }
            return;
        }
        
        consecutiveFailures = 0;
        
        KUID nodeId = node.getNodeID();
        Bucket bucket = bucketTrie.select(nodeId);
        Contact existing = bucket.get(nodeId);
        
        if (existing != null) {
            updateContactInBucket(bucket, existing, node);
        } else if (!bucket.isActiveFull()) {
            if (isOkayToAdd(bucket, node)) {
                addContactToBucket(bucket, node);
            } else {
                // only cache node if the bucket can't be split
                if (!canSplit(bucket)) {
                addContactToBucketCache(bucket, node);
            }
            }
        } else if (split(bucket)) {
            add(node); // re-try to add
        } else {
            replaceContactInBucket(bucket, node);
        }
    }
    
    /**
     * This method updates an existing Contact with data from a new Contact.
     * The initial state is that both Contacts have the same Node ID which
     * doesn't mean they're really the same Node. In order to figure out
     * if they're really equal it's performing some additional checks and
     * there are a few side conditions.
     */
    protected synchronized void updateContactInBucket(Bucket bucket, Contact existing, Contact node) {
        assert (existing.getNodeID().equals(node.getNodeID()));
        
        if (isLocalNode(existing)) {
            // The other Node collides with our Node ID! Do nothing,
            // the other guy will change its Node ID! If it doesn't
            // everybody who has us in their RouteTable will ping us
            // to check if we're alive and we're hopefully able to
            // respond. Besides that there isn't much we can do. :-/
            if (!isLocalNode(node)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(node + " collides with " + existing);
                }
                
            // Must be instance of LocalContact!
            } else if (!(node instanceof LocalContact)) {
                String msg = "Attempting to replace the local Node " 
                    + existing + " with " + node;
                
                if (LOG.isErrorEnabled()) {
                    LOG.error(msg);
                }
                
                throw new IllegalArgumentException(msg);
                
            // Alright, replace the existing Contact with the new
            // LocalContact. Log a warning... 
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Updating " + existing + " with " + node);
                }
                
                bucket.updateContact(node);
                this.localNode = node;
                
                fireContactUpdate(bucket, existing, node);
            }
            
            return;
        }
        
        /*
         * A non-live Contact will never replace a live Contact!
         */
        if (existing.isAlive() && !node.isAlive()) {
            return;
        }
        
        if (!existing.isAlive() 
                || isLocalNode(node)
                || existing.equals(node) // <- checks only nodeId + address!
                || ContactUtils.areLocalContacts(existing, node)) {
            
            /*
             * See JIRA issue MOJITO-54
             */
            
            node.updateWithExistingContact(existing);
            Contact replaced = bucket.updateContact(node);
            assert (replaced == existing);
            
            // a good time to ping least recently seen node if we know we
            // have a node alive in the replacement cache. Don't do this too often!
            long delay = System.currentTimeMillis() - bucket.getTimeStamp();
            if (bucket.containsCachedContact(node.getNodeID())
                    && (delay > RouteTableSettings.BUCKET_PING_LIMIT.getValue())) {
                pingLeastRecentlySeenNode(bucket);
            }
            touchBucket(bucket);
            
            fireContactUpdate(bucket, existing, node);
            
        } else if (node.isAlive() 
                && !existing.hasBeenRecentlyAlive()) {
            
            doSpoofCheck(bucket, existing, node);
        }
    }
    
    /**
     * This method tries to ping the existing Contact and if it doesn't
     * respond it will try to replace it with the new Contact. The initial 
     * state is that both Contacts have the same Node ID.
     */
    protected synchronized void doSpoofCheck(Bucket bucket, final Contact existing, final Contact node) {
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
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " is trying to spoof " + result);
                }
                
                // DO NOTHING! The DefaultMessageHandler takes care 
                // of everything else! DO NOT BAN THE NODE!!!
                // Reason: It was maybe just a Node ID collision!
            }
            
            private void handleExecutionException(ExecutionException e) {
                DHTTimeoutException timeout = ExceptionUtils.getCause(e, DHTTimeoutException.class);
                
                // We can only make decisions for timeouts! 
                if (timeout == null) {
                    return;
                }
                
                KUID nodeId = timeout.getNodeID();
                SocketAddress address = timeout.getSocketAddress();
                
                if (LOG.isInfoEnabled()) {
                    LOG.info(ContactUtils.toString(nodeId, address) 
                            + " did not respond! Replacing it with " + node);
                }
                
                synchronized (RouteTableImpl.this) {
                    Bucket bucket = bucketTrie.select(nodeId);
                    Contact current = bucket.get(nodeId);
                    if (current != null && current.equals(existing)) {
                        
                        /*
                         * See JIRA issue MOJITO-54
                         */
                        
                        // NOTE: We cannot call updateContactInBucket(...) here
                        // because it would do the spoof check again.
                        node.updateWithExistingContact(current);
                        Contact replaced = bucket.updateContact(node);
                        assert (replaced == current);
                        
                        fireContactUpdate(bucket, current, node);
                        
                        // If the Node is in the Cache then ping the least recently
                        // seen live Node which might promote the new Node to a
                        // live Contact!
                        if (bucket.containsCachedContact(nodeId)) {
                            pingLeastRecentlySeenNode(bucket);
                        }
                    } else {
                        add(node);
                    }
                }
            }
        };
        
        fireContactCheck(bucket, existing, node);
        
        ping(existing, listener);
        touchBucket(bucket);
    }
    
    /**
     * This method adds the given Contact to the given Bucket.
     */
    protected synchronized void addContactToBucket(Bucket bucket, Contact node) {
        bucket.addActiveContact(node);
        fireActiveContactAdded(bucket, node);
    }
    
    /**
     * Adds the given Contact to the Bucket's replacement Cache.
     */
    protected synchronized void addContactToBucketCache(Bucket bucket, Contact node) {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding " + node + " to " + bucket + " replacement cache");
        }
        
        // If the cache is full the least recently seen
        // node will be evicted!
        Contact existing = bucket.addCachedContact(node);
        fireCachedContactAdded(bucket, existing, node);
    }
    
    /**
     * Returns true if the bucket can be split.
     */
    private boolean canSplit(Bucket bucket) {
        // Three conditions for splitting:
        // 1. Bucket contains the local Node
        // 2. New node part of the smallest subtree to the local node
        // 3. current_depth mod symbol_size != 0
        
        boolean containsLocalNode = bucket.contains(getLocalNode().getNodeID());
        
        if (containsLocalNode
                || bucket.isInSmallestSubtree()
                || !bucket.isTooDeep()) {
            return true;
        }
        return false;
    }
    
    /**
     * This method splits the given Bucket into two new Buckets.
     * There are a few conditions in which cases we do split and
     * in which cases we don't.
     */
    protected synchronized boolean split(Bucket bucket) {
        
       if (canSplit(bucket)) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Splitting bucket: " + bucket);
            }
            
            List<Bucket> buckets = bucket.split();
            assert (buckets.size() == 2);
            
            Bucket left = buckets.get(0);
            Bucket right = buckets.get(1);
            
            // The left one replaces the current bucket in the Trie!
            Bucket oldLeft = bucketTrie.put(left.getBucketID(), left);
            assert (oldLeft == bucket);
            
            // The right one is new in the Trie!
            Bucket oldRight = bucketTrie.put(right.getBucketID(), right);
            assert (oldRight == null);
            
            fireSplitBucket(bucket, left, right);
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
    /**
     * This method tries to replace an existing Contact in the given
     * Bucket with the given Contact or tries to add the given Contact
     * to the Bucket's replacement Cache. There are certain conditions
     * in which cases we replace Contacts and if it's not possible we're
     * trying to add the Contact to the replacement cache.
     */
    protected synchronized void replaceContactInBucket(Bucket bucket, Contact node) {
        
        if (node.isAlive() && isOkayToAdd(bucket, node)) {
            Contact leastRecentlySeen = bucket.getLeastRecentlySeenActiveContact();
            
            // If all Contacts in the given Bucket have the same time
            // stamp as the local Node then it's possible that the lrs
            // Contact is the local Contact in which case we don't want 
            // to replace the local Contact with the given Contact
            
            // Is the least recently seen node in UNKNOWN or DEAD state OR is the 
            // new Node a priority Node AND the lrs Node is NOT the local Node
            
            if (!isLocalNode(leastRecentlySeen) 
                    && (leastRecentlySeen.isUnknown() 
                            || leastRecentlySeen.isDead() 
                            || (node.getTimeStamp() == Contact.PRIORITY_CONTACT))) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.info("Replacing " + leastRecentlySeen + " with " + node);
                }
                
                boolean  removed = bucket.removeActiveContact(leastRecentlySeen.getNodeID());
                assert (removed == true);
                
                bucket.addActiveContact(node);
                touchBucket(bucket);
                
                fireReplaceContact(bucket, leastRecentlySeen, node);
                
                return;
            }
        }
        
        addContactToBucketCache(bucket, node);
        pingLeastRecentlySeenNode(bucket);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#handleFailure(com.limegroup.mojito.KUID, java.net.SocketAddress)
     */
    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        
        // NodeID might be null if we sent a ping to
        // an unknown Node (i.e. we knew only the
        // address) and the ping failed. 
        if (nodeId == null) {
            return;
        }
        
        // This should never happen -- who knows?!!
        if(nodeId.equals(getLocalNode().getNodeID())) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot handle local Node's errors: " 
                        + ContactUtils.toString(nodeId, address));
            }
            return;
        }
        
        Bucket bucket = bucketTrie.select(nodeId);
        Contact node = bucket.get(nodeId);
        if (node == null) {
            // It's neither a live nor a cached Node
            // in the bucket!
            return;
        }
        
        if (!node.getContactAddress().equals(address)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(node + " address and " + address + " do not match");
            }
            return;
        }
        
        // Ignore failure if we start getting to many disconnections in a row
        if (consecutiveFailures 
                >= RouteTableSettings.MAX_CONSECUTIVE_FAILURES.getValue()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Ignoring node failure as it appears that we are disconnected");
            }
            return;
        }
        consecutiveFailures++;
        
        node.handleFailure();
        if (node.isDead()) {
            
            if (bucket.containsActiveContact(nodeId)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " and replacing it with the MRS Node from Cache");
                }
                
                // Remove a live-dead Contact only if there's something 
                // in the replacement cache or if the Node has too many
                // errors
                
                if (bucket.getCacheSize() > 0) {
                    
                    Contact mrs = null;
                    while((mrs = bucket.getMostRecentlySeenCachedContact()) != null) {
                        boolean removed = bucket.removeCachedContact(mrs.getNodeID());
                        assert (removed == true);
                        
                        if (isOkayToAdd(bucket, mrs)) {
                            removed = bucket.removeActiveContact(nodeId);
                            assert (removed == true);
                            assert (bucket.isActiveFull() == false);
                            
                            bucket.addActiveContact(mrs);
                            fireReplaceContact(bucket, node, mrs);
                            break;
                        }
                    }
                    
                } else if (node.getFailures() 
                            >= RouteTableSettings.MAX_ACCEPT_NODE_FAILURES.getValue()){
                    
                    bucket.removeActiveContact(nodeId);
                    assert (bucket.isActiveFull() == false);
                    
                    fireRemoveContact(bucket, node);
                }
            } else {
                
                // On first glance this might look like as if it is
                // not necessary since we're never contacting cached
                // Contacts but that's not absolutely true. FIND_NODE
                // lookups may return Contacts that are in our cache
                // and if they don't respond we want to remove them...
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " from Cache");
                }
                
                boolean removed = bucket.removeCachedContact(nodeId);
                assert (removed == true);
            }
        }
    }
    
    /**
     * Returns true of it's Okay to add the given Contact to the
     * given Bucket as active Contact. See {@link ClassfulNetworkCounter}
     * for more information!
     */
    protected synchronized boolean isOkayToAdd(Bucket bucket, Contact node) {
        ClassfulNetworkCounter counter = bucket.getClassfulNetworkCounter();
        boolean okay = (counter == null || counter.isOkayToAdd(node));
        
        if (LOG.isTraceEnabled()) {
            if (okay) {
                LOG.trace("It's okay to add " + node + " to " + bucket);
            } else {
                LOG.trace("It's NOT okay to add " + node + " to " + bucket);
            }
        }
        
        return okay;
    }
    
    /**
     * Removes the given Contact from the RouteTable.
     */
    protected synchronized boolean remove(Contact node) {
        return remove(node.getNodeID());
    }
    
    /**
     * Removes the given KUID (Contact with that KUID) 
     * from the RouteTable.
     */
    protected synchronized boolean remove(KUID nodeId) {
        return bucketTrie.select(nodeId).remove(nodeId);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.routing.RouteTable#getBucket(org.limewire.mojito.KUID)
     */
    public synchronized Bucket getBucket(KUID nodeId) {
        return bucketTrie.select(nodeId);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#select(com.limegroup.mojito.KUID)
     */
    public synchronized Contact select(final KUID nodeId) {
        final Contact[] node = new Contact[] { null };
        bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                node[0] = entry.getValue().select(nodeId);
                if (node[0] != null) {
                    return SelectStatus.EXIT;
                }
                return SelectStatus.CONTINUE;
            }
        });
        return node[0];
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#get(com.limegroup.mojito.KUID)
     */
    public synchronized Contact get(KUID nodeId) {
        return bucketTrie.select(nodeId).get(nodeId);
    }
    
    /**
     * Returns 'count' number of Contacts that are nearest (XOR distance)
     * to the given KUID.
     */
    public synchronized Collection<Contact> select(KUID nodeId, int count) {
        return select(nodeId, count, SelectMode.ALL);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.routing.RouteTable#select(org.limewire.mojito.KUID, int, org.limewire.mojito.routing.RouteTable.SelectMode)
     */
    public synchronized Collection<Contact> select(final KUID nodeId, final int count, 
            final SelectMode mode) {
        
        if (count == 0) {
            return Collections.emptyList();
        }
        
        final int maxNodeFailures = RouteTableSettings.MAX_ACCEPT_NODE_FAILURES.getValue();
        final List<Contact> nodes = new ArrayList<Contact>(count);
        bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                
                Collection<Contact> list = null;
                if (mode == SelectMode.ALIVE
                        || mode == SelectMode.ALIVE_WITH_LOCAL) {
                    // Select all Contacts from the Bucket to compensate
                    // the fact that not all of them will be alive. We're
                    // using Bucket.select() instead of Bucket.getActiveContacts()
                    // to get the Contacts sorted by xor distance!
                    list = bucket.select(nodeId, bucket.getActiveSize());
                } else {
                    list = bucket.select(nodeId, count);
                }
                
                for(Contact node : list) {
                    
                    // Exit the loop if done
                    if (nodes.size() >= count) {
                        return SelectStatus.EXIT;
                    }
                    
                    // Ignore all non-alive Contacts if only
                    // active Contacts are requested.
                    // We also ignore the local contact here (see LocalContact.isAlive)
                    // because a node will always have himself in the routing table
                    if (mode == SelectMode.ALIVE && !node.isAlive()) {
                        continue;
                    }
                    
                    if (mode == SelectMode.ALIVE_WITH_LOCAL
                            && !node.isAlive()
                            && !isLocalNode(node)) {
                        continue;
                    }
                    
                    // Ignore all Contacts that are down
                    if (node.isShutdown()) {
                        continue;
                    }
                    
                    if (node.isDead()) {
                        float fact = (maxNodeFailures - node.getFailures()) 
                                        / (float)Math.max(1, maxNodeFailures);
                        
                        if (Math.random() >= fact) {
                            continue;
                        }
                    }
                    
                    nodes.add(node);
                }
                
                return SelectStatus.CONTINUE;
            }
        });
        
        assert (nodes.size() <= count) : "Expected " + count + " or less elements but is " + nodes.size();
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getContacts()
     */
    public synchronized Collection<Contact> getContacts() {
        Collection<Contact> live = getActiveContacts();
        Collection<Contact> cached = getCachedContacts();
        
        List<Contact> nodes = new ArrayList<Contact>(live.size() + cached.size());
        nodes.addAll(live);
        nodes.addAll(cached);
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getActiveContacts()
     */
    public synchronized Collection<Contact> getActiveContacts() {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values()) {
            nodes.addAll(bucket.getActiveContacts());
        }
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getCachedContacts()
     */
    public synchronized Collection<Contact> getCachedContacts() {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values()) {
            nodes.addAll(bucket.getCachedContacts());
        }
        return nodes;
    }
    
    /*
     * If we are bootstrapping, we don't want to refresh the bucket
     * that contains the local node ID, as phase 1 already takes 
     * care of this. Additionally, when we bootstrap, we don't 
     * look at the bucket's timestamp (isRefreshRequired) so 
     * that we randomly fill up our routing table.
     * 
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getRefreshIDs(boolean)
     */
    public synchronized Collection<KUID> getRefreshIDs(final boolean bootstrapping) {
        final KUID nodeId = getLocalNode().getNodeID();
        final List<KUID> randomIds = new ArrayList<KUID>();
        
        bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                
                // Don't refresh the local Bucket if we're bootstrapping
                // since phase one takes already care of it.
                if (bootstrapping && bucket.contains(getLocalNode().getNodeID())) {
                    return SelectStatus.CONTINUE;
                }

                if (bootstrapping || bucket.isRefreshRequired()) {
                    // Select a random ID with this prefix
                    KUID randomId = KUID.createPrefxNodeID(
                            bucket.getBucketID(), bucket.getDepth());
                    
                    if(LOG.isTraceEnabled()) {
                        LOG.trace("Refreshing bucket:" + bucket 
                                + " with random ID: " + randomId);
                    }
                    
                    randomIds.add(randomId);
                    touchBucket(bucket);
                }
                
                return SelectStatus.CONTINUE;
            }
        });
        
        return randomIds;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getBuckets()
     */
    public synchronized Collection<Bucket> getBuckets() {
        return Collections.unmodifiableCollection(bucketTrie.values());
    }
    
    /**
     * Touches the given Bucket (i.e. updates its timeStamp).
     */
    private void touchBucket(Bucket bucket) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
        bucket.touch();
    }
    
    /**
     * Pings the least recently seen active Contact in the given Bucket.
     */
    private void pingLeastRecentlySeenNode(Bucket bucket) {
        Contact lrs = bucket.getLeastRecentlySeenActiveContact();
        if (!isLocalNode(lrs)) {
            ping(lrs, null);
        }
    }
    
    /**
     * Pings the given Contact and adds the given DHTEventListener to
     * the DHTFuture if it's not null.
     */
    private void ping(Contact node, DHTFutureAdapter<PingResult> listener) {
        ContactPinger pinger = this.pinger;
        if (pinger != null) {
            pinger.ping(node, listener);
        } else {
            handleFailure(node.getNodeID(), node.getContactAddress());
            
            if (listener != null) {
                ExecutionException exception = new ExecutionException(
                        new DHTTimeoutException(node.getNodeID(), 
                                node.getContactAddress(), null, 0L));
                
                FutureEvent<PingResult> event 
                    = FutureEvent.createException(exception);
                listener.handleEvent(event);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getLocalNode()
     */
    public Contact getLocalNode() {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        return localNode;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#isLocalNode(com.limegroup.mojito.Contact)
     */
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#size()
     */
    public synchronized int size() {
        return getActiveContacts().size() + getCachedContacts().size();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#clear()
     */
    public synchronized void clear() {
        bucketTrie.clear();
        fireClear();
        init();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.routing.RouteTable#purge(long)
     */
    public synchronized void purge(long elapsedTimeSinceLastContact) {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        
        if (elapsedTimeSinceLastContact == -1L) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        for (Contact node : getActiveContacts()) {
            if (isLocalNode(node)) {
                continue;
            }
            
            if ((currentTime - node.getTimeStamp()) < elapsedTimeSinceLastContact) {
                continue;
            }
            
            remove(node);
        }
        
        for (Contact node : getCachedContacts()) {
            if ((currentTime - node.getTimeStamp()) < elapsedTimeSinceLastContact) {
                continue;
            }
            
            remove(node);
        }
        
        mergeBuckets();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.routing.RouteTable#purge(org.limewire.mojito.routing.RouteTable.PurgeMode, org.limewire.mojito.routing.RouteTable.PurgeMode[])
     */
    public synchronized void purge(PurgeMode first, PurgeMode... rest) {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        
        EnumSet<PurgeMode> modes = EnumSet.of(first, rest);
        
        if (modes.contains(PurgeMode.DROP_CACHE)) {
            dropCache();
        }
        
        if (modes.contains(PurgeMode.PURGE_CONTACTS)) {
            purgeContacts();
        }
        
        if (modes.contains(PurgeMode.MERGE_BUCKETS)) {
            mergeBuckets();
        }
        
        if (modes.contains(PurgeMode.STATE_TO_UNKNOWN)) {
            changeStateToUnknown(getActiveContacts());
            changeStateToUnknown(getCachedContacts());
        }
    }
    
    private synchronized void dropCache() {
        for (Contact node : getCachedContacts()) {
            remove(node);
        }
    }
    
    private synchronized void purgeContacts() {
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                bucket.purge();
                return SelectStatus.CONTINUE;
            }
        });
    }
    
    private synchronized void mergeBuckets() {
        // Get the active Contacts
        Collection<Contact> activeNodes = getActiveContacts();
        activeNodes = ContactUtils.sortAliveToFailed(activeNodes);
        
        // Get the cached Contacts
        Collection<Contact> cachedNodes = getCachedContacts();
        cachedNodes = ContactUtils.sort(cachedNodes);
        
        // We count on the fact that getActiveContacts() and 
        // getCachedContacts() return copies!
        clear();
        
        // Remove the local Node from the List. Shouldn't fail as 
        // activeNodes is a copy!
        boolean removed = activeNodes.remove(localNode);
        assert (removed);
        
        // Re-add the active Contacts
        for (Contact node : activeNodes) {
            add(node);
        }
        
        // And re-add the cached Contacts
        for (Contact node : cachedNodes) {
            add(node);
        }
    }

    private synchronized void changeStateToUnknown(Collection<Contact> nodes) {
        for (Contact node : nodes) {
            node.unknown();
        }
    }
    
    protected void fireActiveContactAdded(Bucket bucket, Contact node) {
        fireRouteTableEvent(bucket, null, null, null, node, EventType.ADD_ACTIVE_CONTACT);
    }
    
    protected void fireCachedContactAdded(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.ADD_CACHED_CONTACT);
    }
    
    protected void fireContactUpdate(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.UPDATE_CONTACT);
    }
    
    protected void fireReplaceContact(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.REPLACE_CONTACT);
    }
    
    protected void fireRemoveContact(Bucket bucket, Contact node) {
        fireRouteTableEvent(bucket, null, null, null, node, EventType.REMOVE_CONTACT);
    }
    
    protected void fireContactCheck(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.CONTACT_CHECK);
    }
    
    protected void fireSplitBucket(Bucket bucket, Bucket left, Bucket right) {
        fireRouteTableEvent(bucket, left, right, null, null, EventType.SPLIT_BUCKET);
    }
    
    protected void fireClear() {
        fireRouteTableEvent(null, null, null, null, null, EventType.CLEAR);
    }
    
    protected void fireRouteTableEvent(Bucket bucket, Bucket left, Bucket right, 
            Contact existing, Contact node, EventType type) {
        
        if (listeners.isEmpty()) {
            return;
        }
        
        final RouteTableEvent event = new RouteTableEvent(
                this, bucket, left, right, existing, node, type);
        
        Runnable r = new Runnable() {
            public void run() {
                for (RouteTableListener listener : listeners) {
                    listener.handleRouteTableEvent(event);
                }
            }
        };
        
        DHTExecutorService e = notifier;
        if (e != null) 
            e.executeSequentially(r);
        else
            r.run();
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Local: ").append(getLocalNode()).append("\n");
        
        int alive = 0;
        int dead = 0;
        int down = 0;
        int unknown = 0;
        
        for(Bucket bucket : getBuckets()) {
            buffer.append(bucket).append("\n");
            
            for (Contact node : bucket.getActiveContacts()) {
                if (node.isShutdown()) {
                    down++;
                }
                
                if (node.isAlive()) {
                    alive++;
                } else if (node.isDead()) {
                    dead++;
                } else {
                    unknown++;
                }
            }
            
            for (Contact node : bucket.getCachedContacts()) {
                if (node.isShutdown()) {
                    down++;
                }
                
                if (node.isAlive()) {
                    alive++;
                } else if (node.isDead()) {
                    dead++;
                } else {
                    unknown++;
                }
            }
        }
        
        buffer.append("Total Buckets: ").append(bucketTrie.size()).append("\n");
        buffer.append("Total Active Contacts: ").append(getActiveContacts().size()).append("\n");
        buffer.append("Total Cached Contacts: ").append(getCachedContacts().size()).append("\n");
        buffer.append("Total Alive Contacts: ").append(alive).append("\n");
        buffer.append("Total Dead Contacts: ").append(dead).append("\n");
        buffer.append("Total Down Contacts: ").append(down).append("\n");
        buffer.append("Total Unknown Contacts: ").append(unknown).append("\n");
        return buffer.toString();
    }
}
