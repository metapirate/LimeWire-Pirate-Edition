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

/**
 * Miscellaneous utilities for Exceptions.
 */
public class ExceptionUtils {
    
    private ExceptionUtils() {}
    
    /**
     * Returns true if Throwable <tt>t</tt> was caused by an another
     * Exception that is of type <tt>clazz</tt>.
     */
    public static boolean isCausedBy(Throwable t, Class<? extends Throwable> clazz) {
        return getCause(t, clazz) != null;
    }
    
    /**
     * Returns the first Exception from the exception cause 
     * chain that is of type <tt>clazz</tt> or null if the
     * given Throwable was not caused a such Exception.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T getCause(Throwable t, Class<T> clazz) {
        while(t != null) {
            if (clazz.isInstance(t)) {
                return (T)t;
            }
            t = t.getCause();
        }
        return null;
    }
}
