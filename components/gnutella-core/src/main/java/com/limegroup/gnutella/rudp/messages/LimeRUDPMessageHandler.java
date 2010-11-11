package com.limegroup.gnutella.rudp.messages;

import java.net.InetSocketAddress;

import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.messages.RUDPMessage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

@EagerSingleton
class LimeRUDPMessageHandler implements MessageHandler, Service {
    
    private final Provider<UDPMultiplexor> plexor;
    private final Provider<MessageRouter> router;
    
    @Inject LimeRUDPMessageHandler(Provider<UDPMultiplexor> plexor, Provider<MessageRouter> router) {
        this.plexor = plexor;
        this.router = router;
    }
    
    @Inject void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    /** Installs this handler on the given router. */
    public void initialize() {
        plexor.get();
        router.get().setUDPMessageHandler(LimeAckMessageImpl.class, this);
        router.get().setUDPMessageHandler(LimeDataMessageImpl.class, this);
        router.get().setUDPMessageHandler(LimeFinMessageImpl.class, this);
        router.get().setUDPMessageHandler(LimeKeepAliveMessageImpl.class, this);
        router.get().setUDPMessageHandler(LimeSynMessageImpl.class, this);
    }

    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        plexor.get().routeMessage((RUDPMessage)msg, addr);
    }

    public void start() {} 
    public void stop() {}
    
    public String getServiceName() {
        return I18nMarker.marktr("RUDP Message Routing");
    }
}
