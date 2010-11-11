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
 
package org.limewire.mojito.messages;

import java.io.Serializable;
import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.db.DHTValueEntity;


/**
 * An interface for StoreResponse implementations.
 */
public interface StoreResponse extends ResponseMessage {

    /**
     * The remote Node was able to store the DHTValue.
     */
    public static final StatusCode OK = StatusCode.valueOf(0x01, "OK");
    
    /**
     * The remote Node was NOT able to store the DHTValue. 
     * Note: This is a very generic error message/code. If you're working
     * with StoreRespones and StoreStatusCodes respectively then consider 
     * checking against the OK StatusCode only.
     */
    public static final StatusCode ERROR = StatusCode.valueOf(0x02, "ERROR");
    
    /**
     * Returns a Collection of StoreStatusCode(s).
     */
    public Collection<StoreStatusCode> getStoreStatusCodes();
    
    /**
     * A StoreStatusCode represents the result of a STORE operation
     * and contains information about the value as well as whether 
     * or not it was stored at the remote Node.
     */
    public static final class StoreStatusCode implements Serializable {
        
        private static final long serialVersionUID = -3753019724686307068L;

        /**
         * The primary key of the DHTValue.
         */
        private final KUID primaryKey;
        
        /**
         * The secondary key of a DHTValue.
         */
        private final KUID secondaryKey;
        
        /**
         * The StatusCode (result) of the STORE operation.
         */
        private final StatusCode statusCode;
        
        public StoreStatusCode(DHTValueEntity entity, StatusCode statusCode) {
            this(entity.getPrimaryKey(), entity.getSecondaryKey(), statusCode);
        }
        
        public StoreStatusCode(KUID primaryKey, KUID secondaryKey, StatusCode statusCode) {
            this.primaryKey = primaryKey;
            this.secondaryKey = secondaryKey;
            this.statusCode = statusCode;
        }
        
        /**
         * Returns true if this StoreStatusCode is for the
         * given DHTValueEntity.
         */
        public boolean isFor(DHTValueEntity entity) {
            return primaryKey.equals(entity.getPrimaryKey())
                    && secondaryKey.equals(entity.getSecondaryKey());
        }
        
        /**
         * Returns the primary key.
         */
        public KUID getPrimaryKey() {
            return primaryKey;
        }
        
        /**
         * Returns the secondary key.
         */
        public KUID getSecondaryKey() {
            return secondaryKey;
        }
        
        /**
         * Returns the StatusCode.
         */
        public StatusCode getStatusCode() {
            return statusCode;
        }
        
        @Override
        public int hashCode() {
            return primaryKey.hashCode() ^ secondaryKey.hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof StoreStatusCode)) {
                return false;
            }
            
            StoreStatusCode other = (StoreStatusCode)o;
            return primaryKey.equals(other.primaryKey)
                    && secondaryKey.equals(other.secondaryKey)
                    && statusCode.equals(other.statusCode);
        }
        
        @Override
        public String toString() {
            return "PrimaryKey=" + primaryKey 
                        + ", secondaryKey=" + secondaryKey 
                        + ", statusCode=" + statusCode;
        }
    }
}
