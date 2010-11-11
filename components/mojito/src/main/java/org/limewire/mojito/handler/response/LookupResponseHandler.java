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

package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.Trie;
import org.limewire.collection.TrieUtils;
import org.limewire.collection.Trie.Cursor;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.SecurityTokenProvider;
import org.limewire.mojito.result.LookupResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.ContactsScrubber;
import org.limewire.mojito.util.EntryImpl;
import org.limewire.security.SecurityToken;


/**
 * The LookupResponseHandler class handles the entire Kademlia 
 * lookup process. Subclasses implement lookup specific features
 * like the type of the lookup (FIND_NODE and FIND_VALUE) or
 * different lookup termination conditions.
 * <p>
 * Think of the LookupResponseHandler as some kind of State-Machine.
 */
public abstract class LookupResponseHandler<V extends LookupResult> extends AbstractResponseHandler<V> {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /** The ID we're looking for. */
    protected final KUID lookupId;
    
    /** The ID which is furthest away from the lookupId. */
    private final KUID furthestId;
    
    /** Set of queried KUIDs. */
    protected final Set<KUID> queried = new LinkedHashSet<KUID>();
    
    /** Trie of Contacts we're going to query. */
    protected final Trie<KUID, Contact> toQuery 
        = new PatriciaTrie<KUID, Contact>(KUID.KEY_ANALYZER);
    
    /** Trie of Contacts that did respond. */
    protected final Trie<KUID, Entry<Contact, SecurityToken>> responsePath 
        = new PatriciaTrie<KUID, Entry<Contact, SecurityToken>>(KUID.KEY_ANALYZER);
    
    /** A Map we're using to count the number of hops. */
    private final Map<KUID, Integer> hopMap = new HashMap<KUID, Integer>();
    
    /** The k-closest IDs we selected to start the lookup. */
    private final Set<KUID> routeTableNodes = new LinkedHashSet<KUID>();
    
    /** A Set of Contacts that have the same Node ID as the local Node. */
    private final Set<Contact> forcedContacts = new LinkedHashSet<Contact>();
    
    /** Collection of Contacts that collide with our Node ID. */
    protected final Collection<Contact> collisions = new LinkedHashSet<Contact>();

    /** The number of currently active (parallel) searches. */
    private int activeSearches = 0;
    
    /** The current hop. */
    private int currentHop = 0;
    
    /** The expected result set size (aka K). */
    private int resultSetSize;
    
    /** The number of parallel lookups.*/
    private int parellelism;

    /**
     * Whether or not this lookup tries to return k live nodes.
     * This will increase the size of the set of hosts to query.
     */
    private boolean selectAliveNodesOnly = false;
    
    /** The time when this lookup started. */
    private long startTime = -1L;
    
    /** The number of Nodes from our RouteTable that failed. */
    private int routeTableFailureCount = 0;
    
    /** The total number of failed lookups. */
    private int totalFailures = 0;
    
    /** 
     * Whether or not the (k+1)-closest Contact should be removed 
     * from the response Set. 
     */
    private boolean deleteFurthest = true;
    
    /**
     * Creates a new LookupResponseHandler.
     */
    protected LookupResponseHandler(Context context, KUID lookupId) {
        super(context);
        
        this.lookupId = lookupId;
        this.furthestId = lookupId.invert();
        
        setMaxErrors(0); // Don't retry on timeout - takes too long!
        setParallelism(-1); // Default number of parallel lookups
        setResultSetSize(-1); // Default result set size
        setDeleteFurthest(LookupSettings.DELETE_FURTHEST_CONTACT.getValue());
    }
    
    /**
     * Returns the Key we're looking for.
     */
    public KUID getLookupID() {
        return lookupId;
    }
    
    /**
     * Sets the result set size.
     */
    public void setResultSetSize(int resultSetSize) {
        if (resultSetSize < 0) {
            this.resultSetSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        } else if (resultSetSize > 0) {
            this.resultSetSize = resultSetSize;
        } else {
            throw new IllegalArgumentException("resultSetSize=" + resultSetSize);
        }
    }
    
    /**
     * Returns the result set size.
     */
    public int getResultSetSize() {
        return resultSetSize;
    }
    
