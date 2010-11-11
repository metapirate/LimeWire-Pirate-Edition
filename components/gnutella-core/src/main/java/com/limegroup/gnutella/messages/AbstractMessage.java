package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.GUID;
import org.limewire.util.ByteUtils;


/**
 * A Gnutella message (packet).  This class is abstract; subclasses
 * implement specific messages such as search requests.<p>
 *
 * All messages have message IDs, function IDs, TTLs, hops taken, and
 * data length.  Messages come in two flavors: requests (ping, search)
 * and replies (pong, search results).  Message are mostly immutable;
 * only the TTL, hops, and priority field can be changed.
 */

public abstract class AbstractMessage implements Message {
    
    
    /** Same as GUID.makeGUID.  This exists for backwards compatibility. */
    public static byte[] makeGuid() {
        return GUID.makeGuid();
    }

    ////////////////////////// Instance Data //////////////////////

    private byte[] guid;
    private final byte func;

    /* We do not support TTLs > 2^7, nor do we support packets
     * of length > 2^31 */
    private byte ttl;
    private byte hops;
    private int length;

    /** Priority for flow-control.  Lower numbers mean higher priority.NOT
     *  written to network. */
    private int priority=0;
    /** Time this was created.  Not written to network. */
    private final long creationTime=System.currentTimeMillis();
    /**
     * The network that this was received on or is going to be sent to.
     */
    private final Network network;
   
    /** Rep. invariant */
    protected void repOk() {
        assert(guid.length==16);
        assert func==F_PING || func==F_PING_REPLY
                    || func==F_PUSH
                    || func==F_QUERY || func==F_QUERY_REPLY
                    || func==F_VENDOR_MESSAGE 
                    || func == F_VENDOR_MESSAGE_STABLE
                    || func == F_UDP_CONNECTION
                    : "Bad function code";

        assert ttl>=0 : "Negative TTL: "+ttl;
        assert hops>=0 : "Negative hops: "+hops;
        assert length>=0 : "Negative length: "+length;
    }

    ////////////////////// Constructors and Producers /////////////////

    /**
     * @requires func is a valid functional id (i.e., 0, 1, 64, 128, 129),
     *  0 &<;= ttl, 0 &<;= length (i.e., high bit not used)
     * @effects Creates a new message with the following data.
     *  The GUID is set appropriately, and the number of hops is set to 0.
     */
    protected AbstractMessage(byte func, byte ttl, int length) {
        this(func, ttl, length, Network.UNKNOWN);
    }

    protected AbstractMessage(byte func, byte ttl, int length, Network network) {
        this(makeGuid(), func, ttl, (byte)0, length, network);
    }

    /**
     * Same as above, but caller specifies TTL and number of hops.
     * This is used when reading packets off network.
     */
    protected AbstractMessage(byte[] guid, byte func, byte ttl,
              byte hops, int length) {
        this(guid, func, ttl, hops, length, Network.UNKNOWN);
    }

    /**
     * Same as above, but caller specifies the network.
     * This is used when reading packets off network.
     */
    protected AbstractMessage(byte[] guid, byte func, byte ttl,
              byte hops, int length, Network network) {
        if (guid.length != 16) {
            throw new IllegalArgumentException("invalid guid length: " + guid.length);
        }
        this.guid = guid;
        this.func = func;
        this.ttl = ttl;
        this.hops = hops;
        this.length = length;
        this.network = network;
        // repOk();
    }

