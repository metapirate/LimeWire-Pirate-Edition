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

import org.limewire.mojito.routing.Contact;


/**
 * BootstrapResults are fired during bootstrapping and after
 * bootstrapping has finished.
 */
public class BootstrapResult implements Result {
    
    /**
     * Various types of Bootstrap Events.
     */
    public static enum ResultType {
        
        /**
         * Fired when the bootstrap process finished
         * successfully.
         */
        BOOTSTRAP_SUCCEEDED,
        
        /**
         * Fired when the bootstrap process finished
         * unsuccessfully.
         */
        BOOTSTRAP_FAILED;
    }
    
    private final Contact node;
    
    private final long time;
    
    private final ResultType resultType;
    
    public BootstrapResult(Contact node, long time, ResultType resultType) {
        this.node = node;
        this.time = time;
        this.resultType = resultType;
    }
    
    /**
     * Returns the initial bootstrap Node.
     */
    public Contact getContact() {
        return node;
    }
    
    /**
     * Returns the ResultType.
     */
    public ResultType getResultType() {
        return resultType;
    }
    
    /**
     * Returns the total bootstrapping time.
     */
    public long getTime() {
        return time;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ResultType: ").append(resultType).append("\n");
        buffer.append("Time: ").append(getTime()).append("ms\n");
        buffer.append("Contact: ").append(getContact()).append("\n");
        
        return buffer.toString();
    }
}
