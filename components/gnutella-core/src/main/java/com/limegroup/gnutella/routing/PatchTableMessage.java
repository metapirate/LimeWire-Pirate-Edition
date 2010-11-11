package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;


/**
 * The PATCH route table update message.  This class is as simple as possible.
 * For example, the getData() method returns the raw bytes of the message,
 * requiring the caller to call the getEntryBits() method to calculate the i'th
 * patch value.  (Note that this is trivial if getEntryBits() returns 8.)  This
 * is by intention, as patches are normally split into several 
 * PatchTableMessages.
 */
public class PatchTableMessage extends RouteTableMessage {
    /** For sequenceNumber and size, we really do need values of 0-255.
     *  Java bytes are signed, of course, so we store shorts internally
     *  and convert to bytes when writing. */
    private short sequenceNumber;
    private short sequenceSize;
    private byte compressor;
    private byte entryBits;
    //TODO: I think storing payload here would be more efficient
    private byte[] data;

    public static final byte COMPRESSOR_NONE=0x0;
    public static final byte COMPRESSOR_DEFLATE=0x1;
    

    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Creates a new PATCH variant from scratch, with TTL 1.  The patch data is
     * copied from dataSrc[datSrcStart...dataSrcStop-1], inclusive.  
     * 
     * @requires sequenceNumber and sequenceSize can fit in one unsigned byte,
     *              sequenceNumber and sequenceSize >= 1,
     *              sequenceNumber<=sequenceSize
     *           compressor one of COMPRESSOR_NONE or COMPRESSOR_DEFLATE
     *           entryBits less than 1
     *           dataSrcStart>dataSrcStop
     *           dataSrcStart or dataSrcStop not valid indices fof dataSrc
     * @see RouteTableMessage 
     */
    public PatchTableMessage(short sequenceNumber,
                             short sequenceSize,
                             byte compressor,
                             byte entryBits,
                             byte[] dataSrc,
                             int dataSrcStart,
                             int dataSrcStop) {
        //Payload length INCLUDES variant
        super((byte)1,
              5+(dataSrcStop-dataSrcStart), 
              RouteTableMessage.PATCH_VARIANT);
        this.sequenceNumber=sequenceNumber;
        this.sequenceSize=sequenceSize;
        this.compressor=compressor;
        this.entryBits=entryBits;
        //Copy dataSrc[dataSrcStart...dataSrcStop-1] to data
        data=new byte[dataSrcStop-dataSrcStart];       //TODO3: avoid
        System.arraycopy(dataSrc, dataSrcStart, data, 0, data.length);
    }

    @Override
    protected void writePayloadData(OutputStream out) throws IOException {
        //Does NOT include variant
        byte[] buf=new byte[4+data.length];
        buf[0]=(byte)sequenceNumber;
        buf[1]=(byte)sequenceSize;
        buf[2]=compressor;
        buf[3]=entryBits;
        System.arraycopy(data, 0, buf, 4, data.length); //TODO3: avoid
        out.write(buf);
    }

    
    /////////////////////////////// Decoding ///////////////////////////////

    /**
     * Creates a new PATCH variant with data read from the network.  
     * The first byte is guaranteed to be PATCH_VARIANT.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length, the sequence size
     *  is less than the sequence number, etc.
     */
    protected PatchTableMessage(byte[] guid, byte ttl, byte hops, byte[] payload, Network network)
            throws BadPacketException {
        super(guid, ttl, hops, payload.length, RouteTableMessage.PATCH_VARIANT, network);
        // TODO: maybe we shouldn't enforce this
        //if (payload.length<5)
        //    throw new BadPacketException("Extra arguments in reset message.");
        assert(payload[0]==PATCH_VARIANT);
        this.sequenceNumber=(short)ByteUtils.ubyte2int(payload[1]);
        this.sequenceSize=(short)ByteUtils.ubyte2int(payload[2]);
        if (sequenceNumber<1 || sequenceSize<1 || sequenceNumber>sequenceSize) 
            throw new BadPacketException(
                "Bad sequence/size: "+sequenceNumber+"/"+sequenceSize);
        this.compressor=payload[3];
        if (! (compressor==COMPRESSOR_NONE || compressor==COMPRESSOR_DEFLATE))
            throw new BadPacketException("Bad compressor: "+compressor);
        this.entryBits=payload[4];
        if (entryBits<0)
            throw new BadPacketException("Negative entryBits: "+entryBits);
        this.data=new byte[payload.length-5];        
        System.arraycopy(payload, 5, data, 0, data.length);  //TODO3: avoid
    }


    /////////////////////////////// Accessors ///////////////////////////////
    
    public short getSequenceNumber() {
        return sequenceNumber;
    }

    public short getSequenceSize() {
        return sequenceSize;
    }
        
    public byte getCompressor() {
        return compressor;
    }

    public byte getEntryBits() {
        return entryBits;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("{PATCH, Sequence: "+getSequenceNumber()+"/"+getSequenceSize()
              +", Bits: "+entryBits+", Compr: "+getCompressor()+", [");
//          for (int i=0; i<data.length; i++) {
//              if (data[i]!=0)
//                  buf.append(i+"/"+data[i]+", ");
//          }
        buf.append("<"+data.length+" bytes>");
        buf.append("]");
        return buf.toString();
    }
}
