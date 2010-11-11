package com.limegroup.gnutella;

import java.net.InetSocketAddress;

import org.limewire.collection.FixedsizeForgetfulHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UDPReplyHandlerCacheImpl implements UDPReplyHandlerCache {
    
    private final UDPReplyHandlerFactory udpReplyHandlerFactory;
    
    /** A mapping of UDPReplyHandlers, to prevent creation of them over-and-over. */
    private final FixedsizeForgetfulHashMap<InetSocketAddress, UDPReplyHandler> udpReplyHandlerCache =
        new FixedsizeForgetfulHashMap<InetSocketAddress, UDPReplyHandler>(500);
    
    @Inject
    public UDPReplyHandlerCacheImpl(UDPReplyHandlerFactory udpReplyHandlerFactory) {
        this.udpReplyHandlerFactory = udpReplyHandlerFactory;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.UDPReplyHnadlerCache#getUDPReplyHandler(java.net.InetSocketAddress)
     */
    public synchronized UDPReplyHandler getUDPReplyHandler(InetSocketAddress addr) {
        UDPReplyHandler handler = udpReplyHandlerCache.get(addr);
        if (handler == null) {
            handler = udpReplyHandlerFactory.createUDPReplyHandler(addr);
            udpReplyHandlerCache.put(addr, handler);
        }
        return handler;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.UDPReplyHnadlerCache#clear()
     */
    public synchronized void clear() {
        udpReplyHandlerCache.clear();
    }
}
