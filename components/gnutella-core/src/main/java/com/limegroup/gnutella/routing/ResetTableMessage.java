package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.BadPacketException;


/**
 * The RESET route table update message.
 */
public class ResetTableMessage extends RouteTableMessage {
    private int tableSize;
    private byte infinity;

    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Creates a new ResetTableMessage from scratch, with TTL 1. The
     * receiver should initialize its routing table to a tableSize array
     * with values of infinity.  Throws IllegalArgumentException if
     * either value is less than 1.
     *
     * @param tableSize the size of the table
     * @param infinity the smallest value in the route table for infinity,
     *  i.e., one more than the max TTL
     * @see RouteTableMessage
     */
    public ResetTableMessage(int tableSize,
                             byte infinity) {
        //Payload length includes variant
        super((byte)1, 1+4+1, RouteTableMessage.RESET_VARIANT);
        if (tableSize<1 || infinity<1)
            throw new IllegalArgumentException("Argument too small: "
                                               +tableSize+", "+infinity);
        this.tableSize=tableSize;
        this.infinity=infinity;
    }

    @Override
    protected void writePayloadData(OutputStream out) throws IOException {
        byte[] buf=new byte[5];
        ByteUtils.int2leb(tableSize, buf, 0);
        buf[4]=infinity;
        out.write(buf);
    }

    
    /////////////////////////////// Decoding ///////////////////////////////

    /**
     * Creates a new ResetTableMessage with data read from the network.  The
     * payload argument is the complete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT.
     * 
     * @exception BadPacketException the remaining values in payload are not
     *  well-formed, e.g., because it's the wrong length.  
     */
    protected ResetTableMessage(byte[] guid, byte ttl, byte hops, byte[] payload, Network network) {
        super(guid, ttl, hops, payload.length, 
              RouteTableMessage.RESET_VARIANT, network);
        //TODO: maybe we shouldn't enforce this
        //if (payload.length!=(2+4))
        //    throw new BadPacketException("Extra arguments in reset message.");
        tableSize=ByteUtils.leb2int(payload, 1);
        infinity=payload[5];
    }


    /////////////////////////////// Accessors ///////////////////////////////
    
    /** Returns the smallest value in the route table for infinity,
     *  i.e., one more than the max TTL. */
    public byte getInfinity() {
        return infinity;
    }

    /** Returns the new size of the route table. */
    public int getTableSize() {
        return tableSize;
    }

    @Override
    public String toString() {
        return "{RESET, tableSize: "+getTableSize()
              +", Infinity: "+getInfinity()+"}";
    }
}
