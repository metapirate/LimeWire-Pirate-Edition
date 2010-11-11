package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.messages.AbstractMessage;
import com.limegroup.gnutella.messages.BadPacketException;

public abstract class AbstractVendorMessage extends AbstractMessage implements VendorMessage {

    /**
     * Bytes 0-3 of the Vendor Message.  Something like "LIME".getBytes().
     */
    private final byte[] _vendorID;

    /**
     * The Sub-Selector for this message.  Bytes 4-5 of the Vendor Message.
     */
    private final int _selector;

    /**
     * The Version number of the message.  Bytes 6-7 of the Vendor Message.
     */
    private final int _version;

    /**
     * The payload of this VendorMessage.  This usually holds data that is 
     * interpreted by the type of message determined by _vendorID, _selector,
     * and (to a lesser extent) _version.
     */
    private final byte[] _payload;

    /** Cache the hashcode cuz it isn't cheap to compute.
     */
    private final int _hashCode;

    //----------------------------------
    // CONSTRUCTORS
    //----------------------------------


    /**
     * Constructs a new VendorMessage with the given data.
     * Each Vendor Message class delegates to this constructor (or the one
     * also taking a network parameter) to construct new locally generated
     * VMs.
     *  @param vendorIDBytes the Vendor ID of this message (bytes).  
     *  @param selector the selector of the message.
     *  @param version  the version of this message.
     *  @param payload  the payload (not including vendorIDBytes, selector, and
     *  version.
     *  @exception NullPointerException Thrown if payload or vendorIDBytes are
     *  null.
     */
    protected AbstractVendorMessage(byte[] vendorIDBytes, int selector, int version, 
                            byte[] payload) {
        this(vendorIDBytes, selector, version, payload, Network.UNKNOWN);
    }
    
    /**
     * Constructs a new VendorMessage with the given data.
     * Each Vendor Message class delegates to this constructor (or the one that
     * doesn't take the network parameter) to construct new locally generated
     * VMs.
     *  @param vendorIDBytes the Vendor ID of this message (bytes).  
     *  @param selector the selector of the message.
     *  @param version  the version of this message.
     *  @param payload  the payload (not including vendorIDBytes, selector, and
     *  version.
     *  @param network the network this VM is to be written on.
     *  @exception NullPointerException thrown if payload or vendorIDBytes are
     *  null.
     */
    protected AbstractVendorMessage(byte[] vendorIDBytes, int selector, int version, 
                            byte[] payload, Network network) {
        super(F_VENDOR_MESSAGE, (byte)1, LENGTH_MINUS_PAYLOAD + payload.length,
              network);
        if ((vendorIDBytes.length != 4))
            throw new IllegalArgumentException("wrong vendorID length: " +
                                                vendorIDBytes.length);
        if ((selector & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("invalid selector: " + selector);
        if ((version & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("invalid version: " + version);
        // set the instance params....
        _vendorID = vendorIDBytes;
        _selector = selector;
        _version = version;
        _payload = payload;
        // lastly compute the hash
        _hashCode = computeHashCode(_version, _selector, _vendorID, _payload);
    }

    /**
     * Constructs a new VendorMessage with data from the network.
     * Primarily built for the convenience of the class Message.
     * Subclasses must extend this (or the above constructor that doesn't 
     * takes a network parameter) and use getPayload() to parse the payload
     * and do anything else they need to.
     */
    protected AbstractVendorMessage(byte[] guid, byte ttl, byte hops,byte[] vendorID,
                            int selector, int version, byte[] payload, 
                            Network network) throws BadPacketException {
        super(guid, (byte)0x31, ttl, hops, LENGTH_MINUS_PAYLOAD+payload.length,
              network);
        // set the instance params....
        if ((vendorID.length != 4)) {  
            throw new BadPacketException("Vendor ID Invalid!");
        }
        if ((selector & 0xFFFF0000) != 0) {
            throw new BadPacketException("Selector Invalid!");
        }
        if ((version & 0xFFFF0000) != 0) {
            throw new BadPacketException("Version Invalid!");
        }        
        _vendorID = vendorID;
        _selector = selector;
        _version = version;
        _payload = payload;
        // lastly compute the hash
        _hashCode = computeHashCode(_version, _selector, _vendorID,
                                    _payload);
    }

    /**
     * Computes the hash code for a vendor message.
     */
    private static int computeHashCode(int version, int selector, 
                                       byte[] vendorID, byte[] payload) {
        int hashCode = 0;
        hashCode += 17*version;
        hashCode += 17*selector;
        for (int i = 0; i < vendorID.length; i++)
            hashCode += 17*vendorID[i];
        for (int i = 0; i < payload.length; i++)
            hashCode += 17*payload[i];
        return hashCode;
    }

    //----------------------------------


    //----------------------------------
    // ACCESSOR methods
    //----------------------------------

    /** Allows subclasses to make changes gain access to the payload.  They 
     *  can:
     *  <pre>
     *  1) change the contents
     *  2) parse the contents.
     *  </pre>
     *  In general, 1) is discouraged, 2) is necessary.  Subclasses CANNOT
     *  re-init the payload.
     */
    protected byte[] getPayload() {
        return _payload;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.VendorMessageI#getVersion()
     */
    public int getVersion() {
        return _version;
    }

    //----------------------------------

    //----------------------
    // Methods for all subclasses....
    //----------------------
    
    /**
     * @return true if the two VMPs have identical signatures - no more, no 
     * less.  Does not take version into account, but if different versions
     * have different payloads, they'll differ.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof AbstractVendorMessage) {
            AbstractVendorMessage vmp = (AbstractVendorMessage) other;
            return ((_selector == vmp._selector) &&
                    (Arrays.equals(_vendorID, vmp._vendorID)) &&
                    (Arrays.equals(_payload, vmp._payload))
                    );
        }
        return false;
    }
   
    @Override
    public int hashCode() {
        return _hashCode;
    }
 
    //----------------------

    //----------------------
    // ABSTRACT METHODS
    //----------------------

    //----------------------


    //----------------------------------
    // FULFILL abstract Message methods
    //----------------------------------

    // INHERIT COMMENT
    @Override
    protected void writePayload(OutputStream out) throws IOException {
        out.write(_vendorID);
        ByteUtils.short2leb((short)_selector, out);
        ByteUtils.short2leb((short)_version, out);
        writeVendorPayload(out);
    }
    
    protected void writeVendorPayload(OutputStream out) throws IOException {
        out.write(getPayload());
    }

    //----------------------------------


}

