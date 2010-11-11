package org.limewire.rudp.messages.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.util.BufferUtils;

/** Abstract message class to allow a reliable UDP 
 * protocol to be built on top of Gnutella messages.
 */
public abstract class RUDPMessageImpl implements RUDPMessage {

    /** The maximum amount of data that can go into the GUID   */
    protected static final int MAX_DATA1_SIZE = 12; 

	/** An empty byte array for internal use */
	protected static byte[] emptyByteArray = new byte[16];

    /** This is an identifier for a stream from a given IP. 1 byte   */
    protected final byte _connectionID;

    /** This is the opcode for the sub-message type.        1 nibble */
    protected final OpCode _opcode;

    /** The communications message sequence number.         2 bytes  */
    protected long _sequenceNumber;
    
    /** The first piece of data in this message.
        This will hold any data stored in the GUID.
        Up to MAX_DATA1_SIZE bytes */
    protected final ByteBuffer _data1; 
    
    /** The second piece of data in this message.
        This will hold any data stored in the payload. 
        Up to 512 bytes by design. */
    protected final ByteBuffer _data2;
    
    /** Constructs an RUDPMessage with shorts integers of data. */
    protected RUDPMessageImpl(byte connectionID, OpCode opcode, long sequenceNumber, short d1, short d2) {
        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        _data1 = ByteBuffer.allocate(4);
        _data1.order(ByteOrder.BIG_ENDIAN);
        _data1.putShort(d1);
        _data1.putShort(d2);
        _data1.flip();
        _data2 = BufferUtils.getEmptyBuffer();
    }
    
    /** Constructs an RUDPMessage with one byte of data. */
    protected RUDPMessageImpl(byte connectionID, OpCode opcode, long sequenceNumber, byte b) {
        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        _data1 = ByteBuffer.allocate(1);
        _data1.put(b);
        _data1.flip();
        _data2 = BufferUtils.getEmptyBuffer();
    }
    
    /** Constructs an RUDPMessage with one byte and one short of data. */
    protected RUDPMessageImpl(byte connectionID, OpCode opcode, long sequenceNumber, byte b, short d) {
        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        _data1 = ByteBuffer.allocate(3);
        _data1.order(ByteOrder.BIG_ENDIAN);
        _data1.put(b);
        _data1.putShort(d);
        _data1.flip();
        _data2 = BufferUtils.getEmptyBuffer();
    }

    protected RUDPMessageImpl(byte connectionID, OpCode opcode, long sequenceNumber,
            byte[] data) {
        this(connectionID, opcode, sequenceNumber, data, data.length);
    }
    
    /** Constructs an RUDPMessage with a byte[] of data. */
    protected RUDPMessageImpl(byte connectionID, OpCode opcode, long sequenceNumber,
                              byte[] data, int datalength ) {
        _connectionID   = connectionID;
        _opcode         = opcode;
        _sequenceNumber = sequenceNumber;
        if(datalength > 0)
            _data1 = ByteBuffer.wrap(data, 0, Math.min(datalength, MAX_DATA1_SIZE));
        else
            _data1 = BufferUtils.getEmptyBuffer();
        
        int data2Length = Math.max(0, datalength - MAX_DATA1_SIZE);
        if(data2Length > 0)
            _data2 = ByteBuffer.wrap(data, MAX_DATA1_SIZE, data2Length);
        else
            _data2 = BufferUtils.getEmptyBuffer();
    }
    
    /**
     * Construct a new UDPConnectionMessage from the network.
     */
    protected RUDPMessageImpl(OpCode opcode, byte connectionId, long sequenceNumber, ByteBuffer data1, ByteBuffer data2) 
      throws MessageFormatException {
        _opcode = opcode;
        _connectionID = connectionId;
        _sequenceNumber = sequenceNumber;
        _data1 = data1;
        _data2 = data2;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPMessage#getConnectionID()
     */
    public byte getConnectionID() {
        return _connectionID;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPMessage#getSequenceNumber()
     */
    public long getSequenceNumber() {
        return _sequenceNumber;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPMessage#extendSequenceNumber(long)
     */
    public void extendSequenceNumber(long seqNo) {
        _sequenceNumber = seqNo;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPMessage#getDataLength()
     */
    public int getDataLength() {
        return _data1.limit() + _data2.limit();
    }
    
    public int getLength() {
        return 23 + _data2.limit();
    }
    
    public OpCode getOpCode() {
        return _opcode;
    }
    
    /**
     * Returns the length of the data1 block. Since only some packets have
     * an actual length value the default implementation returns 0;
     */
    protected int getData1Length() {
       return 0; 
    }
    
    /** Writes the entire message to an OutputStream. */
    public void write(OutputStream out) throws IOException {
        out.write(_connectionID);
        out.write(((_opcode.toByte() & 0x0F) << 4) | ((byte)getData1Length() & 0x0F));
        out.write((byte)((_sequenceNumber & 0xFF00) >> 8));
        out.write((byte)(_sequenceNumber & 0x00FF));
        if(_data1.hasRemaining())
            out.write(_data1.array(), _data1.arrayOffset() + _data1.position(), _data1.remaining());
        //make sure we fill up the remaining header data.
        if(_data1.remaining() < MAX_DATA1_SIZE)
            out.write(emptyByteArray, 0, MAX_DATA1_SIZE - _data1.remaining());
        // write out the reserved area numbers.
        out.write(F_RUDP_MESSAGE);
        out.write((byte)1);
        out.write((byte)0);
        // write out the length of the payload.
        org.limewire.util.ByteUtils.int2leb(_data2.remaining(), out);
        // write the payload.
        if ( _data2.hasRemaining() )
            out.write(_data2.array(), _data2.arrayOffset() + _data2.position(), _data2.remaining());
    }
}
