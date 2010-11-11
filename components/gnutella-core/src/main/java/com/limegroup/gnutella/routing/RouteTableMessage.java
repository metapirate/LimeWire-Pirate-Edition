package com.limegroup.gnutella.routing;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.AbstractMessage;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * An abstract class representing all variants of the new ROUTE_TABLE_UPDATE
 * message. Like Message, this has no public constructors.  To decode bytes from
 * call the static read(..) method.  To create a new message from scratch, call
 * one of its subclass' constructors.<p>
 */
public abstract class RouteTableMessage extends AbstractMessage {
    public static final byte RESET_VARIANT=(byte)0x0;
    public static final byte PATCH_VARIANT=(byte)0x1;

    private byte variant;


    //////////////////////////// Encoding /////////////////////////////////////

    /**
     * Creates a new RouteTableMessage from scratch.
     */
    protected RouteTableMessage(byte ttl,
                                int length, 
                                byte variant) {
        super(Message.F_ROUTE_TABLE_UPDATE, ttl, length);
        this.variant=variant;
    }


    @Override
    protected void writePayload(OutputStream out) throws IOException {
        out.write(variant);
        writePayloadData(out);
    }

    /** 
     * @modifies out
     * @effects writes the data following the common variant 
     *  to out.  Does NOT flush out.
     */
    protected abstract void writePayloadData(OutputStream out) 
                                                      throws IOException;



    //////////////////////////////// Decoding ////////////////////////////////

    /**
     * Creates a new RouteTableMessage from raw bytes read from the network.
     * The returned value is a subclass of RouteTableMessage depending on
     * the variant in the payload.  (That's why there is no corresponding
     * constructor in this.)
     * 
     * @throws BadPacketException the payload is not well-formatted
     */
    public static RouteTableMessage read(byte[] guid, byte ttl, byte hops, byte[] payload,
            Network network) throws BadPacketException {
        // Parse the common bytes of the payload...
        if (payload.length<2)
            throw new BadPacketException("Payload too small");
        byte variant=payload[0];
        
        //...and pass them to the subclass' constructor, which will in turn
        //call this constructor.
        switch (variant) {
        case RESET_VARIANT:
            return new ResetTableMessage(guid, ttl, hops, payload, network);
        case PATCH_VARIANT:
            return new PatchTableMessage(guid, ttl, hops, payload, network);
        default:
            throw new BadPacketException("Unknown table variant");
        }
    }


    /*
     * Creates a new RouteTableMessage with data read from the network.  This
     * is called by subclasses after the payload is fully decoded.
     * 
     * @param variant the type of update message.  REQUIRES: one of 
     *  RESET_VARIANT or PATCH_VARIANT.
     *
     * @see com.limegroup.gnutella.Message
     */
    protected RouteTableMessage(byte[] guid, byte ttl, byte hops, int length, byte variant,
            Network network) {
        super(guid, Message.F_ROUTE_TABLE_UPDATE, ttl, hops, length, network);
        this.variant = variant;
    }



    // /////////////////////////// Accessors //////////////////////////////

    /**
     * Returns the variant of this, i.e. one of RESET_VARIANT, or PATCH_VARIANT.
     */
    public byte getVariant() {
        return variant;
    }
}


 


