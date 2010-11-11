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
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.result.PingResult;


/**
 * Defines the interface for a <a href="http://en.wikipedia.org/wiki/Routing_table">
 * RouteTable</a>.
 */
public interface RouteTable extends Serializable {
    
    /**
     * Selection mode for the RouteTable's select() operation.
     */
    public static enum SelectMode {
        /**
         * Selects all Contacts.
         */
        ALL,
        
        /**
         * Selects only alive Contacts.
         */
        ALIVE,
        
        /**
         * Selects only alive and the local Contacts.
         */
        ALIVE_WITH_LOCAL;
    }
    
    /**
     * Rebuild mode for the RouteTable's purge() operation.
     */
    public static enum PurgeMode {
        /**
         * Drops all Contacts in the replacement cache.
         */
        DROP_CACHE,
        
        /**
         * Deletes all non-alive Contacts from the active
         * RouteTable and fill up the new slots with alive
         * Contacts from the replacement cache.
         */
        PURGE_CONTACTS,
        
        /**
         * Merges all Buckets by essentially rebuilding the
         * entire RouteTable.
         */
        MERGE_BUCKETS,
        
        /**
         * Marks all Contacts as unknown.
         */
        STATE_TO_UNKNOWN,
    }
    
    /**
     * Adds a new Contact or if it's already in the RouteTable updates 
     * its contact information.
     * 
     * @param node the Contact we would like to add
     */
    public void add(Contact node);
    
    /**
     * Returns a Contact from the local RoutingTable if such Contact exists 
     * and null if it doesn't.
     */
    public Contact get(KUID nodeId);
    
    /**
     * Selects the best matching Contact for the provided KUID.
     * This method will guaranteed return a non-null value if the
     * RoutingTable is not empty.
     */
    public Contact select(KUID nodeId);
    
    /**
     * Selects the best matching k Contacts for the provided KUID. The returned
     * Contacts are sorted by their closeness to the lookup Key from closest to
     * least closest Contact. Use {@link org.limewire.mojito.util.BucketUtils#sort(List)}
     * to sort the list from least-recently-seen to most-recently-seen Contact.
     * 
     * @param nodeId the lookup KUID
     * @param count the number of Contact (maybe less if RoutingTable has less than 'count' entries!)
     * @param mode whether or not only alive Contacts should be in the result set
     * @return list of Contacts sorted by closeness
     */
    public Collection<Contact> select(KUID nodeId, int count, SelectMode mode);
    
    /**
     * Notifies the RoutingTable that the Contact with the provided
     * KUID has failed to answer to a request.
     */
    public void handleFailure(KUID nodeId, SocketAddress address);
    
    /**
     * Returns all Contacts as List.
     */
    public Collection<Contact> getContacts();
    
    /**
     * Returns Contacts that are actively used for routing.
     */
    public Collection<Contact> getActiveContacts();
    
    /**
     * Returns cached Contacts that are in the replacement cache.
     */
    public Collection<Contact> getCachedContacts();
    
    /**
     * Returns a Bucket that is nearest (XOR distance) 
     * to the given KUID.
     */
    public Bucket getBucket(KUID nodeId);
    
    /**
     * Returns all Buckets as an Collection.
     */
    public Collection<Bucket> getBuckets();
    
    /**
     * Returns a List of KUIDs that need to be looked up in order
     * to refresh (or bootstrap) the RouteTable.
     * 
     * @param bootstrapping whether or not this refresh is done during bootstrap
     */
    public Collection<KUID> getRefreshIDs(boolean bootstrapping);
    
    /**
     * Clears all elements from the RoutingTable.
     */
    public void clear();
    
    /**
     * Purges all Contacts from the RouteTable whose time stamp 
     * is older than the given time and merges all Buckets.
     */
    public void purge(long elapsedTimeSinceLastContact);
    
    /**
     * Purges/Rebuilds the RouteTable based on the given Modes.
     */
    public void purge(PurgeMode first, PurgeMode... rest);
    
