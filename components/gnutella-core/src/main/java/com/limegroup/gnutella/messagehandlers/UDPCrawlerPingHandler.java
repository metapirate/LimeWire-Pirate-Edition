package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;

public class UDPCrawlerPingHandler extends RestrictedResponder {
    
    /**
     * keeps a list of the people who have requested our connection lists. 
     * used to make sure we don't get ping-flooded. 
     * not final so that tests won't take forever.
     */
    private FixedSizeExpiringSet<InetAddress> _UDPListRequestors 
        = new FixedSizeExpiringSet<InetAddress>(2000, 10*60 * 1000); //10 minutes.
    
    private final UDPCrawlerPongFactory udpCrawlerPongFactory;
    
    @Inject
    public UDPCrawlerPingHandler(NetworkManager networkManager,
            UDPReplyHandlerFactory udpReplyHandlerFactory,
            UDPCrawlerPongFactory udpCrawlerPongFactory,
            NetworkInstanceUtils networkInstanceUtils) {
        super(FilterSettings.CRAWLER_IP_ADDRESSES, networkManager,
                udpReplyHandlerFactory, null, networkInstanceUtils);
        this.udpCrawlerPongFactory = udpCrawlerPongFactory;
    }
    
	@Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
		assert msg instanceof UDPCrawlerPing;
		if (!_UDPListRequestors.add(handler.getInetAddress()))
			return;
		
		UDPCrawlerPong pong = udpCrawlerPongFactory.createUDPCrawlerPong((UDPCrawlerPing)msg);
		handler.reply(pong);
	}
	
}
