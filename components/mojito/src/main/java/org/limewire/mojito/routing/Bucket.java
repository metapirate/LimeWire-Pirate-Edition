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
import java.util.Collection;
import java.util.List;

import org.limewire.mojito.KUID;


/**
 * An interface for Buckets.
 */
public interface Bucket extends Serializable {
    
    /**
     * Returns the Bucket KUID.
     */
    public KUID getBucketID();

    /**
     * Returns the depth of the Bucket in the Trie.
     */
    public int getDepth();

    /**
     * Set the time stamp of the Bucket to 'now'.
     */
    public void touch();

    /**
     * Returns the time stamp when this Bucket was refreshed
     * last time.
     */
    public long getTimeStamp();

    /**
     * Adds the Contact as an active contact.
     */
    public void addActiveContact(Contact node);

    /**
     * Add the Contact to the replacement cache.
     */
    public Contact addCachedContact(Contact node);

    /**
     * Updates the Contact in this bucket.
     */
    public Contact updateContact(Contact node);
    
    /**
     * Returns the Contact that has the provided KUID.
     */
    public Contact get(KUID nodeId);

    /**
     * Returns the Contact that has the provided KUID.
     */
    public Contact getActiveContact(KUID nodeId);

    /**
     * Returns the Contact that has the provided KUID.
     */
    public Contact getCachedContact(KUID nodeId);

    /**
     * Returns the best matching Contact for the provided KUID.
     */
    public Contact select(KUID nodeId);

    /**
     * Returns the 'count' best matching Contacts for the provided KUID.
     */
    public Collection<Contact> select(KUID nodeId, int count);

    /**
     * Removes the Contact that has the provided KUID.
     */
    public boolean remove(KUID nodeId);

    /**
     * Removes the Contact that has the provided KUID.
     */
    public boolean removeActiveContact(KUID nodeId);

    /**
     * Removes the Contact that has the provided KUID.
     */
    public boolean removeCachedContact(KUID nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with this KUID.
     */
    public boolean contains(KUID nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with this KUID.
     */
    public boolean containsActiveContact(KUID nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with this KUID.
     */
    public boolean containsCachedContact(KUID nodeId);

    /**
     * Returns whether or not this Bucket is full.
     */
    public boolean isActiveFull();

    /**
     * Returns whether or not this Bucket is full.
     */
    public boolean isCacheFull();

    /**
     * Returns whether or not this Bucket is too deep in the Trie.
     */
    public boolean isTooDeep();

    /**
     * Returns whether or not this Bucket is in the smallest subtree.
     */
    public boolean isInSmallestSubtree();

    /**
     * Returns all active Contacts as List.
     */
    public Collection<Contact> getActiveContacts();

    /**
     * Returns all cached Contacts as List.
     */
    public Collection<Contact> getCachedContacts();

    /**
     * Returns the least recently seen active Contact.
     */
    public Contact getLeastRecentlySeenActiveContact();

    /**
     * Returns the most recently seen active Contact.
     */
    public Contact getMostRecentlySeenActiveContact();

    /**
     * Returns the least recently seen cached Contact.
     */
    public Contact getLeastRecentlySeenCachedContact();

    /**
     * Returns the most recently seen cached Contact.
     */
    public Contact getMostRecentlySeenCachedContact();
    
    /**
     * Removes the unknown and dead Contacts in this bucket.
     */
    public void purge();

    /**
     * Splits the Bucket into two parts.
     */
    public List<Bucket> split();

    /**
     * Returns the total number of Contacts in the Bucket.
     */
    public int size();

    /**
     * Returns the number of active Contacts in the Bucket.
     */
    public int getActiveSize();
    
    /**
     * Returns the maximum number of active Contacts the
     * Bucket can hold (aka k).
     */
    public int getMaxActiveSize();
    
    /**
     * Returns the number of cached Contacts in the Bucket.
     */
    public int getCacheSize();

    /**
     * Clears the Bucket.
     */
    public void clear();
    
    /**
     * Returns whether or not this Bucket needs to be refreshed.
     */
    public boolean isRefreshRequired();
    
    /**
     * Returns true if the given Contact is the local Node.
     */
    public boolean isLocalNode(Contact node);
    
    /**
     * Returns the ClassfulNetworkCounter of this Bucket.
     */
    public ClassfulNetworkCounter getClassfulNetworkCounter();
}
