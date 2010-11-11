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
 
package org.limewire.mojito.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.CollectionUtils;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.impl.LocalContact;

import com.google.inject.Inject;


/**
 * Miscellaneous utilities for Contacts.
 */
public final class ContactUtils {

    private static final Log LOG = LogFactory.getLog(ContactUtils.class);
    
    @Inject
    private static volatile NetworkInstanceUtils networkInstanceUtils = new SimpleNetworkInstanceUtils();
    
    public static void setNetworkInstanceUtils(NetworkInstanceUtils networkInstanceUtils) {
        ContactUtils.networkInstanceUtils = networkInstanceUtils;
    }

    /**
     * A helper method to compare longs.
     */
    private static int compareLong(long a, long b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return 1;
        } else {
            return 0;
        }
    }
    
    /**
     * A Comparator that orders a Collection of Contacts from
     * most recently seen to least recently seen.
     */
    public static final Comparator<Contact> CONTACT_MRS_COMPARATOR = new Comparator<Contact>() {
        public int compare(Contact a, Contact b) {
            // Note: There's a minus sign to change the order from
            // 'small to big' to 'big to small' values
            return -compareLong(a.getTimeStamp(), b.getTimeStamp());
        }
    };
    
    /**
     * A Comparator that orders a Collection of Contacts from alive
     * to failed. The sub-set of alive Contacts is ordered from most
     * recently seen to least recently seen and the sub-set of failed
     * Contacts is ordered by least recently failed to most recently
     * failed.
     */
    public static final Comparator<Contact> CONTACT_ALIVE_TO_FAILED_COMPARATOR = new Comparator<Contact>() {
        public int compare(Contact a, Contact b) {
            // If neither a nor b has failed then use the standard
            // most recently seen (MRS) Comparator
            if (!a.hasFailed() && !b.hasFailed()) {
                return CONTACT_MRS_COMPARATOR.compare(a, b);
            
            // If a has failed and b hasn't then move a to the
            // end of the Collection
            } else if (a.hasFailed() && !b.hasFailed()) {
                return 1;
            
            // If a hasn't failed and b has then move b to
            // the end of the Collection
            } else if (!a.hasFailed() && b.hasFailed()) {
                return -1;
            
            // If both have failed then order by least recently 
            // failed to most recently failed
            } else { 
                return compareLong(a.getLastFailedTime(), b.getLastFailedTime());
            }
        }
    };
    
    private ContactUtils() {}
    
    /**
     * Returns the nodeId and address as a formatted String.
     */
    public static String toString(KUID nodeId, SocketAddress address) {
        if (nodeId != null) {
            if (address != null) {
                return nodeId + " (" + address + ")";
            } else {
                return nodeId.toString();
            }
        } else if (address != null) {
            return address.toString();
        } else {
            return "null";
        }
    }
    
    /**
     * Returns true if the given Contact's address is any of
     * localhost's addresses.
     */
    public static boolean isLocalAddress(Contact node) {
        return NetworkUtils.isLocalAddress(node.getContactAddress());
    }
    
    /**
     * Returns true if the given Contacts have both a localhost address.
     */
    public static boolean areLocalContacts(Contact existing, Contact node) {
        // Huh? The addresses are not equal but both belong
        // obviously to this local machine!?
        InetSocketAddress newAddress = (InetSocketAddress) node.getContactAddress();
        InetSocketAddress oldAddress = (InetSocketAddress) existing.getContactAddress();
        if (NetworkUtils.isLocalAddress(newAddress)
                && NetworkUtils.isLocalAddress(oldAddress)
                && newAddress.getPort() == oldAddress.getPort()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns true if the Contact has a valid SocketAddress.
     */
    public static boolean isValidSocketAddress(Contact node) {
        return NetworkUtils.isValidSocketAddress(node.getContactAddress());
    }
    
    /**
     * Returns true if the given InetAddress is a private address.
     * <p>
     * NOTE: ContactUtils.isPrivateAddress() is checking internally
     * if NetworkSettings.LOCAL_IS_PRIVATE is true! If you're planning 
     * to run the DHT on a Local Area Network (LAN) you want to set 
     * LOCAL_IS_PRIVATE to false!
     */
    public static boolean isPrivateAddress(InetAddress addr) {
        return networkInstanceUtils.isPrivateAddress(addr);
    }
    
    /**
     * Returns true if the given SocketAddress is a private address.
     * <p>
     * NOTE: ContactUtils.isPrivateAddress() is checking internally
     * if NetworkSettings.LOCAL_IS_PRIVATE is true! If you're planning 
     * to run the DHT on a Local Area Network (LAN) you want to set 
     * LOCAL_IS_PRIVATE to false!
     */
    public static boolean isPrivateAddress(SocketAddress address) {
        return networkInstanceUtils.isPrivateAddress(address);
    }
    
    /**
     * Returns true if the Contact has a private SocketAddress.
     * <p>
     * NOTE: ContactUtils.isPrivateAddress() is checking internally
     * if NetworkSettings.LOCAL_IS_PRIVATE is true! If you're planning 
     * to run the DHT on a Local Area Network (LAN) you want to set 
     * LOCAL_IS_PRIVATE to false!
     */
    public static boolean isPrivateAddress(Contact node) {
        return isPrivateAddress(node.getContactAddress());
    }
    
    /**
     * Returns true if the given Contact's contact address is
     * an IPv4 address.
     */
    public static boolean isIPv4Address(Contact node) {
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        return (addr instanceof Inet4Address);
    }
    
    /**
     * Returns true if the given Contact's contact address is
     * an IPv4-compatible IPv6 address.
     */
    public static boolean isIPv4CompatibleAddress(Contact node) {
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        if (addr instanceof Inet6Address
                && ((Inet6Address)addr).isIPv4CompatibleAddress()) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns the masked Class C Network address of the given
     * Contact. 
     * 
     * @see NetworkUtils#getClassC(InetAddress)
     */
    public static int getClassC(Contact node) {
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        return NetworkUtils.getClassC(addr);
    }
    
    /**
     * Returns true if the given Contact's contact address is
     * a private IPv4-compatible IPv6 address.
     */
    public static boolean isPrivateIPv4CompatibleAddress(Contact node) {
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        return NetworkUtils.isPrivateIPv4CompatibleAddress(addr);
    }
    
    /**
     * Returns true if both Contacts have the same Node ID.
     */
    public static boolean isSameNodeID(Contact node1, Contact node2) {
        return node1.getNodeID().equals(node2.getNodeID());
    }
    
    /**
     * Returns true if the given Contact has the same Node ID as the
     * local Node but a different IP Address.
     */
    public static boolean isCollision(Context context, Contact node) {
        if (context.isLocalNodeID(node.getNodeID())
                && !context.isLocalContactAddress(node.getContactAddress())) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the given Contact has the same Node ID or the
     * same IP Address as the local Node.
     */
    public static boolean isLocalContact(Context context, Contact node) {
        
        if (context.isLocalNodeID(node.getNodeID())) {
            return true;
        }
        
        // Imagine you have two Nodes that have each other in
        // their RouteTable. The first Node quits and restarts
        // with a new Node ID. The second Node pings the first
        // Node and we add it to the RouteTable. The first Node
        // starts a lookup and we get a Set of contacts from
        // the second Node which contains our old Contact (different 
        // Node ID but same IPP). So what happens now is that
        // we're sending a lookup to that Node which is the same
        // as sending the lookup to ourself (loopback).
        if (context.isLocalContactAddress(node.getContactAddress())) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(node + " has the same Contact addess as we do " 
                        + context.getLocalNode());
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns true if both Contacts have an IPv4 or IPv6 address.
     */
    public static boolean isSameAddressSpace(Contact a, Contact b) {
        return NetworkUtils.isSameAddressSpace(
                    a.getContactAddress(), 
                    b.getContactAddress());
    }

    /**
     * Takes the given Contact and returns a version of it that
     * can be used to test for Node ID collisions.
     */
    public static Contact createCollisionPingSender(Contact localNode) {
        if (!(localNode instanceof LocalContact)) {
            throw new IllegalArgumentException("Contact must be an instance of LocalContact: " + localNode);
        }

        // The idea is to invert our local Node ID so that the
        // other Node doesn't get the impression we're trying
        // to spoof anything and we don't want that the other
        // guy adds this Contact to its RouteTable. To do so
        // we're creating a firewalled version of our local Node
        // (with the inverted Node ID of course).
        Vendor vendor = localNode.getVendor();
        Version version = localNode.getVersion();
        KUID nodeId = localNode.getNodeID().invert();
        SocketAddress addr = localNode.getContactAddress();
        Contact sender = ContactFactory.createLiveContact(
                addr, vendor, version, nodeId, addr, 0, Contact.FIREWALLED_FLAG);
        
        return sender;
    }

    /**
     * Returns true if the given Contact is a collision ping sender.
     */
    public static boolean isCollisionPingSender(KUID localNodeId, Contact sender) {
        // The sender must be firewalled!
        if (!sender.isFirewalled()) {
            return false;
        }
        
        // See createCollisionPingSender(...)
        KUID expectedSenderId = localNodeId.invert();
        return expectedSenderId.equals(sender.getNodeID());
    }
    
    /**
     * Returns the most recently seen contact from the list.
     * Use ContactUtils.sort() prior to calling this Method!
     */
    public static <T extends Contact> Contact getMostRecentlySeen(Collection<T> nodes) {
        List<T> list = CollectionUtils.toList(nodes);
        assert (list.get(0).getTimeStamp() >= list.get(nodes.size()-1).getTimeStamp());
        return list.get(0);
    }

    /**
     * Returns the least recently seen contact from the list.
     * Use ContactUtils.sort() prior to calling this Method!
     */
    public static <T extends Contact> Contact getLeastRecentlySeen(Collection<T> nodes) {
        List<T> list = CollectionUtils.toList(nodes);
        assert (list.get(nodes.size()-1).getTimeStamp() <= list.get(0).getTimeStamp());
        return list.get(nodes.size()-1);
    }

    /**
     * Sorts the given List of Contacts from most recently seen to 
     * least recently seen and returns a sub-list with at most
     * count number of elements.
     */
    public static <T extends Contact> Collection<T> sort(Collection<T> nodes, int count) {
        return sort(nodes).subList(0, Math.min(count, nodes.size()));
    }

    /**
     * Sorts the Contacts from most recently seen to least recently seen.
     */
    public static <T extends Contact> List<T> sort(Collection<T> nodes) {
        List<T> list = CollectionUtils.toList(nodes);
        Collections.sort(list, CONTACT_MRS_COMPARATOR);
        return list;
    }

    /**
     * Sorts the contacts from most recently seen to
     * least recently seen based on their timestamp and last failed time.
     * <p>
     * Used when loading the routing table if our nodeID has changed.
     */
    public static <T extends Contact> List<T> sortAliveToFailed(Collection<T> nodes) {
        List<T> list = CollectionUtils.toList(nodes);
        Collections.sort(list, CONTACT_ALIVE_TO_FAILED_COMPARATOR);
        return list;
    }
}
