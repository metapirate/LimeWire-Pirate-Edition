package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.Message;

/** Vendor Messages are Gnutella Messages that are NEVER forwarded after
 *  received.
 */
public interface VendorMessage extends Message {
    
    static final int LENGTH_MINUS_PAYLOAD = 8;
    
    //Functional IDs defined by Gnutella VendorMessage protocol....
    public static final int F_MESSAGES_SUPPORTED = 0;

    public static final int F_HOPS_FLOW = 4;

    public static final int F_TCP_CONNECT_BACK = 7;

    public static final int F_UDP_CONNECT_BACK = 7;

    public static final int F_UDP_CONNECT_BACK_REDIR = 8;

    public static final int F_CAPABILITIES = 10;

    public static final int F_LIME_ACK = 11;

    public static final int F_REPLY_NUMBER = 12;

    public static final int F_OOB_PROXYING_CONTROL = 13;

    public static final int F_PUSH_PROXY_REQ = 21;

    public static final int F_PUSH_PROXY_ACK = 22;

    @Deprecated
    public static final int F_GIVE_STATS = 14;

    @Deprecated
    public static final int F_STATISTICS = 15;

    public static final int F_CRAWLER_PING = 5;

    public static final int F_CRAWLER_PONG = 6;

    public static final int F_SIMPP_REQ = 16;

    public static final int F_SIMPP = 17;

    public static final int F_UDP_HEAD_PING = 23;

    public static final int F_UDP_HEAD_PONG = 24;

    public static final int F_HEADER_UPDATE = 25;

    public static final int F_UPDATE_REQ = 26;

    public static final int F_UPDATE_RESP = 27;

    public static final int F_CONTENT_REQ = 28;

    public static final int F_CONTENT_RESP = 29;

    public static final int F_INSPECTION_REQ = 30;

    public static final int F_INSPECTION_RESP = 31;

    public static final int F_ADVANCED_TOGGLE = 32;

    public static final int F_DHT_CONTACTS = 33;

    public static final byte[] F_LIME_VENDOR_ID = { (byte) 76, (byte) 73, (byte) 77, (byte) 69 };

    public static final byte[] F_BEAR_VENDOR_ID = { (byte) 66, (byte) 69, (byte) 65, (byte) 82 };

    public static final byte[] F_GTKG_VENDOR_ID = { (byte) 71, (byte) 84, (byte) 75, (byte) 71 };

    public static final byte[] F_NULL_VENDOR_ID = { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };

    public int getVersion();
    
    /** Marker interface for vendor messages which are also Control messages. */
    public static interface ControlMessage extends VendorMessage {}

}