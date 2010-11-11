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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.limewire.collection.IntHashMap;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;

/**
 * The SameClassFilter filters all Contacts that have the same
 * Class C Network IP Address.
 */
public class SameClassFilter {
    
    private final IntHashMap<Contact> filter;
    
    public SameClassFilter(Contact sender) {
        this(sender, KademliaSettings.REPLICATION_PARAMETER.getValue());
    }
    
    public SameClassFilter(Contact sender, int initalSize) {
        this.filter = new IntHashMap<Contact>(initalSize);
        add(sender);
    }

    private boolean add(Contact node) {
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        return filter.put(NetworkUtils.getClassC(addr), node) == null;
    }
    
    /**
     * Returns true if the given Contact is from the
     * same Class C Network as the sender or an another
     * Contact.
     */
    public boolean isSameNetwork(Contact node) {
        return !add(node);
    }
}
