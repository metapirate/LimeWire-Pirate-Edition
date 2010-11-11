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

package org.limewire.mojito.db;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.limewire.mojito.routing.Version;

/**
 * Defines an interface for a DHT value encapsulating the {@link DHTValueType}, 
 * {@link Version} and value. 
 */
public interface DHTValue extends Serializable {

    /**
     * An empty value is a value without an actual payload 
     * and storing an empty value in the DHT will remove an
     * existing value from the DHT.
     */
    public static final DHTValue EMPTY_VALUE = new EmptyValue();
    
    /**
     * Returns the type of the value.
     */
    public DHTValueType getValueType();

    /**
     * Returns the version of the value.
     */
    public Version getVersion();

    /**
     * Returns the actual value (a copy) as bytes.
     */
    public byte[] getValue();

    /**
     * Writes the value to the <code>OutputStream</code>.
     */
    public void write(OutputStream out) throws IOException;

    /**
     * Returns the size of the value payload in byte.
     */
    public int size();

    /**
     * An implementation of <code>DHTValue</code> that has no payload.
     */
    static final class EmptyValue implements DHTValue {
        
        private static final long serialVersionUID = 4690500560328936523L;

        private static final byte[] EMPTY = new byte[0];
        
        private EmptyValue() {
        }
        
        public byte[] getValue() {
            return EMPTY;
        }
        
        public void write(OutputStream out) throws IOException {
        }

        public DHTValueType getValueType() {
            return DHTValueType.BINARY;
        }

        public Version getVersion() {
            return Version.ZERO;
        }

        public int size() {
            return 0;
        }
        
        @Override
        public String toString() {
            return "This is an empty DHTValue";
        }
    }
}