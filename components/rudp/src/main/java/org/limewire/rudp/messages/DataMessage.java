package org.limewire.rudp.messages;

import java.nio.ByteBuffer;

/**
 * Defines an interface for a data message. The data message is used to 
 * communicate data on the connection.
 */
public interface DataMessage extends RUDPMessage {

    /** The maximum amount of data a message can hold. */
    public static final int MAX_DATA = 512;

    /** Returns the chunk that was used for creation, if it was created with a chunk. */
    public ByteBuffer getChunk();

    /** Return the data in the GUID as the data1 chunk. */
    public ByteBuffer getData1Chunk();

    /** Return the data in the payload as the data2 chunk. */
    public ByteBuffer getData2Chunk();

    /** Returns the piece of data at this point of the message. */
    public byte getDataAt(int i);

}