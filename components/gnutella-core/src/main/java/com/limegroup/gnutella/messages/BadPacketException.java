package com.limegroup.gnutella.messages;

import org.limewire.io.InvalidDataException;

/** 
 * An exception for reading bad data from the network. 
 * This is generally non-fatal.
 */
public class BadPacketException extends InvalidDataException {
    public BadPacketException() { }
    public BadPacketException(String msg) { super(msg); }
    public BadPacketException(Throwable cause) { super(cause); }

    /** 
     * Reusable exception for efficiency that can be statically
     * accessed.  These are created a lot, so it makes sense to
     * cache it.
     */
    public static final BadPacketException HOPS_EXCEED_SOFT_MAX = 
        new BadPacketException("Hops already exceeds soft maximum");

    /**
     * Cached exception for not handling URN queries.
     */
    public static final BadPacketException CANNOT_ACCEPT_URN_QUERIES =
        new BadPacketException("cannot accept URN queries");

    /**
     * Cached exception for queries that are too big.
     */
    public static final BadPacketException QUERY_TOO_BIG =
        new BadPacketException("query too big");

    /**
     * Cached exception for XML queries that are too big.
     */
    public static final BadPacketException XML_QUERY_TOO_BIG =
        new BadPacketException("XML query too big");

    /**
     * Cached exception for queries that have illegal characters.
     */
    public static final BadPacketException ILLEGAL_CHAR_IN_QUERY =
        new BadPacketException("illegal chars in query");

}
