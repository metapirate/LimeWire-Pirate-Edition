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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.handler.response.PingResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler.PingIterator;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.EntryImpl;


/**
 * Provides various <code>PingIterator</code> 
 * implementations for the <code>PingManager</code>.
 */
class PingIteratorFactory {
    
    private PingIteratorFactory() {
    }
    
    /**
     * A ContactPinger sends ping requests to a Set of Contacts
     */
    static class ContactPinger implements PingIterator {
        
        final Collection<? extends Contact> nodes;
        
        final Iterator<? extends Contact> it;
        
        public ContactPinger(Contact node) {
            this(Collections.singleton(node));
        }
        
        public ContactPinger(Set<? extends Contact> nodes) {
            if (nodes == null) {
                throw new NullPointerException("Set<Contact> is null");
            }
            
            for (Contact c : nodes) {
                if (c == null) {
                    throw new NullPointerException("Contact is null");
                }
            }
            
            this.nodes = nodes;
            this.it = nodes.iterator();
        }
        
        public boolean hasNext() {
            return it.hasNext();
        }
        
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            Contact node = it.next();
            
            KUID nodeId = node.getNodeID();
            SocketAddress dst = node.getContactAddress();
            
            RequestMessage request = context.getMessageHelper().createPingRequest(dst);
            return context.getMessageDispatcher().send(nodeId, dst, request, responseHandler);
        }
        
        @Override
        public String toString() {
            return "ContactPinger: " + nodes;
        }
    }
    
    /**
     * Sends collision ping requests to a Set of <code>Contact</code>s.
     */
    static class CollisionPinger extends ContactPinger {
        
        private final Contact sender;
        
        public CollisionPinger(Context context, Contact sender, Contact node) {
            this(context, sender, Collections.singleton(node));
        }
        
        public CollisionPinger(Context context, Contact sender, 
                Set<? extends Contact> nodes) {
            super(nodes);
            
            if (ContextSettings.ASSERT_COLLISION_PING.getValue()) {
                assertCollisionPing(context, sender, nodes);
            }
            
            this.sender = sender;
        }
        
        private void assertCollisionPing(Context context, Contact sender, 
                Set<? extends Contact> nodes) {
            
            KUID localId = context.getLocalNodeID();
            
            if (!ContactUtils.isCollisionPingSender(localId, sender)) {
                throw new IllegalArgumentException(sender 
                        + " is not a valid collision ping Contact");
            }
            
            for (Contact node : nodes) {
                if (!localId.equals(node.getNodeID())) {
                    throw new IllegalArgumentException(node 
                            + " must have the same ID as the local Node ID: " + localId);
                }
            }
        }
        
        @Override
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            Contact node = it.next();
            
            KUID nodeId = node.getNodeID();
            SocketAddress dst = node.getContactAddress();
            
            // Send a collision test ping instead of a regular ping
            RequestMessage request = context.getMessageFactory().createPingRequest(sender, dst);
            
            return context.getMessageDispatcher().send(nodeId, dst, request, responseHandler);
        }
        
        @Override
        public String toString() {
            return "CollisionPinger: " + sender;
        }
    }

    /**
     * Sends ping requests to a Set of <code>SocketAddresses</code>.
     */
    static class SocketAddressPinger implements PingIterator {
        
        private final Set<? extends SocketAddress> hosts;
        
        private final Iterator<? extends SocketAddress> it;
        
        public SocketAddressPinger(SocketAddress host) {
            this(Collections.singleton(host));
        }
        
        public SocketAddressPinger(Set<? extends SocketAddress> hosts) {
            if (hosts == null) {
                throw new NullPointerException("Set<SocketAddress> is null");
            }
            
            for (SocketAddress addr : hosts) {
                if (addr == null) {
                    throw new NullPointerException("SocketAddress is null");
                }
            }
            
            this.hosts = hosts;
            this.it = hosts.iterator();
        }
        
        public boolean hasNext() {
            return it.hasNext();
        }
        
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            SocketAddress dst = it.next();
            
            RequestMessage request = context.getMessageHelper().createPingRequest(dst);
            return context.getMessageDispatcher().send(null, dst, request, responseHandler);
        }
        
        @Override
        public String toString() {
            return "SocketAddressPinger: " + hosts.toString();
        }
    }
    
    /**
     * An EntryPinger sends ping requests to a Set of Entry<KUID, SocketAddress>
     */
    static class EntryPinger implements PingIterator {
        
        private final Collection<? extends Entry<KUID, ? extends SocketAddress>> entries;
        
        private final Iterator<? extends Entry<KUID, ? extends SocketAddress>> it;
        
        public EntryPinger(KUID nodeId, SocketAddress address) {
            this(Collections.singleton(
                    new EntryImpl<KUID, SocketAddress>(nodeId, address)));
        }
        
        public EntryPinger(Entry<KUID, ? extends SocketAddress> entry) {
            this(Collections.singleton((Entry<KUID, ? extends SocketAddress>)entry));
        }
        
        public EntryPinger(Set<? extends Entry<KUID, ? extends SocketAddress>> entries) {
            if (entries == null) {
                throw new NullPointerException("Set<Entry<KUID, SocketAddress>> is null");
            }
            
            for (Entry<KUID, ? extends SocketAddress> entry : entries) {
                if (entry == null) {
                    throw new NullPointerException("Entry<KUID, SocketAddress> is null");
                }
                
                if (entry.getValue() == null) {
                    throw new NullPointerException("SocketAddress is null");
                }
            }
            
            this.entries = entries;
            this.it = entries.iterator();
        }
        
        public boolean hasNext() {
            return it.hasNext();
        }
        
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            
            Entry<KUID, ? extends SocketAddress> entry = it.next();
            
            KUID nodeId = entry.getKey();
            SocketAddress dst = entry.getValue();
            
            RequestMessage request = context.getMessageHelper().createPingRequest(dst);
            return context.getMessageDispatcher().send(nodeId, dst, request, responseHandler);
        }
        
        @Override
        public String toString() {
            return "EntryPinger: " + entries.toString();
        }
    }
}
