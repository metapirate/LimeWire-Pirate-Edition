package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UDPReplyHandlerFactoryImpl implements UDPReplyHandlerFactory {

    private final UDPService udpService;

    @Inject
    public UDPReplyHandlerFactoryImpl(UDPService udpService) {
        this.udpService = udpService;
    }
    
    public UDPReplyHandler createUDPReplyHandler(InetSocketAddress addr) {
        return new UDPReplyHandler(addr, udpService);
    }

    public UDPReplyHandler createUDPReplyHandler(InetAddress addr, int port) {
        return new UDPReplyHandler(addr, port, udpService);
    }
}

