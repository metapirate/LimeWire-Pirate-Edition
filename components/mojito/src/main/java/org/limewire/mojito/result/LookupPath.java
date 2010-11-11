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

package org.limewire.mojito.result;

import java.util.Collection;
import java.util.Map.Entry;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * A LookupPath is the result of a lookup. Depending on the
 * lookup configuration it's maybe not the full path.
 */
public interface LookupPath {
    
    /**
     * Returns the lookup ID.
     */
    public KUID getLookupID();
    
    /**
     * Returns the lookup path sorted from nearest to 
     * the furthest Contact on the path.
     */
    public Collection<? extends Contact> getPath();
    
    /**
     * Returns SecurityToken for the given Contact.
     */
    public SecurityToken getSecurityToken(Contact node);
    
    /**
     * Returns the lookup path as a Collection of Contact & SecurityToken entries.
     */
    public Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> getEntryPath();
}
