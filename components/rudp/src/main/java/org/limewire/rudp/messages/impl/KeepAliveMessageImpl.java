package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.MessageFormatException;

/** 
 *  The keepalive message is used to ensure that any firewalls continue 
 *  to allow passage of UDP messages on the connection.  
 * 
 *  Information about the senders data window for buffered incoming data 
 *  and the highest received data packet is included in the otherwise 
 *  unused data space within the guid.  This will be required in the 
 *  case where Ack messages stop flowing because the data window space 
 *  has gone to zero and only KeepAliveMessages are flowing.  Once the 
 *  data window opens back up, Acks will again provide this information.
 */
class KeepAliveMessageImpl extends RUDPMessageImpl implements KeepAliveMessage {

    private long _windowStart;
    private int  _windowSpace;

    /**
     * Construct a new KeepAliveMessage with the specified settings and data
     */
    KeepAliveMessageImpl(byte connectionID, long windowStart, int windowSpace) {
        super(connectionID, OpCode.OP_KEEPALIVE, 0,
                (short)(windowStart & 0xFFFF),
                (short)(windowSpace < 0 ? 0 : windowSpace & 0xFFFF));
        _windowStart = windowStart;
        _windowSpace = windowSpace;
    }

    /**
     * Construct a new KeepAliveMessage from the network
     */
    KeepAliveMessageImpl(byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2)
      throws MessageFormatException {
        super(OpCode.OP_KEEPALIVE, connectionId, sequenceNumber, data1, data2);
        if (data1.remaining() < 4) {
            throw new MessageFormatException("Message not long enough, message length " + data1.remaining() + " < 4");
        }
        data1.order(ByteOrder.BIG_ENDIAN);
        // Parse the added windowStart and windowSpace information
        _windowStart = data1.getShort();
        _windowSpace = data1.getShort();
        data1.rewind();
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.KeepAliveMessage#getWindowStart()
     */
    public long getWindowStart() {
        return _windowStart;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.KeepAliveMessage#extendWindowStart(long)
     */
	public void extendWindowStart(long wStart) {
		_windowStart = wStart;
	}

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.KeepAliveMessage#getWindowSpace()
     */
    public int getWindowSpace() {
        return _windowSpace;
    }

	@Override
    public String toString() {
		return "KeepAliveMessage DestID:"+getConnectionID()+
          " start:"+_windowStart+" space:"+_windowSpace;
	}
}