    /**
     * Writes a message quickly, without using temporary buffers or crap.
     */
    public void writeQuickly(OutputStream out) throws IOException {
        out.write(guid, 0, guid.length /* 16 */);
        out.write(func);
        out.write(ttl);
        out.write(hops);
        ByteUtils.int2leb(length, out);
        writePayload(out);
    }
    
    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    public void write(OutputStream out, byte[] buf) throws IOException {
        System.arraycopy(guid, 0, buf, 0, guid.length /* 16 */);
        buf[16]=func;
        buf[17]=ttl;
        buf[18]=hops;
        ByteUtils.int2leb(length, buf, 19);
        out.write(buf);
        writePayload(out);
    }

    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStream out) throws IOException {
        write(out, new byte[23]);
    }

    /** @modifies out
     *  @effects writes the payload specific data to out (the stuff
     *   following the header).  Does NOT flush out.
     */
    protected abstract void writePayload(OutputStream out) throws IOException;
    
    ////////////////////////////////////////////////////////////////////
    public Network getNetwork() {
        return network;
    }
    
    public boolean isMulticast() {
        return network == Network.MULTICAST;
    }
    
    public boolean isUDP() {
        return network == Network.UDP;
    }
    
    public boolean isTCP() {
        return network == Network.TCP;
    }
    
    public boolean isUnknownNetwork() {
        return network == Network.UNKNOWN;
    }

    public byte[] getGUID() {
        return guid;
    }

    public byte getFunc() {
        return func;
    }

    public byte getTTL() {
        return ttl;
    }

    /**
     * If ttl is less than zero, throws IllegalArgumentException.  Otherwise sets
     * this TTL to the given value.  This is useful when you want certain messages
     * to travel less than others.
     *    @modifies this' TTL
     */
    public void setTTL(byte ttl) throws IllegalArgumentException {
        if (ttl < 0)
            throw new IllegalArgumentException("invalid TTL: "+ttl);
        this.ttl = ttl;
    }
    
    /**
     * Sets the guid for this message. Is needed, when we want to cache 
     * query replies or other messages, and change the GUID as per the 
     * request
     * @param guid the guid to be set
     */
    protected void setGUID(GUID guid) {
        this.guid = guid.bytes();
    }
    
    /**
     * If the hops is less than zero, throws IllegalArgumentException.
     * Otherwise sets this hops to the given value.  This is useful when you
     * want certain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    public void setHops(byte hops) throws IllegalArgumentException {
        if(hops < 0)
            throw new IllegalArgumentException("invalid hops: " + hops);
        this.hops = hops;
    }

    public byte getHops() {
        return hops;
    }

    /** Returns the length of this' payload, in bytes. */
    public int getLength() {
        return length;
    }

    /** Updates length of this' payload, in bytes. */
    protected void updateLength(int l) {
        length=l;
    }

    /** Returns the total length of this, in bytes. */
    public int getTotalLength() {
        //Header is 23 bytes.
        return 23+length;
    }

    /** @modifies this
     *  @effects increments hops, decrements TTL if > 0, and returns the
     *   OLD value of TTL.
     */
    public byte hop() {
        hops++;
        if (ttl>0)
            return ttl--;
        else
            return ttl;
    }

    /** 
     * Returns the system time (i.e., the result of System.currentTimeMillis())
     * this was instantiated.
     */
    public long getCreationTime() {
        return creationTime;
    }

    /** Returns this user-defined priority.  Lower values are higher priority. */
    public int getPriority() {
        return priority;
    }

    /** Set this user-defined priority for flow-control purposes.  Lower values
     *  are higher priority. */
    public void setPriority(int priority) {
        this.priority=priority;
    }
    
    /** 
     * Returns a negative value if this is of lesser priority than message,
     * positive value if of higher priority, or zero if of same priority.
     * Remember that lower priority numbers mean HIGHER priority.
     *
     * @exception ClassCastException message not an instance of Message 
     */
    public int compareTo(Message m) {
        return m.getPriority() - this.getPriority();
    }

    @Override
    public String toString() {
        return "{guid="+(new GUID(guid)).toString()
             +", ttl="+ttl
             +", hops="+hops
             +", priority="+getPriority()+"}";
    }

    /**
     * Should return the most specific message interface that his class
     * implements.
     * <p>
     * This is needed since listeners register themselves on the interface class
     * id. It can go away once listeners subscribe to the message id or instance
     * of checks are used.
     * </p>
     */
    public Class<? extends Message> getHandlerClass() {
        return getClass();
    }
}
