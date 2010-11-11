package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.MessageFormatException;

/** The fin message is used to signal the end of the connection.
 */
class FinMessageImpl extends RUDPMessageImpl implements FinMessage {

    private byte _reasonCode;

    /**
     * Construct a new FinMessage with the specified settings.
     */
    FinMessageImpl(byte connectionID, long sequenceNumber, byte reasonCode) {
        super(connectionID, OpCode.OP_FIN, sequenceNumber, reasonCode);
        _reasonCode = reasonCode;
    }

    /**
     * Construct a new FinMessage from the network.
     */
    FinMessageImpl(byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2)
      throws MessageFormatException {
        super(OpCode.OP_FIN, connectionId, sequenceNumber, data1, data2);
        if (data1.remaining() < 1) {
            throw new MessageFormatException("Message not long enough, message length " + data1.remaining() + " < 1");
        }
        _reasonCode = data1.get();
        data1.rewind(); 
    }

	@Override
    public String toString() {
		return "FinMessage DestID:"+getConnectionID()+" reasonCode:"+_reasonCode;
	}
}
