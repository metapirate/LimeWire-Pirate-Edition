package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.SynMessage;

/** Begins a reliable UDP connection by pinging the other host
 *  and by communicating the desired identifying connection ID.
 */
public class SynMessageImpl extends RUDPMessageImpl implements SynMessage {

	private final byte _senderConnectionID;
    private final short  _protocolVersionNumber;
    private final Role role;

    /**
     * Construct a new SynMessage with the specified settings and data
     */
    SynMessageImpl(byte connectionID, Role role) {
        this(connectionID, (byte)0, role);
    }

    /**
     * Construct a new SynMessage with both my Connection ID and theirs
     */
    SynMessageImpl(byte connectionID, byte theirConnectionID, Role role) {
        super(theirConnectionID, OpCode.OP_SYN, 0, deriveData(connectionID, PROTOCOL_VERSION_NUMBER, role));
        _senderConnectionID    = connectionID;
        this.role = role;
        _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }

    static byte[] deriveData(byte connectionID, short protocolVersionNumber, Role role) {
        ByteBuffer data = ByteBuffer.allocate(4);
        data.order(ByteOrder.BIG_ENDIAN);
        data.put(connectionID);
        data.putShort(protocolVersionNumber);
        data.put(role.byteValue());
        return data.array();
    }

    /**
     * Construct a new SynMessage from the network
     */
    SynMessageImpl(byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2)
      throws MessageFormatException {
        super(OpCode.OP_SYN, connectionId, sequenceNumber, data1, data2);
        if (data1.remaining() < 3) {
            throw new MessageFormatException("Message not long enough, message length " + data1.remaining() + " < 3");
        }
        _senderConnectionID = data1.get();
        data1.order(ByteOrder.BIG_ENDIAN);
        _protocolVersionNumber = data1.getShort();
        if (_protocolVersionNumber >= 1) {
            byte value = data1.get();
            Role role = Role.valueOf(value);
            this.role = role != null ? role : Role.UNDEFINED;
        } else {
            this.role = Role.UNDEFINED;
        }
        data1.rewind();
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.SynMessage#getSenderConnectionID()
     */
    public byte getSenderConnectionID() {
        return _senderConnectionID; 
    }

	/* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.SynMessage#getProtocolVersionNumber()
     */
	public int getProtocolVersionNumber() {
		return _protocolVersionNumber; 
	}

	@Override
    public String toString() {
		return "SynMessage DestID:"+getConnectionID()+
		  " SrcID:"+_senderConnectionID+" vNo:"+_protocolVersionNumber;
	}

    @Override
    public Role getRole() {
        return role;
    }
}