    /**
     * Returns the number of live and cached Contacts in the Route Table.
     */
    public int size();
    
    /**
     * Returns whether or not the given Contact is the local
     * Node.
     */
    public boolean isLocalNode(Contact node);
    
    /**
     * Returns the local Node.
     */
    public Contact getLocalNode();
    
    /**
     * Sets the RouteTable PingCallback.
     */
    public void setContactPinger(ContactPinger pinger);
    
    /**
     * Sets the executor on which to notify for route table events.
     */
    public void setNotifier(DHTExecutorService executor);
    
    /**
     * Adds a RouteTableListener.
     * 
     * @param l the RouteTableListener to add
     */
    public void addRouteTableListener(RouteTableListener l);
    
    /**
     * Removes a RouteTableListener.
     * 
     * @param l the RouteTableListener to remove
     */
    public void removeRouteTableListener(RouteTableListener l);
    
    /**
     * An interface used by the RouteTable to access 
     * external resources.
     */
    public static interface ContactPinger {
        
        /** Sends a PING to the given Node */
        public void ping(Contact node, DHTFutureAdapter<PingResult> listener);
    }
    
    /**
     * The interface to receive RouteTable events.
     */
    public static interface RouteTableListener {
        
        /**
         * Invoked when an event occurs.
         * 
         * @param event the event that occurred
         */
        public void handleRouteTableEvent(RouteTableEvent event);
    }
    
    /**
     * Created and fired for various RouteTable events.
     */
    public static class RouteTableEvent {
        
        /**
         * The types of events that may occur.
         */
        public static enum EventType {
            
            /** A Contact was added to the RouteTable. */
            ADD_ACTIVE_CONTACT,
            
            /** A Contact was added to the replacement cache. */
            ADD_CACHED_CONTACT,
            
            /** A Contact was replaced. */
            REPLACE_CONTACT,
            
            /** A Contact was updated. */
            UPDATE_CONTACT,
            
            /** A Contact was removed. */
            REMOVE_CONTACT,
            
            /** A Contact was contacted to check if it's alive. */
            CONTACT_CHECK,
            
            /** A Bucket was split. */
            SPLIT_BUCKET,
            
            /** The RouteTable was cleared.*/
            CLEAR;
        }
        
        private final RouteTable routeTable;
        
        private final Bucket bucket;
        
        private final Bucket left;
        
        private final Bucket right;
        
        private final Contact existing;
        
        private final Contact node;
        
        private final EventType type;
        
        private final long timeStamp = System.currentTimeMillis();
        
        public RouteTableEvent(RouteTable routeTable, 
                Bucket bucket, Bucket left, Bucket right,
                Contact existing, Contact node, EventType type) {
            this.routeTable = routeTable;
            this.bucket = bucket;
            this.left = left;
            this.right = right;
            this.existing = existing;
            this.node = node;
            this.type = type;
        }
        
        /**
         * Returns the RouteTable which triggered the Event.
         */
        public RouteTable getRouteTable() {
            return routeTable;
        }
        
        /**
         * The Bucket where an Event occurred.
         */
        public Bucket getBucket() {
            return bucket;
        }
        
        /**
         * Returns the new left hand Bucket if this is
         * a SPLIT_BUCKET event and null otherwise.
         */
        public Bucket getLeftBucket() {
            return left;
        }
        
        /**
         * Returns the new right hand Bucket if this is
         * a SPLIT_BUCKET event and null otherwise.
         */
        public Bucket getRightBucket() {
            return right;
        }
        
        /**
         * Returns the existing Contact that was updated.
         * This might be null in certain cases!
         */
        public Contact getExistingContact() {
            return existing;
        }
        
        /**
         * Returns the Contact that was added to the RouteTable.
         */
        public Contact getContact() {
            return node;
        }
        
        /**
         * Returns the type of the Event.
         */
        public EventType getEventType() {
            return type;
        }
        
        /**
         * Returns the time when this Event occurred.
         */
        public long getTimeStamp() {
            return timeStamp;
        }
    }
}
