package org.limewire.rudp.messages.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.io.ByteBufferInputStream;
import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.RUDPMessage.OpCode;
import org.limewire.rudp.messages.SynMessage.Role;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.google.inject.Singleton;


@Singleton
public class DefaultMessageFactory implements RUDPMessageFactory {
    
    private static final MessageFormatException NO_MATCH = 
        new MessageFormatException("No matching RUDPMessage");
    
    /** 
     * Constructs a message from the given network data.
     * If the ByteBuffers are divided correctly, the data is referenced
     * as opposed to copied, so do not pass mutable buffers.
     */
    public RUDPMessage createMessage(ByteBuffer... data) throws MessageFormatException {
        ByteBufferInputStream in = new ByteBufferInputStream(data);
        if(in.available() < 23)
            throw new MessageFormatException("not enough data for header!");

        // 012345678901234567890123...
        // ABCCDDDDDDDDDDDDEEEFFFFGGGG
        // Explanations:
        // A - The connection id of the remote side
        // B - The first four bits are the opcode,
        //     The second four bits are the length of
        //     data stored in the Ds.
        // C - The sequence number of the message.
        // D - Up to 12 bytes of data (more data is stored in Gs)
        // E - Unused.  May be any value, preferably 0.
        // F - The length of the rest of the message.
        // G - The rest of the data.
        
        byte connectionID = (byte)in.read(); // A
        byte b = (byte)in.read(); // B
        OpCode opcode = OpCode.valueOf((b & 0xF0) >> 4); // b1
        long sequenceNumber = ((long)in.read() & 0xFF) << 8 | ((long)in.read() & 0xFF); // C
        int data1Length = b & 0xF; // b2
        if(data1Length > RUDPMessageImpl.MAX_DATA1_SIZE)
            throw new MessageFormatException("data1Length too big: " + data1Length);
        ByteBuffer data1 = ByteBuffer.allocate(RUDPMessageImpl.MAX_DATA1_SIZE);
        in.read(data1); // D
        data1.flip();
        // only limit data1 buffer to read length for DATA packets, see spec
        if (opcode == OpCode.OP_DATA) {
            data1.limit(data1Length);
        }
        in.skip(3); // E
        
        // Assert that the int in F is the number of bytes remaining.
        int remaining = -1;
        try {
            remaining = ByteUtils.leb2int(in);
        } catch(IOException impossible) {
            ErrorService.error(impossible);
        }
        
        if(remaining != in.available())
            throw new MessageFormatException("inconsistent message size.  expected: " + remaining + ", was: " + in.available());
        
        // Return a reference to the remaining data if possible.
        ByteBuffer data2 = in.bufferFor(remaining); // G
        assert in.available() == 0;

        switch (opcode) {
            case OP_SYN:
                return createSynMessage(connectionID, sequenceNumber, data1, data2);
            case OP_ACK:
                return new AckMessageImpl(connectionID, sequenceNumber, data1, data2);
            case OP_KEEPALIVE:
                return new KeepAliveMessageImpl(connectionID, sequenceNumber, data1, data2);
            case OP_DATA:
                return new DataMessageImpl(connectionID, sequenceNumber, data1, data2);
            case OP_FIN:
                return new FinMessageImpl(connectionID, sequenceNumber, data1, data2);
        }
        
        throw NO_MATCH;
    }
    
    public DataMessage createDataMessage(byte connectionID, long sequenceNumber, ByteBuffer chunk) {
        return new DataMessageImpl(connectionID, sequenceNumber, chunk);
    }

    public AckMessage createAckMessage(byte connectionID, long sequenceNumber, long windowStart, int windowSpace) {
        return new AckMessageImpl(connectionID, sequenceNumber, windowStart, windowSpace);
    }
    
    public FinMessage createFinMessage(byte connectionID, long sequenceNumber, byte reasonCode) {
        return new FinMessageImpl(connectionID, sequenceNumber, reasonCode);
    }

    public KeepAliveMessage createKeepAliveMessage(byte connectionID, long windowStart, int windowSpace) {
        return new KeepAliveMessageImpl(connectionID, windowStart, windowSpace);
    }
    
    public SynMessage createSynMessage(byte connectionID, Role role) {
        return new SynMessageImpl(connectionID, role);
    }
    
    public SynMessage createSynMessage(byte connectionID, byte theirConnectionID, Role role) {
        return new SynMessageImpl(connectionID, theirConnectionID, role);
    }
    
    /**
     * Creates syn message from data read from the network, stubbed out here, so the factory
     * can be subclassed to test old message versions. 
     */
    protected SynMessage createSynMessage(byte connectionID, long sequenceNumber, ByteBuffer data1,
            ByteBuffer data2) throws MessageFormatException {
        return new SynMessageImpl(connectionID, sequenceNumber, data1, data2);
    }
}
