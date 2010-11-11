package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface UDPReplyHandlerFactory {

    public UDPReplyHandler createUDPReplyHandler(InetSocketAddress addr);

    public UDPReplyHandler createUDPReplyHandler(InetAddress addr, int port);
    
}