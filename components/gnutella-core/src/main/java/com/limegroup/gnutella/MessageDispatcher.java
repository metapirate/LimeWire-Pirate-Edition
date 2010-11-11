package com.limegroup.gnutella;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.Message;

/**
 * Dispatches messages to the MessageRouter.
 */
@Singleton
public class MessageDispatcher {
    
    private final Executor DISPATCH;

    private final MessageRouter messageRouter;
    
    private final EventBroadcaster<MessageSentEvent> messageSentEventBroadcaster;
    
    @Inject
    public MessageDispatcher(MessageRouter messageRouter, @Named("messageExecutor") Executor dispatch,
            EventBroadcaster<MessageSentEvent> messageSentEventBroadcaster) {
        this.messageRouter = messageRouter;
        this.DISPATCH = dispatch;
        this.messageSentEventBroadcaster = messageSentEventBroadcaster;
    }
    
    /** Dispatches a runnable, to allow arbitrary runnables to be processed on the message thread. */
    public void dispatch(Runnable r) {
        DISPATCH.execute(r);
    }
    
    /**
     * Dispatches a UDP message.
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new UDPDispatch(messageRouter, m, addr));
    }
    
    /**
     * Dispatches a Multicast message.
     */
    public void dispatchMulticast(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new MulticastDispatch(messageRouter, m, addr));
    }
    
    /**
     * Dispatches a TCP message.
     */
    public void dispatchTCP(Message m, RoutedConnection conn) {
        DISPATCH.execute(new TCPDispatch(messageRouter, m, conn));
    }
    
    /**
     * Dispatches the event that <code>message</code> was sent to <code>routedConnection</code>. 
     */
    public void dispatchTCPMessageSent(Message message, RoutedConnection routedConnection) {
        messageSentEventBroadcaster.broadcast(new MessageSentEvent(routedConnection, message));        
    }
    
    private static abstract class Dispatch implements Runnable {
        protected final MessageRouter messageRouter;
        protected final Message m;
        
        Dispatch(MessageRouter messageRouter, Message m) {
            this.messageRouter = messageRouter;
            this.m = m;
        }
        
        public void run() {
            dispatch();
        }
        
        protected abstract void dispatch();
    }
    
    private static class UDPDispatch extends Dispatch {
        
        private final InetSocketAddress addr;

        UDPDispatch(MessageRouter messageRouter, 
                Message m, 
                InetSocketAddress addr) {
            super(messageRouter, m);
            this.addr = addr;
        }

        @Override
        protected void dispatch() {
            messageRouter.handleUDPMessage(m, addr);
        }
    }
    
    private static class MulticastDispatch extends Dispatch {
        
        private final InetSocketAddress addr;
        
        MulticastDispatch(MessageRouter messageRouter, 
                Message m, 
                InetSocketAddress addr) {
            super(messageRouter, m);
            this.addr = addr;
        }
        
        @Override
        protected void dispatch() {
            messageRouter.handleMulticastMessage(m, addr);
        }
    }
    
    private static class TCPDispatch extends Dispatch {
        
        private final RoutedConnection conn;
        
        TCPDispatch(MessageRouter messageRouter, 
                Message m, 
                RoutedConnection conn) {
            super(messageRouter, m);
            this.conn = conn;
        }
        
        @Override
        protected void dispatch() {
            messageRouter.handleMessage(m, conn);
        }
    }

}