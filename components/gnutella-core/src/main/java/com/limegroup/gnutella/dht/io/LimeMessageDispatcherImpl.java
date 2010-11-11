package com.limegroup.gnutella.dht.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.PublicKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.util.CryptoUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Provider;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.dht.messages.FindNodeRequestWireImpl;
import com.limegroup.gnutella.dht.messages.FindNodeResponseWireImpl;
import com.limegroup.gnutella.dht.messages.FindValueRequestWireImpl;
import com.limegroup.gnutella.dht.messages.FindValueResponseWireImpl;
import com.limegroup.gnutella.dht.messages.MessageFactoryWire;
import com.limegroup.gnutella.dht.messages.PingRequestWireImpl;
import com.limegroup.gnutella.dht.messages.PingResponseWireImpl;
import com.limegroup.gnutella.dht.messages.StatsRequestWireImpl;
import com.limegroup.gnutella.dht.messages.StatsResponseWireImpl;
import com.limegroup.gnutella.dht.messages.StoreRequestWireImpl;
import com.limegroup.gnutella.dht.messages.StoreResponseWireImpl;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;

/**
 * LimeMessageDispatcher re-routes DHTMessage(s) through the LimeWire core so
 * that all communcation can be done over a single network port.
 */
public class LimeMessageDispatcherImpl extends MessageDispatcher implements
        MessageHandler {

    private static final Log LOG = LogFactory
            .getLog(LimeMessageDispatcherImpl.class);

    /**
     * An array of Messages this MessageHandler supports
     */
    private static final Class[] UDP_MESSAGE_TYPES = {
            PingRequestWireImpl.class, PingResponseWireImpl.class,
            StoreRequestWireImpl.class, StoreResponseWireImpl.class,
            FindNodeRequestWireImpl.class, FindNodeResponseWireImpl.class,
            FindValueRequestWireImpl.class, FindValueResponseWireImpl.class,
            StatsRequestWireImpl.class, StatsResponseWireImpl.class };

    private volatile boolean running = false;

    private volatile boolean bound = false;
    
    private final Provider<UDPService> udpService;
    private final Provider<SecureMessageVerifier> secureMessageVerifier;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<com.limegroup.gnutella.MessageDispatcher> messageDispatcher;

    public LimeMessageDispatcherImpl(Context context, Provider<UDPService> udpService,
            Provider<SecureMessageVerifier> secureMessageVerifier,
            Provider<MessageRouter> messageRouter,
            Provider<com.limegroup.gnutella.MessageDispatcher> messageDispatcher,
            MessageFactory messageFactory) {
        super(context);

        this.udpService = udpService;
        this.secureMessageVerifier = secureMessageVerifier;
        this.messageRouter = messageRouter;
        this.messageDispatcher = messageDispatcher;

        // Get Context's MessageFactory and wrap it into a
        // MessageFactoryWire and set it as the MessageFactory
        context.setMessageFactory(new MessageFactoryWire(context
                .getMessageFactory()));

        // Register the Message type
        MessageParserDelegate parser = new MessageParserDelegate(context
                .getMessageFactory());

        messageFactory.setParser((byte) DHTMessage.F_DHT_MESSAGE, parser);
    }

    @Override
    protected boolean allow(DHTMessage message) {
        // Host blocking is already done in NIODispatcher
        return true;
    }

    @Override
    public void bind(SocketAddress address) throws IOException {
        assert (!bound);
        bound = true;
    }

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public synchronized void start() {
        // Install the Message handlers
        for (Class<? extends Message> clazz : UDP_MESSAGE_TYPES) {
            messageRouter.get().setUDPMessageHandler(clazz, this);
        }

        running = true;
        super.start();
    }

    @Override
    public synchronized void stop() {
        running = false;
        super.stop();

        // Remove the Message handlers
        for (Class<? extends Message> clazz : UDP_MESSAGE_TYPES) {
            messageRouter.get().setUDPMessageHandler(clazz, null);
        }
    }

    @Override
    public void close() {
        bound = false;
        super.close();
    }

    /*
     * Overwritten:
     * 
     * Takes the payload of Tag and sends it via LimeWire's UDPService
     */
    @Override
    protected boolean submit(Tag tag) {
        InetSocketAddress dst = (InetSocketAddress) tag.getSocketAddress();
        ByteBuffer data = tag.getData();
        udpService.get().send(data, dst, true);
        register(tag);

        if (LOG.isInfoEnabled()) {
            LOG.info("Sent: " + tag);
        }
        return true;
    }

    /*
     * Implements:
     * 
     * Takes the Message, fixes the source address and delegates it to
     * MessageDispatcher's back-end
     */
    public void handleMessage(Message msg, InetSocketAddress addr,
            ReplyHandler handler) {

        if (LOG.isInfoEnabled()) {
            LOG.info("Received message from " + addr + ", " + msg);
        }

        if (!isRunning()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping message from " + addr
                        + " because DHT is not running");
            }
            return;
        }

        DHTMessage dhtMessage = (DHTMessage) msg;
        ((RemoteContact) dhtMessage.getContact())
                .fixSourceAndContactAddress(addr);
        handleMessage(dhtMessage);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    protected void process(Runnable runnable) {
        if (isRunning()) {
            messageDispatcher.get().dispatch(runnable);
        }
    }

    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        PublicKey pubKey = context.getPublicKey();
        if (pubKey == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping SecureMessage " + secureMessage
                        + " because PublicKey is not set");
            }
            return;
        }
        if (! DHTSettings.ALLOW_DHT_SECURE_MESSAGE.getValue()){
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping secureMessage " + secureMessage
                        + " because secureMessages are not allowed.");
            }
            return;
        }
        secureMessageVerifier.get().verify(pubKey, CryptoUtils.SIGNATURE_ALGORITHM,
                secureMessage, smc);
    }
}
