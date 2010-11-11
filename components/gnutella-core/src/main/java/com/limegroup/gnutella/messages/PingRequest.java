package com.limegroup.gnutella.messages;

import org.limewire.io.GUID;

public interface PingRequest extends Message {

    /* various flags related to the SCP ggep field */
    /** Mask for where leaf/ultrapeer requests are. */
    public static final byte SCP_ULTRAPEER_OR_LEAF_MASK = 0x1;

    /** If we're requesting leaf hosts. */
    public static final byte SCP_LEAF = 0x0;

    /** If we're requesting ultrapeer hosts. */
    public static final byte SCP_ULTRAPEER = 0x1;

    /** If we support incoming TLS. */
    public static final byte SCP_TLS = 0x2;

    /**
     * GUID to send out for UDP pings.
     */
    public static final GUID UDP_GUID = new GUID();

    /**
     * Accessor for whether or not this ping meets the criteria for being a
     * "heartbeat" ping, namely having ttl=0 and hops=1.
     * 
     * @return <tt>true</tt> if this ping appears to be a "heartbeat" ping,
     *  otherwise <tt>false</tt>
     */
    public boolean isHeartbeat();

    /**
     * Marks this ping request as requesting a pong carrying
     * an ip:port info.
     */
    public void addIPRequest();

    /**
     * get locale of this PingRequest. 
     */
    public String getLocale();

    /**
     * Determines if this PingRequest has the 'supports cached pongs'
     * marking.
     */
    public boolean supportsCachedPongs();

    /**
     * Gets the data value for the SCP field, if one exists.
     * If none exist, null is returned.  Else, a byte[] of some
     * size is returned.
     */
    public byte[] getSupportsCachedPongData();

    public boolean isQueryKeyRequest();

    /**
     * @return whether this ping wants a reply carrying IP:Port info.
     */
    public boolean requestsIP();

    /**
     * @return whether this ping wants a reply carrying DHT IPP info
     */
    public boolean requestsDHTIPP();

}