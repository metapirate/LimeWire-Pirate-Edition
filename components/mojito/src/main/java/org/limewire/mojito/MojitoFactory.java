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

package org.limewire.mojito;

import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;

/**
 * Creates or loads <code>MojitoDHT</code>s.
 */
public class MojitoFactory {
    
    /**
     * The default name of a Mojito DHT instance if none is specified.
     */
    private static final String DEFAULT_NAME = "DHT";
    
    private MojitoFactory() {}
    
    /**
     * Creates a <code>MojitoDHT</code> with default settings.
     */
    public static MojitoDHT createDHT() {
        return createDHT(DEFAULT_NAME);
    }
    
    /**
     * Creates a <code>MojitoDHT</code> with the given name.
     */
    public static MojitoDHT createDHT(String name) {
        return create(name, false);
    }
    
    /**
     * Creates a <code>MojitoDHT</code> with the given name, vendor code and version.
     */
    public static MojitoDHT createDHT(String name, Vendor vendor, Version version) {
        return create(name, vendor, version, false);
    }
    
    /**
     * Creates a firewalled <code>MojitoDHT</code>.
     */
    public static MojitoDHT createFirewalledDHT() {
        return createFirewalledDHT(DEFAULT_NAME);
    }
    
    /**
     * Creates a firewalled <code>MojitoDHT</code> with the given name.
     */
    public static MojitoDHT createFirewalledDHT(String name) {
        return create(name, true);
    }
    
    /**
     * Creates a firewalled <code>MojitoDHT</code> with the given name, 
     * vendor code and version.
     */
    public static MojitoDHT createFirewalledDHT(String name, Vendor vendor, Version version) {
        return create(name, vendor, version, true);
    }
    
    /**
     * Creates a <code>MojitoDHT</code> with the given arguments.
     */
    private static Context create(String name, boolean firewalled) {
        return create(name, 
                    ContextSettings.getVendor(), 
                    ContextSettings.getVersion(), 
                    firewalled);
    }
    
    /**
     * Creates a <code>MojitoDHT</code> with the given arguments.
     */
    private static Context create(String name, Vendor vendor, Version version, boolean firewalled) {
        
        if (name == null) {
            name = DEFAULT_NAME;
        }
        
        return new Context(name, vendor, version, firewalled);
    }
}
