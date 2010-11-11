package com.limegroup.gnutella;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Cancellable;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * Sends Gnutella messages via UDP to a set of hosts and calls back to a 
 * listener whenever responses are returned.
 */
public class UDPPingerImpl implements UDPPinger {
    
    private static final Log LOG = LogFactory.getLog(UDPPingerImpl.class);
        
    protected static final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("UDPHostRanker");
        
    /**
     * The time to wait before expiring a message listener.
     *
     * Non-final for testing.
     */
    public static int DEFAULT_LISTEN_EXPIRE_TIME = 20 * 1000;
    
    /** Send pings every this often */
    private static final long SEND_INTERVAL = 500;
    
    /** Send this many pings each time */
    private static final int MAX_SENDS = 15;
    
    /**
     * The current number of datagrams we've sent in the past 500 milliseconds.
     */
    private static int _sentAmount;
    
    /**
     * The last time we sent a datagram.
     */
    private static long _lastSentTime;
    
    private final Provider<MessageRouter> messageRouter;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<UDPService> udpService;

    private final PingRequestFactory pingRequestFactory;
    
    @Inject
    public UDPPingerImpl(Provider<MessageRouter> messageRouter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<UDPService> udpService,
            PingRequestFactory pingRequestFactory) {
        this.messageRouter = messageRouter;
        this.backgroundExecutor = backgroundExecutor;
        this.udpService = udpService;
        this.pingRequestFactory = pingRequestFactory;
    }

    public void rank(Collection<? extends IpPort> hosts, Cancellable canceller) {
        rank(hosts, null, canceller, null);
    }
    
    public void rank(final Collection<? extends IpPort> hosts,
                            final MessageListener listener,
                            Cancellable canceller,
                            final Message message) {
        rank(hosts, listener, canceller, message, -1);
    }
    
    public void rank(final Collection<? extends IpPort> hosts,
                            final MessageListener listener,
                            Cancellable canceller,
                            final Message message,
                            int expireTime) {
        if(hosts == null)
            throw new NullPointerException("null hosts not allowed");
        if(canceller == null) {
            canceller = new Cancellable() {
                public boolean isCancelled() { return false; }
            };
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Adding hosts "+hosts+" with message: "+message+" to processing queue");
        }
        
        SenderBundle bundle;
        if(expireTime > 0) {
            bundle = new SenderBundle(hosts, listener, canceller, message, expireTime);
        } else {
            bundle = new SenderBundle(hosts, listener, canceller, message, DEFAULT_LISTEN_EXPIRE_TIME);
        }
        
        bundle.validateHosts();
        
        QUEUE.execute(bundle);
    }
    
    /**
     * Waits for UDP listening to be activated.
     */
    private boolean waitForListening(Cancellable canceller) {
        int waits = 0;
        while(!udpService.get().isListening() && waits < 10 &&
              !canceller.isCancelled()) {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                // Should never happen.
                ErrorService.error(e);
            }
            waits++;
        }
        
        return waits < 10;
    }
        
    /**
     * Sends the given send bundle.
     */
    protected void send(Collection<? extends IpPort> hosts, 
            final MessageListener listener,
            Cancellable canceller,
            Message message, int expireTime) {
        
        // something went wrong with UDPService - don't try to send
        if (!waitForListening(canceller))
            return;
    
        if(message == null)
            message = pingRequestFactory.createUDPPing();
            
        final byte[] messageGUID = message.getGUID();
        
        if (listener != null)
            messageRouter.get().registerMessageListener(messageGUID, listener);

        
        for(IpPort ipp : hosts) {
            if(canceller.isCancelled())
                break;
            sendSingleMessage(ipp, message);
        }

        // also take care of any MessageListeners
        if (listener != null) {
            // Now schedule a runnable that will remove the mapping for the GUID
            // of the above message after 20 seconds so that we don't store it 
            // indefinitely in memory for no reason.
            Runnable udpMessagePurger = new Runnable() {
                    public void run() {
                        messageRouter.get().unregisterMessageListener(messageGUID, listener);
                    }
                };
         
            // Purge after 20 seconds.
            backgroundExecutor.schedule(udpMessagePurger, expireTime, TimeUnit.MILLISECONDS);
        }
    }
    
    protected void sendSingleMessage(IpPort host, Message message) {
        
        long now = System.currentTimeMillis();
        if(now > _lastSentTime + SEND_INTERVAL) {
            _sentAmount = 0;
        } else if(_sentAmount == MAX_SENDS) {
            try {
                Thread.sleep(SEND_INTERVAL);
                now = System.currentTimeMillis();
            } catch(InterruptedException ignored) {}
            _sentAmount = 0;
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace("Sending to " + host + ": " + message.getClass()+" "+message);
        udpService.get().send(message, host);
        _sentAmount++;
        _lastSentTime = now;
    }
    
    /**
     * Simple bundle that can send itself.
     */
    private class SenderBundle implements Runnable {
        private final Collection<? extends IpPort> hosts;
        private final MessageListener listener;
        private final Cancellable canceller;
        private final Message message;
        private final int expireTime;

        public SenderBundle(Collection<? extends IpPort> hosts, MessageListener listener,
                Cancellable canceller, Message message, int expireTime) {
            this.hosts = hosts;
            this.listener = listener;
            this.canceller = canceller;
            this.message = message;
            this.expireTime = expireTime;
        }

        public void run() {
            send(hosts,listener,canceller,message,expireTime);
        }
        
        /**
         * Runs through all the hosts and throws an IllegalArgumentException
         * if any host is invalid.
         */
        void validateHosts() {
            for(IpPort host : hosts) {
                // check avoids address re-resolution  
                if(!NetworkUtils.isValidAddressAndPort(host.getInetAddress().getAddress(), host.getPort()))
                    throw new IllegalArgumentException("invalid host: " + host);
            }
        }
    }
}
