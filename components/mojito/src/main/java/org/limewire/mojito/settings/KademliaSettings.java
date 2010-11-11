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
 
package org.limewire.mojito.settings;

/**
 * Various Kademlia related Settings.
 */
public class KademliaSettings {
    
    private KademliaSettings() {}
    
    /**
     * The replication parameter is also known as K.
     */
    public static final FakeIntSetting REPLICATION_PARAMETER = new FakeIntSetting(20);
    
    public static class FakeIntSetting {
        
        private final int value;
        
        private FakeIntSetting(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
}