    /**
     * Sets the number of parallel lookups this handler
     * should maintain.
     */
    public void setParallelism(int parellelism) {
        if (parellelism < 0) {
            this.parellelism = getDefaultParallelism();
        } else if (parellelism > 0) {
            this.parellelism = parellelism;
        } else {
            throw new IllegalArgumentException("parellelism=" + parellelism);
        }
    }
    
    /**
     * Returns the default number of parallel lookups this
     * handler maintains .
     */
    protected abstract int getDefaultParallelism();
    
    /**
     * Returns the number of parallel lookups this handler
     * maintains.
     */
    public int getParallelism() {
        return parellelism;
    }
    
    /**
     * Adds the given Contact to the collection of Contacts
     * that must be contacted during the lookup.
     */
    public void addForcedContact(Contact node) {
        forcedContacts.add(node);
    }
    
    /**
     * Returns an unmodifiable collection of Contacts
     * that must be contacted during the lookup.
     */
    public Collection<Contact> getForcedContacts() {
        return Collections.unmodifiableSet(forcedContacts);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.AbstractResponseHandler#getElapsedTime()
     */
    @Override
    public long getElapsedTime() {
        if (startTime > 0L) {
            return System.currentTimeMillis() - startTime;
        }
        return -1L;
    }
    
    /**
     * Returns the number of Nodes from our RouteTable that failed
     * to respond.
     */
    public int getRouteTableFailureCount() {
        return routeTableFailureCount;
    }
    
    /**
     * Returns true if the lookup has timed out.
     */
    protected abstract boolean isTimeout(long time);
        
    /**
     * Sets whether or not only alive Contacts from the local 
     * RouteTable should be used as the lookup start Set. The
     * default is false as lookups are an important tool to
     * refresh the local RouteTable but in some cases it's
     * useful to use 'guaranteed' alive Contacts.
     */
    public void setSelectAliveNodesOnly(boolean selectAliveNodesOnly) {
        this.selectAliveNodesOnly = selectAliveNodesOnly;
    }
    
    /**
     * Returns whether or not only alive Contacts should be
     * selected (from the RouteTable) for the first hop.
     */
    public boolean isSelectAliveNodesOnly() {
        return selectAliveNodesOnly;
    }
    
    /**
     * Sets whether or not the furthest of the (k+1)-closest Contacts
     * that did respond should be deleted from the response Set.
     * This is primarily a memory optimization as we're only interested
     * in the k-closest Contacts.
     * <p>
     * For caching we need the lookup path though (that means we'd set
     * this to false).
     */
    public void setDeleteFurthest(boolean deleteFurthest) {
        this.deleteFurthest = deleteFurthest;
    }
    
    /**
     * Returns whether or not the furthest of the (k+1)-closest
     * Contacts will be removed from the response Set.
     */
    public boolean isDeleteFurthest() {
        return deleteFurthest;
    }
    
    @Override
    protected void start() throws DHTException {
        
        // Get the closest Contacts from our RouteTable 
        // and add them to the yet-to-be queried list.
        Collection<Contact> nodes = null;
        if (isSelectAliveNodesOnly()) {
            // Select twice as many Contacts which should guarantee that
            // we've k-closest Nodes at the end of the lookup
            nodes = context.getRouteTable().select(lookupId, 2 * getResultSetSize(), SelectMode.ALIVE);
        } else {
            nodes = context.getRouteTable().select(lookupId, getResultSetSize(), SelectMode.ALL);
        }
        
        // Add the Nodes to the yet-to-be queried List and remember
        // the IDs of the Nodes we selected from our RouteTable
        for(Contact node : nodes) {
            addYetToBeQueried(node, currentHop+1);
            routeTableNodes.add(node.getNodeID());
        }
        
        // Mark the local node as queried (we did a lookup on our own RouteTable)
        addToResponsePath(context.getLocalNode(), null);
        markAsQueried(context.getLocalNode());
        
        // Get the first round of alpha nodes and send them requests
        List<Contact> alphaList = new ArrayList<Contact>(
        		getContactsToQuery(lookupId, getParallelism()));
        
        // Optimize the first lookup step if we have enough parallel lookup slots
        if(alphaList.size() >= 3) {
            // Get the MRS node of the k closest nodes
            nodes = ContactUtils.sort(nodes);
            Contact mrs = ContactUtils.getMostRecentlySeen(nodes);
            if(!alphaList.contains(mrs) && !context.isLocalNode(mrs)) {
                // If list is full, remove last element and add the MRS node
                if (alphaList.size() >= getParallelism()) {
                    alphaList.remove(alphaList.size()-1);
                }
                alphaList.add(mrs);
            }
        }
        
        // Make sure the forced Contacts are in the alpha list
        for (Contact forced : forcedContacts) {
            if (!alphaList.contains(forced)) {
                alphaList.add(0, forced);
                hopMap.put(forced.getNodeID(), currentHop+1);
                
                int last = alphaList.size()-1;
                if (alphaList.size() > getParallelism() 
                        && !forcedContacts.contains(alphaList.get(last))) {
                    alphaList.remove(last);
                }
            }
        }
        
        // Go Go Go!
        startTime = System.currentTimeMillis();  
        for(Contact node : alphaList) {
            try {
                lookup(node);
            } catch (IOException err) {
                throw new DHTException(err);
            }
        }
        
        finishLookupIfDone();
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        decrementActiveSearches();
        Contact contact = message.getContact();
        
        Integer hop = hopMap.remove(contact.getNodeID());
        assert (hop != null);
        
        currentHop = hop.intValue();
        
        if (nextStep(message)) {
            nextLookupStep();
        }
        
        finishLookupIfDone();
    }
    
    /**
     * @return if the next step in the lookup should be performed
     */
    protected abstract boolean nextStep(ResponseMessage message) throws IOException;
    
    /**
     * Handles a node response message.  This type of message is handled in the same
     * way regardless of the type of lookup.
     */
    protected final boolean handleNodeResponse(FindNodeResponse response) {
        Contact sender = response.getContact();
        Collection<? extends Contact> nodes = response.getNodes();
        
        // Nodes that are currently bootstrapping return
        // an empty Collection of Contacts! 
        if (nodes.isEmpty()) {
            if (LookupSettings.ACCEPT_EMPTY_FIND_NODE_RESPONSES.getValue()) {
                addToResponsePath(response);
            }
            return true;
        }
        
        ContactsScrubber scrubber = ContactsScrubber.scrub(
                context, sender, nodes, 
                LookupSettings.CONTACTS_SCRUBBER_REQUIRED_RATIO.getValue());
        
        if (scrubber.isValidResponse()) {
            for(Contact node : scrubber.getScrubbed()) {
                
                if (!isQueried(node) 
                        && !isYetToBeQueried(node)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adding " + node + " to the yet-to-be queried list");
                    }
                    
                    addYetToBeQueried(node, currentHop+1);
                    
                    // Add them to the routing table as not alive
                    // contacts. We're likely going to add them
                    // anyways!
                    assert (node.isAlive() == false);
                    context.getRouteTable().add(node);
                }
            }
            
            collisions.addAll(scrubber.getCollisions());
            addToResponsePath(response);
        }
        
        return true;
    }
    
    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException { 
        
        decrementActiveSearches();
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactUtils.toString(nodeId, dst) 
                    + " did not respond to our " + message);
        }
        
        Integer hop = hopMap.remove(nodeId);
        assert (hop != null);
        
        if (routeTableNodes.contains(nodeId)) {
            routeTableFailureCount++;
        }
        
        totalFailures++;
        
        currentHop = hop.intValue();
        nextLookupStep();
        finishLookupIfDone();
    }

    @Override
    protected void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        if (e instanceof SocketException && hasActiveSearches()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (hasActiveSearches() == false) {
                    setException(new DHTException(err));
                }
            }
        } else {
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }
    
    /**
     * This method is the heart of the lookup process. It selects 
     * Contacts from the toQuery Trie and sends them lookup requests
     * until we find a Node with the given ID, the lookup times out
     * or there are no Contacts left to query. 
     */
    protected void nextLookupStep() throws IOException {
        
        long totalTime = getElapsedTime();
        
        if (isTimeout(totalTime)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookupId + " terminates after "
                        + currentHop + " hops and " + totalTime + "ms due to timeout.");
            }

            killActiveSearches();
            // finishLookup() gets called if activeSearches is zero!
            return;
        }
        
        if (!hasActiveSearches()) {
            
            // Finish if nothing left to query...
            if (!hasContactsToQuery()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookupId + " terminates after "
                            + currentHop + " hops and " + totalTime + "ms. No contacts left to query.");
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
                
            // ...or if we found the target node
            // It is important to have finished all the active searches before
            // probing for this condition, because in the case of a bootstrap lookup
            // we are actually updating the routing tables of the nodes we contact.
            } else if (!context.isLocalNodeID(lookupId) 
                    && responsePath.containsKey(lookupId)) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookupId + " terminates after "
                            + currentHop + " hops. Found target ID!");
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
            }
        }
        
        if (responsePath.size() >= getResultSetSize()) {
            KUID worst = responsePath.select(furthestId).getKey().getNodeID();
            
            KUID best = null;            
            if (!toQuery.isEmpty()) {
                best = toQuery.select(lookupId).getNodeID();
            }
            
            if (best == null || worst.isNearerTo(lookupId, best)) {
                if (!hasActiveSearches()) {
                    if (LOG.isTraceEnabled()) {
                        Contact bestResponse = responsePath.select(lookupId).getKey();
                        LOG.trace("Lookup for " + lookupId + " terminates after "
                                + currentHop + " hops, " + totalTime + "ms and " + queried.size() 
                                + " queried Nodes with " + bestResponse + " as best match");
                    }
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
            }
        }
        
        int numLookups = getParallelism() - getActiveSearches();
        if (numLookups > 0) {
            Collection<Contact> toQueryList = getContactsToQuery(lookupId, numLookups);
            for (Contact node : toQueryList) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a find request for " + lookupId);
                }
                
                try {
                    lookup(node);
                } catch (SocketException err) {
                    LOG.error("A SocketException occurred", err);
                }
            }
        }
    }
    
    /**
     * Sends a lookup request to the given Contact.
     */
    protected boolean lookup(Contact node) throws IOException {
        LookupRequest request = createLookupRequest(node);
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending " + node + " a " + request);
        }
        
        markAsQueried(node);
        boolean requestWasSent = context.getMessageDispatcher().send(node, request, this);
        
        if (requestWasSent) {
            incrementActiveSearches();
        }
        return requestWasSent;
    }
    
    /**
     * Creates and returns a LookupRequest message.
     */
    protected abstract LookupRequest createLookupRequest(Contact node);
    
    /**
     * Calls finishLookup() if the lookup isn't already
     * finished and there are no parallel searches active.
     */
    private void finishLookupIfDone() {
        if (!isDone() && !isCancelled() && !hasActiveSearches()) {
            finishLookup();
        }
    }
    
    /**
     * Called when the lookup finishes.
     */
    protected abstract void finishLookup();
    
    /**
     * Increments the 'activeSearches' counter by one.
     */
    protected void incrementActiveSearches() {
        activeSearches++;
    }
    
    /**
     * Decrements the 'activeSearches' counter by one.
     */
    protected void decrementActiveSearches() {
        if (activeSearches == 0) {
            if (LOG.isErrorEnabled()) {
                LOG.error("ActiveSearches counter is already 0");
            }
            return;
        }
        
        activeSearches--;
    }
    
    /**
     * Sets the 'activeSearches' counter to zero
     */
    protected void killActiveSearches() {
        activeSearches = 0;
    }
    
    /**
     * Returns the number of current number of active.
     * searches
     */
    protected int getActiveSearches() {
        return activeSearches;
    }
    
    /**
     * Returns whether or not there are currently any
     * searches active.
     */
    protected boolean hasActiveSearches() {
        return getActiveSearches() > 0;
    }
    
    /** 
     * Returns whether or not the Node has been queried. 
     */
    protected boolean isQueried(Contact node) {
        return queried.contains(node.getNodeID());            
    }
    
    /** 
     * Marks the Node as queried. 
     */
    protected void markAsQueried(Contact node) {
        queried.add(node.getNodeID());
        toQuery.remove(node.getNodeID());
    }
    
    /** 
     * Returns whether or not the Node is in the to-query Trie. 
     */
    protected boolean isYetToBeQueried(Contact node) {
        return toQuery.containsKey(node.getNodeID());            
    }
    
    /**
     * Returns true if there are more Contact to query.
     */
    protected boolean hasContactsToQuery() {
    	return !toQuery.isEmpty();
    }

    /**
     * Returns the next Contacts for the lookup.
     */
    protected Collection<Contact> getContactsToQuery(KUID lookupId, int count) {
    	return TrieUtils.select(toQuery, lookupId, count);
    }
    
    /** 
     * Adds the Node to the to-query Trie. 
     */
    protected boolean addYetToBeQueried(Contact node, int hop) {
            
        if (isQueried(node)) {
            return false;
        }
        
        KUID nodeId = node.getNodeID();
        if (context.isLocalNodeID(nodeId)
                || context.isLocalContactAddress(node.getContactAddress())) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has either the same NodeID or contact"
                        + " address as the local Node " + context.getLocalNode());
            }
            return false;
        }
        
        toQuery.put(nodeId, node);
        hopMap.put(nodeId, hop);
        return true;
    }
    
    /**
     * Adds the response to the lookup/response Path.
     */
    protected void addToResponsePath(ResponseMessage response) {
        Contact sender = response.getContact();
        SecurityToken securityToken = null;
        if (response instanceof SecurityTokenProvider) {
            securityToken = ((SecurityTokenProvider)response).getSecurityToken();
        }
        addToResponsePath(sender, securityToken);
    }
    
    /** 
     * Adds the Contact-SecurityToken Tuple to the response Trie.
     */
    protected void addToResponsePath(Contact node, SecurityToken securityToken) {
        
        Entry<Contact,SecurityToken> entry 
            = new EntryImpl<Contact,SecurityToken>(node, securityToken, true);
        
        responsePath.put(node.getNodeID(), entry);
        
        // We're only interested in the k-closest
        // Contacts so remove the worst ones
        if (isDeleteFurthest() && responsePath.size() > getResultSetSize()) {
            Contact worst = responsePath.select(furthestId).getKey();
            responsePath.remove(worst.getNodeID());
        }
    }
    
    /**
     * Returns the lookup/response Path.
     */
    protected Map<Contact, SecurityToken> getPath() {
        return getContacts(responsePath.size());
    }
    
    /**
     * Returns the k-closest Contacts sorted by their closeness
     * to the given lookup key.
     */
    protected Map<Contact, SecurityToken> getNearestContacts() {
        return getContacts(getResultSetSize());
    }
    
    /**
     * Returns count number of Contacts sorted by their closeness
     * to the given lookup key.
     */
    protected Map<Contact, SecurityToken> getContacts(int count) {
        if (count < 0) {
            count = responsePath.size();
        }
        
        final int maxCount = count;
        
        // Use a LinkedHashMap which preserves the insertion order...
        final Map<Contact, SecurityToken> nearest = new LinkedHashMap<Contact, SecurityToken>();
        
        responsePath.select(lookupId, new Cursor<KUID, Entry<Contact,SecurityToken>>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Entry<Contact, SecurityToken>> entry) {
                Entry<Contact, SecurityToken> e = entry.getValue();
                nearest.put(e.getKey(), e.getValue());
                
                if (nearest.size() < maxCount) {
                    return SelectStatus.CONTINUE;
                }
                
                return SelectStatus.EXIT;
            }
        });
        
        return nearest;
    }
    
    /**
     * Returns all queried KUIDs.
     */
    protected Set<KUID> getQueried() {
        return queried;
    }
    
    /**
     * Returns the current hop.
     */
    public int getCurrentHop() {
        return currentHop;
    }
    
    @Override
    public String toString() {
        long time = getElapsedTime();
        boolean timeout = isTimeout(time);
        int activeSearches = getActiveSearches();
        
        return ", lookup: " + lookupId 
            + ", time: " + time 
            + ", timeout: " + timeout 
            + ", activeSearches: " + activeSearches;
    }
}
