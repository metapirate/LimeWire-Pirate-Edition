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

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * Defines the interface for all DHT messages.
 */
public interface DHTMessage {
    
    /** The function ID of our DHT Message */
    public static final int F_DHT_MESSAGE = 0x44; // 'D'
    
    /**
     * The opcodes of the LimeWire DHT messages.
     */
    public static enum OpCode {
        
        PING_REQUEST(0x01),
        PING_RESPONSE(0x02),
        
        STORE_REQUEST(0x03),
        STORE_RESPONSE(0x04),
        
        FIND_NODE_REQUEST(0x05),
        FIND_NODE_RESPONSE(0x06),
        
        FIND_VALUE_REQUEST(0x07),
        FIND_VALUE_RESPONSE(0x08),
        
        STATS_REQUEST(0x09),
        STATS_RESPONSE(0x0A);
        
        private final int opcode;
            
        private OpCode(int opcode) {
            this.opcode = opcode;
        }
    
        public int toByte() {
            return opcode;
        }
        
        @Override
        public String toString() {
            return name() + " (" + toByte() + ")";
        }
        
        private static OpCode[] OPCODES;
        
        static {
            OpCode[] values = values();
            OPCODES = new OpCode[values.length];
            for (OpCode o : values) {
                int index = o.opcode % OPCODES.length;
                if (OPCODES[index] != null) {
                    // Check the enums for duplicate opcodes!
                    throw new IllegalStateException("OpCode collision: index=" + index 
                            + ", OPCODES=" + OPCODES[index] + ", o=" + o);
                }
                OPCODES[index] = o;
            }
        }
        
        /**
         * Returns the OpCode enum for the integer. Throws an
         * MessageFormatException if opcode is unknown!
         */
        public static OpCode valueOf(int opcode) throws MessageFormatException {
            OpCode o = OPCODES[opcode % OPCODES.length];
            if (o != null && o.opcode == opcode) {
                return o;
            }
            throw new MessageFormatException("Unknown opcode: " + opcode);
        }
    }
    
    /** Returns the opcode (type) of the Message. */
    public OpCode getOpCode();
    
    /** Returns the sender of this Message. */
    public Contact getContact();
    
    /** Returns the Message ID of the Message. */
    public MessageID getMessageID();
    
    /** Returns the Version of the Message. */
    public Version getMessageVersion();
    
    /** Writes this Message to the OutputStream. */
    public void write(OutputStream out) throws IOException;

    /** The length of this message. */
    public int getLength();
}
