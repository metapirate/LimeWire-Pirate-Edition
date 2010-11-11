package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;

/** 
 * Defines the interface for a Gnutella message (packet). See
 * <a href="http://rfc-gnutella.sourceforge.net/developer/testing/messageArchitecture.html">
 * Gnutella message architecture</a> for more information.
 *
 */
public interface Message extends Comparable<Message> {

    /** The network a message came from or will travel through. */
    public static enum Network {
        UNKNOWN, TCP, UDP, MULTICAST;        
    }
    
    // Functional IDs defined by Gnutella protocol.
    public static final byte F_PING = (byte) 0x0;

    public static final byte F_PING_REPLY = (byte) 0x1;

    public static final byte F_PUSH = (byte) 0x40;

    public static final byte F_QUERY = (byte) 0x80;

    public static final byte F_QUERY_REPLY = (byte) 0x81;

    public static final byte F_ROUTE_TABLE_UPDATE = (byte) 0x30;

    public static final byte F_VENDOR_MESSAGE = (byte) 0x31;

    public static final byte F_VENDOR_MESSAGE_STABLE = (byte) 0x32;

    public static final byte F_UDP_CONNECTION = (byte) 0x41;

    /**
     * Writes a message quickly, without using temporary buffers or crap.
     */
    public void writeQuickly(OutputStream out) throws IOException;

    /**
     * Writes a message out, using the buffer as the temporary header.
     */
    public void write(OutputStream out, byte[] buf) throws IOException;

    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out.
     */
    public void write(OutputStream out) throws IOException;

    ////////////////////////////////////////////////////////////////////
    public Network getNetwork();

    public boolean isMulticast();

    public boolean isUDP();

    public boolean isTCP();

    public boolean isUnknownNetwork();

    public byte[] getGUID();

    public byte getFunc();

    public byte getTTL();

    /**
     * If Time To Live (TTL) is less than zero, throws IllegalArgumentException.  
     * Otherwise sets this TTL to the given value.  This is useful when you 
     * want certain messages to travel less than others.
     *    @modifies this' TTL
     */
    public void setTTL(byte ttl) throws IllegalArgumentException;

    /**
     * If the hops is less than zero, throws IllegalArgumentException.
     * Otherwise sets this hops to the given value.  This is useful when you
     * want certain messages to look as if they've travelled further.
     *   @modifies this' hops
     */
    public void setHops(byte hops) throws IllegalArgumentException;

    public byte getHops();

    /** Returns the length of this' payload, in bytes. */
    public int getLength();

    /** Returns the total length of this, in bytes. */
    public int getTotalLength();

    /** @modifies this
     *  @effects increments hops, decrements TTL if > 0, and returns the
     *   OLD value of TTL.
     */
    public byte hop();

    /** 
     * Returns the system time (i.e., the result of System.currentTimeMillis())
     * this was instantiated.
     */
    public long getCreationTime();

    /** Returns this user-defined priority.  Lower values are higher priority. */
    public int getPriority();

    /** Set this user-defined priority for flow-control purposes.  Lower values
     *  are higher priority. */
    public void setPriority(int priority);

    /**
     * Returns the class that message handlers for it should register upon, i.e.
     * the interface class that it implements. 
     */
    public Class<? extends Message> getHandlerClass();
}