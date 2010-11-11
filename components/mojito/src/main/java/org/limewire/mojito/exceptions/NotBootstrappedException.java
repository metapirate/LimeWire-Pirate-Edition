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

/**
 * Thrown if the user is attempting
 * to execute certain operations that require a bootstrapped DHT.
 */
public class NotBootstrappedException extends IllegalStateException {
    
    private static final long serialVersionUID = 5286215339791253173L;

    public NotBootstrappedException(String name, String operation) {
        super(getErrorMessage(name, operation));
    }
    
    public static String getErrorMessage(String name, String operation) {
        return name + " is attempting to execute a " + operation 
                    + " while not bootstrapped to the network";
    }
}
