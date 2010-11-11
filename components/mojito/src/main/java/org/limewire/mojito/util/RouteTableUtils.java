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

import java.util.Collection;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;

/**
 * Utilities for the RouteTable.
 */
public class RouteTableUtils {
    
    private RouteTableUtils() {}
    
    /**
     * Returns the percentage of active Contacts that are alive 
     * in the RouteTable.
     */
    public static float getPercentageOfAliveContacts(RouteTable routeTable) {
        Collection<Contact> activeContacts = routeTable.getActiveContacts();
        if (activeContacts.isEmpty()) {
            return 0.0f;
        }
        
        int alive = 0;
        for (Contact node : activeContacts) {
            if (node.isAlive()) {
                alive++;
            }
        }
        
        return ((float)alive)/((float)activeContacts.size());
    }
}
