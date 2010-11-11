package com.limegroup.gnutella.rudp;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.RUDPSettings;
import org.limewire.rudp.UDPService;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageParser;

/** The parts necessary for linking together LimeWire & RUDP. */
@Singleton
class LimeRUDPContext implements RUDPContext {

    private final RUDPMessageFactory rudpMessageFactory;
    private final RUDPSettings rudpSettings;
    private final UDPService udpService;
    private final NIODispatcher nioDispatcher;
    
    /**
     * Constructs a new LimeRUDPContext and installs it as the parser
     * for incoming messages.
     */
    @Inject
    public LimeRUDPContext(UDPService udpService,
            RUDPMessageFactory rudpMessageFactory, RUDPSettings rudpSettings, MessageFactory messageFactory,
            NIODispatcher nioDispatcher) {
        this.udpService = udpService;
        this.rudpMessageFactory = rudpMessageFactory;
        this.rudpSettings = rudpSettings;
        this.nioDispatcher = nioDispatcher;
        
        MessageParser parser = new LimeRUDPMessageParser(rudpMessageFactory);
        messageFactory.setParser(RUDPMessage.F_RUDP_MESSAGE, parser);
    }

    public RUDPMessageFactory getMessageFactory() {
        return rudpMessageFactory;
    }

    public TransportListener getTransportListener() {
        return nioDispatcher.getTransportListener();
    }

    public RUDPSettings getRUDPSettings() {
        return rudpSettings;
    }

    public UDPService getUDPService() {
        return udpService;
    }

}
