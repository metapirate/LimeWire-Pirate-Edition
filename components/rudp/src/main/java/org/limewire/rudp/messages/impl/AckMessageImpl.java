package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.MessageFormatException;

/** The ack message is used to acknowledge all non-ack packets in the protocol.
 */
class AckMessageImpl extends RUDPMessageImpl implements AckMessage {

    private long _windowStart;
    private int  _windowSpace;

    /**
     * Construct a new AckMessage with the specified settings and data
     */
    AckMessageImpl(byte connectionID, long sequenceNumber, long windowStart, int windowSpace) {
        super(connectionID, OpCode.OP_ACK, sequenceNumber,
              (short)(windowStart & 0xFFFF),
              (short)(windowSpace < 0 ? 0 : windowSpace & 0xFFFF));
        _windowStart = windowStart;
        _windowSpace = windowSpace;
    }

    /**
     * Construct a new AckMessage from the network
     */
    AckMessageImpl(byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2)
      throws MessageFormatException {
        super(OpCode.OP_ACK, connectionId, sequenceNumber, data1, data2);
        if (data1.remaining() < 4) {
            throw new MessageFormatException("Message not long enough, message length " + data1.remaining() + " < 4");
        }
        data1.order(ByteOrder.BIG_ENDIAN);
        _windowStart = data1.getShort();
        _windowSpace = data1.getShort();
        data1.rewind();
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.AckMessage#getWindowStart()
     */
    public long getWindowStart() {
        return _windowStart;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.AckMessage#extendWindowStart(long)
     */
	public void extendWindowStart(long wStart) {
		_windowStart = wStart;
	}

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.AckMessage#getWindowSpace()
     */
    public int getWindowSpace() {
        return _windowSpace;
    }

	@Override
    public String toString() {
		return "AckMessage DestID:"+getConnectionID()+
		  " start:"+_windowStart+" space:"+_windowSpace+
		  " seq:"+getSequenceNumber();
	}
}
