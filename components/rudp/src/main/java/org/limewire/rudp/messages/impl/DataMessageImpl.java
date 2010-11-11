package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.MessageFormatException;

/** The data message is used to communicate data on the connection.
 */
class DataMessageImpl extends RUDPMessageImpl implements DataMessage {

	private final ByteBuffer chunk;

    /**
     * Construct a new <code>DataMessage</code> with the specified data.
     */
    DataMessageImpl(byte connectionID, long sequenceNumber, ByteBuffer chunk) {
        super(connectionID, OpCode.OP_DATA, sequenceNumber, chunk.array(), chunk.remaining());
        this.chunk = chunk;
    }
    
    DataMessageImpl(byte connectionID, long sequenceNumber, byte[] data, int len) {
        super(connectionID, OpCode.OP_DATA, sequenceNumber, data, len);
        this.chunk = null;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getChunk()
     */
    public ByteBuffer getChunk() {
        return chunk;
    }

    /**
     * Construct a new <code>DataMessage</code> from the network.
     */
    DataMessageImpl(byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2)
      throws MessageFormatException {
        super(OpCode.OP_DATA, connectionId, sequenceNumber, data1, data2);
        this.chunk = null;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getData1Chunk()
     */
    public ByteBuffer getData1Chunk() {
        return _data1;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getData2Chunk()
     */
    public ByteBuffer getData2Chunk() {
        return _data2;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getDataAt(int)
     */
    public byte getDataAt(int i) {
        if (i < MAX_DATA1_SIZE) 
            return _data1.get(i + _data1.position());
        else
            return _data2.get(i-MAX_DATA1_SIZE + _data2.position());
    }

	@Override
    public String toString() {
		return "DataMessage DestID:"+getConnectionID()+" len:"+
          getDataLength()+" seq:"+getSequenceNumber();
	}
    
    @Override
    protected int getData1Length() {
        return _data1.limit();
    }
}
