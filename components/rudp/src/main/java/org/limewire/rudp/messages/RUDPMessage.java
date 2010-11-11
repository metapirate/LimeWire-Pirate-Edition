package org.limewire.rudp.messages;

import java.io.IOException;
import java.io.OutputStream;

/** Defines the interface in which all RUDP messages derive.  */
public interface RUDPMessage {

    /** The main functor of an RUDP message. */
    public static final byte F_RUDP_MESSAGE = (byte)0x41;
    
    // The version number of the protocol to allow for future improvements
    public static final short PROTOCOL_VERSION_NUMBER = 1;
    
    /** Used to specify a message type (either SYN, ACK, KEEPALIVE, DATA or 
     * FIN).
     */
    public static enum OpCode {
        OP_SYN(0x0),
        OP_ACK(0x1),
        OP_KEEPALIVE(0x2),
        OP_DATA(0x3),
        OP_FIN(0x4);
        
        private int opcode;
            
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
    
    /** Gets the OpCode for the message. */
    public OpCode getOpCode();
    
    /** Writes the message to the OutputStream. */
    public void write(OutputStream out) throws IOException;

    /** Return the messages connectionID identifier. */
    public byte getConnectionID();

    /** Return the messages sequence number */
    public long getSequenceNumber();

    /**
     *  Extend the sequence number of incoming messages with the full 8 bytes
     *  of state.
     */
    public void extendSequenceNumber(long seqNo);

    /** Return the length of data stored in this message. */
    public int getDataLength();
    
    /** Returns the length of the message. */
    public int getLength();

}