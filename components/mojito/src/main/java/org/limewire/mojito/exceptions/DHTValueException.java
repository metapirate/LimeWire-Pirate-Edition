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

package org.limewire.mojito.exceptions;

import java.io.IOException;

/**
 * Thrown if there's a problem with a <code>DHTValue</code>.
 */
public class DHTValueException extends IOException {
    
    private static final long serialVersionUID = -2488641245100658950L;

    public DHTValueException() {
        super();
    }

    public DHTValueException(String s) {
        super(s);
    }
    
    public DHTValueException(Throwable cause) {
        super();
        initCause(cause);
    }
    
    public DHTValueException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
