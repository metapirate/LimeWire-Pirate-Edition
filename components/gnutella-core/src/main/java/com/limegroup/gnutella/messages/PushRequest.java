package com.limegroup.gnutella.messages;

public interface PushRequest extends Message {

    public static final long FW_TRANS_INDEX = Integer.MAX_VALUE - 2;

    /** Returns true if this Push indicates the host is capable of receiving TLS connections. */
    public boolean isTLSCapable();

    public byte[] getClientGUID();

    public long getIndex();

    public boolean isFirewallTransferPush();

    public byte[] getIP();

    public int getPort();
}