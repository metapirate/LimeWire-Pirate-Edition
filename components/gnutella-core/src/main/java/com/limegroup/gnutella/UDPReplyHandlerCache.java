package com.limegroup.gnutella;

import java.net.InetSocketAddress;

public interface UDPReplyHandlerCache {

    public ReplyHandler getUDPReplyHandler(InetSocketAddress addr);

    public void clear();
}