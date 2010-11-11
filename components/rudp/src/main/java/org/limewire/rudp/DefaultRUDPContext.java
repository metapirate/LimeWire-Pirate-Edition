package org.limewire.rudp;

import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

/**
 * Aggregates key RUDP classes. <code>DefaultRUDPContext</code> contains
 * a {@link RUDPMessageFactory}, a {@link TransportListener},
 * {@link UDPService}, {@link RUDPSettings} and a {@link MessageDispatcher},
 * along with getters for each object.
 */
public class DefaultRUDPContext implements RUDPContext {
    
    /** The MessageFactory RUDP should create messages from. */
    private final RUDPMessageFactory messageFactory;
    
    /** The TransportListener that should be notified when events are pending. */
    private final TransportListener transportListener;
    
    /** The UDPService that messages are sent through & udp data is learned from. */
    private final UDPService udpService;
    
    /** The dispatcher that messages are pumped through. */
    private final MessageDispatcher messageDispatcher;
    
    /** The settings that control the algorithm. */
    private final RUDPSettings rudpSettings;
   
    public DefaultRUDPContext() {
        this(new DefaultMessageFactory(), new NoOpTransportListener(),
             null, new DefaultMessageDispatcher(), new DefaultRUDPSettings());
    }
    
    public DefaultRUDPContext(RUDPMessageFactory factory) {
        this(factory, new NoOpTransportListener(),
             null, new DefaultMessageDispatcher(), new DefaultRUDPSettings());
    }
    
    public DefaultRUDPContext(TransportListener transportListener) {
        this(new DefaultMessageFactory(), transportListener,
             null, new DefaultMessageDispatcher(), new DefaultRUDPSettings());
    }
    
    public DefaultRUDPContext(UDPService udpService) {
        this(new DefaultMessageFactory(), new NoOpTransportListener(),
             udpService, null, new DefaultRUDPSettings());
    }
    
    public DefaultRUDPContext(RUDPSettings settings) {
        this(new DefaultMessageFactory(), new NoOpTransportListener(),
             null, new DefaultMessageDispatcher(), settings);
    }
    
    public DefaultRUDPContext(RUDPMessageFactory factory,
                              TransportListener transportListener,
                              UDPService udpService, 
                              RUDPSettings settings) {
        this(factory, transportListener, udpService, null, settings);
    }
    
    public DefaultRUDPContext(RUDPMessageFactory factory,
                              TransportListener transportListener,
                              UDPService udpService,
                              MessageDispatcher dispatcher, 
                              RUDPSettings settings) {
        this.messageFactory = factory;
        this.transportListener = transportListener;
        if(udpService == null)
            this.udpService = new DefaultUDPService(dispatcher);
        else
            this.udpService = udpService;
        this.messageDispatcher = dispatcher;
        this.rudpSettings = settings;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPContext#getMessageFactory()
     */
    public RUDPMessageFactory getMessageFactory() {
        return messageFactory;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPContext#getTransportListener()
     */
    public TransportListener getTransportListener() {
        return transportListener;
    }
    
    public UDPService getUDPService() {
        return udpService;
    }
    
    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
    
    public RUDPSettings getRUDPSettings() {
        return rudpSettings;
    }
    
    /** A NoOp TransportListener. */
    private static final class NoOpTransportListener implements TransportListener {
        public void eventPending() {
        }
    }

}
