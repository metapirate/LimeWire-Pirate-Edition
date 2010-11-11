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

/**
 * Defines an interface for requesting statistic messages. The messages contain
 * security state information.
 */
public interface StatsRequest extends RequestMessage, DHTSecureMessage {

    /**
     * Defines the types of statistic requests for a remote Node.
     */
    public static enum StatisticType {
        
        /**
         * Request the remote Node's Statistics.
         */
        STATISTICS(0x01),
        
        /**
         * Request the remote Node's Database.
         */
        DATABASE(0x02),
        
        /**
         * Request the remote Node's RouteTable.
         */
        ROUTETABLE(0x03);
        
        private int type;
        
        private StatisticType(int type) {
            this.type = type;
        }
        
        public int toByte() {
            return type;
        }
        
        @Override
        public String toString() {
            return name() + " (" + toByte() + ")";
        }
        
        private static final StatisticType[] TYPES;
        
        static {
            StatisticType[] types = values();
            TYPES = new StatisticType[types.length];
            for (StatisticType t : types) {
                int index = t.type % TYPES.length;
                if (TYPES[index] != null) {
                    throw new IllegalStateException("Type collision: index=" + index 
                            + ", TYPE=" + TYPES[index] + ", t=" + t);
                }
                TYPES[index] = t;
            }
        }
        
        public static StatisticType valueOf(int type) throws MessageFormatException {
            int index = type % TYPES.length;
            StatisticType t = TYPES[index];
            if (t.type == type) {
                return t;
            }
            
            throw new MessageFormatException("Unknown type: " + type);
        }
    }
    
    /**
     * Returns the Type of the request.
     */
    public StatisticType getType();
}
