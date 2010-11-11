/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.exceptions.IllegalSocketAddressException;
import org.limewire.mojito.handler.DefaultMessageHandler;
import org.limewire.mojito.handler.RequestHandler;
import org.limewire.mojito.handler.ResponseHandler;
import org.limewire.mojito.handler.request.FindNodeRequestHandler;
import org.limewire.mojito.handler.request.FindValueRequestHandler;
import org.limewire.mojito.handler.request.PingRequestHandler;
import org.limewire.mojito.handler.request.StatsRequestHandler;
import org.limewire.mojito.handler.request.StoreRequestHandler;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.io.Tag.Receipt;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.DHTSecureMessage;
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageFormatException;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.StatsRequest;
import org.limewire.mojito.messages.StatsResponse;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.FixedSizeHashMap;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito.util.MessageUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.util.StringUtils;


/**
 * An abstract class that takes care of all Mojito's communication needs.
 */
public abstract class MessageDispatcher {
    
    private static final Log LOG = LogFactory.getLog(MessageDispatcher.class);
    
    /** 
     * The maximum size of a serialized Message we can send.
     */
    private static final int MAX_MESSAGE_SIZE
        = NetworkSettings.MAX_MESSAGE_SIZE.getValue();
    
    /** Map of Messages (responses) we're awaiting. */
    private final ReceiptMap receiptMap = new ReceiptMap(512);
    
    /** Handle of the Context. */
    protected final Context context;
    
    private final DefaultMessageHandler defaultHandler;
    private final PingRequestHandler pingHandler;
    private final FindNodeRequestHandler findNodeHandler;
    private final FindValueRequestHandler findValueHandler;
    private final StoreRequestHandler storeHandler;
    private final StatsRequestHandler statsHandler;
    
    /**
     * Handle of the cleanup task future.
     */
    private ScheduledFuture cleanupTaskFuture;
    
    /** A list of MessageDispatcherListeners */
    private final List<MessageDispatcherListener> listeners 
        = new CopyOnWriteArrayList<MessageDispatcherListener>();
    
    public MessageDispatcher(Context context) {
        this.context = context;
        
        defaultHandler = new DefaultMessageHandler(context);
        pingHandler = new PingRequestHandler(context);
        findNodeHandler = new FindNodeRequestHandler(context);
        findValueHandler = new FindValueRequestHandler(context, findNodeHandler);
        storeHandler = new StoreRequestHandler(context);
        statsHandler = new StatsRequestHandler(context);
    }
    
    /**
     * Adds a MessageDispatcherListener.
     * <p>
     * Implementation Note: The listener(s) is not called from a 
     * separate event Thread! That means processor intensive tasks
     * that are performed straight in the listener(s) can slowdown 
     * the processing throughput significantly. Offload intensive
     * tasks to separate Threads in necessary!
     * 
     * @param l the MessageDispatcherListener instance to add
     */
    public void addMessageDispatcherListener(MessageDispatcherListener l) {
        if (l == null) {
            throw new NullPointerException("MessageDispatcherListener is null");
        }
        
        listeners.add(l);
    }
    
    /**
     * Removes a <code>MessageDispatcherListener</code>.
     * 
     * @param l the MessageDispatcherListener instance to remove
     */
    public void removeMessageDispatcherListener(MessageDispatcherListener l) {
        if (l == null) {
            throw new NullPointerException("MessageDispatcherListener is null");
        }
        
        listeners.remove(l);
    }
    
    /**
     * Binds the DatagramSocket to the given SocketAddress.
     */
    public abstract void bind(SocketAddress address) throws IOException;
    
    /**
     * Starts the MessageDispatcher.
     */
    public void start() {
        // Start the CleanupTask
    	synchronized (receiptMap) {
            if (cleanupTaskFuture == null) {
                long delay = NetworkSettings.CLEANUP_RECEIPTS_DELAY.getValue();
                
                Runnable task = new Runnable() {
                    public void run() {
                        process(new CleanupProcessor());
                    }
                };
                
                cleanupTaskFuture = context.getDHTExecutorService().scheduleWithFixedDelay(
                      task, 0L, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the MessageDispatcher.
     */
    public void stop() {
        // Stop the CleanupTask
    	synchronized (receiptMap) {
            if (cleanupTaskFuture != null) {
                cleanupTaskFuture.cancel(true);
                cleanupTaskFuture = null;
            }
        }
    }
    
    /**
     * Closes the MessageDispatcher and releases all resources.
     */
    public void close() {
        stop();
        clear();
    }
    
    /**
     * Returns whether or not incoming Requests or Responses
     * are accepted. The default implementation returns true.
     */
    public boolean isAccepting() {
        return true;
    }
    
    /**
     * Returns whether or not the MessageDispatcher is bound to a Socket.
     */
    public abstract boolean isBound();
    
    /**
     * Returns whether or not the MessageDispatcher is running.
     */
    public abstract boolean isRunning();
    
    /**
     * Sends a ResponseMessage to the given Contact.
     */
    public boolean send(Contact contact, ResponseMessage response) 
            throws IOException {
        return send(new Tag(contact, response)); 
    }
    
    /**
     * Sends a RequestMessage to the given SocketAddress and registers
     * a ResponseHandler.
     */
    public boolean send(SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(dst, request, responseHandler));
    }
    
    /**
     * Sends a RequestMessage to the given SocketAddress and registers
     * a ResponseHandler.
     */
    public boolean send(KUID nodeId, SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(nodeId, dst, request, responseHandler));
    }
    
    /**
     * Sends a RequestMessage to the given Contact and registers
     * a ResponseHandler.
     */
    public boolean send(Contact contact, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(contact, request, responseHandler));
    }
    
    /**
     * The actual send method.
     */
    protected boolean send(final Tag tag) throws IOException {
        if (!isRunning()) {
            throw new IOException("Cannot send Message because MessageDispatcher is not running");
        }
        
        KUID nodeId = tag.getNodeID();
        SocketAddress dst = tag.getSocketAddress();
        DHTMessage message = tag.getMessage();
        
        // Make sure we're not sending messages to ourself
        if (context.isLocalContactAddress(dst)) {
            // This can happen when the RouteTable was
            // initialized with Nodes from an external
            // source like a file. It's not really an
            // error because the Node's ID is different
            // but there's no point in sending the Msg.
            String msg = "Cannot send Message of type "
                + message.getClass().getName()
                + " to " + ContactUtils.toString(nodeId, dst)
                + " because it has the same contact address as the local Node "
                + context.getLocalNode() + " has";
            
            if (LOG.isInfoEnabled()) {
                LOG.info(msg);
            }
            
            process(new ErrorProcessor(tag, new IOException(msg)));
            return false;
        }
        
        // Like above. It makes no sense to send messages to a
        // Node that has our local Node ID but we have to permit
        // this case for ID collision test pings
        if (context.isLocalNodeID(nodeId) 
                && !MessageUtils.isCollisionPingRequest(
                        context.getLocalNodeID(), message)) {
            
            String msg = "Cannot send Message of type " 
                + message.getClass().getName() 
                + " to " + ContactUtils.toString(nodeId, dst) 
                + " which is equal to our local Node " 
                + context.getLocalNode();
            
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            process(new ErrorProcessor(tag, new IOException(msg)));
            return false;
        }
        
        // Check if it's a valid destination address
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            String msg = "Invalid IP:Port " + ContactUtils.toString(nodeId, dst);
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            process(new ErrorProcessor(tag, new IllegalSocketAddressException(msg)));
            return false;
        }
        
        // And make sure we're not sending messages to private
        // IPs if it's not permitted. Two Nodes behind the same 
        // NAT using private IPs to communicate with each other
        // would screw up a few things.
        if (ContactUtils.isPrivateAddress(dst)) {
            String msg = "Private IP:Port " + ContactUtils.toString(nodeId, dst);
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            process(new ErrorProcessor(tag, new IllegalSocketAddressException(msg)));
            return false;
        }
        
        // Serialize the Message
        ByteBuffer data = serialize(dst, message);
        int size = data.remaining();
        if (size <= 0) {
            process(new ErrorProcessor(tag, new IOException("Illegal Message size: " + size)));
            return false;
            
        } else if (size >= MAX_MESSAGE_SIZE) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Message (" + message.getClass().getName()  + ") is too large: " 
                    + size + " >= " + MAX_MESSAGE_SIZE);
            }
        }
        
        tag.setData(data);
        process(new SubmitProcessor(tag));
        return true;
    }
    
    /**
     * Enqueues Tag to the Output queue.
     */
    protected abstract boolean submit(Tag tag);
    
    /**
     * A helper method to serialize DHTMessage(s).
     */
    protected ByteBuffer serialize(SocketAddress dst, DHTMessage message) throws IOException {
        return context.getMessageFactory().writeMessage(dst, message);
    }
    
    /**
     * A helper method to deserialize DHTMessage(s).
     */
    protected DHTMessage deserialize(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException {
        return context.getMessageFactory().createMessage(src, data);
    }
    
    /**
     * Handles a DHTMessage as read from Network.
     */
    protected void handleMessage(DHTMessage message) {
        
        if (!isAccepting()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dropping " + message 
                        + " because MessageDispatcher is "
                        + "not accepting requests nor responses");
            }
            return;
        }

        // Make sure we're not receiving messages from ourself.
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        SocketAddress src = node.getContactAddress();
        MessageID messageId = message.getMessageID();
        
        if (context.isLocalContactAddress(src)
                || (context.isLocalNodeID(nodeId) 
                        && !(message instanceof PingResponse))) {
            
            if (LOG.isErrorEnabled()) {
                String msg = "Received a message of type " 
                    + message.getClass().getName() 
                    + " from " + node 
                    + " which is equal to our local Node " 
                    + context.getLocalNode();
                
                LOG.error(msg);
            }
            
            return;
        }
        
        if (!NetworkUtils.isValidSocketAddress(src)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " has an invalid IP:Port");
            }
            return;
        }
        
        // Make sure we're not mixing IPv4 and IPv6 addresses.
        // See RouteTableImpl.add() for more info!
        if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
            
            // Log as ERROR so that we're not missing this
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is from a different IP address space than local Node");
            }
            return;
        }
        
        fireMessageReceived(message);
        
        if (!accept(message)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping message from " + node);
            }
            
            fireMessageFiltered(message);
            return;
        }
        
        if (message instanceof ResponseMessage) {
            ResponseMessage response = (ResponseMessage)message;
            
            // The remote Node thinks it's firewalled but it responded 
            // for some odd reason which it shouldn't regardless if it
            // is really firewalled (it didn't receive our request in
            // first place) or pretends to be firewalled in which is
            // a hint that it doesn't want to be contacted. Anyways, it
            // is a bug on the other side (a Mojito compatible impl).
            if (node.isFirewalled() 
                    && NetworkSettings.DROP_RESPONE_IF_FIREWALLED.getValue()) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Dropping " + message + " because sender is firewalled");
                }
                return;
            }

            // Check the SecurityToken in the MessageID to figure out
            // whether or not we have ever sent a Request to that Host!
            if (messageId.isTaggingSupported()
                    && !messageId.isFor(src)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(response.getContact() + " sent us an unsolicited response: " + response);
                }
                return;
            }
            
            Receipt receipt = null;
            
            synchronized(receiptMap) {
                receipt = receiptMap.get(messageId);
                
                if (receipt != null) {
                    receipt.received();
            
                    // The SecurityToken check should catch all malicious
                    // and some buggy Nodes. Do some additional sanity
                    // checks to make sure the NodeID, IP:Port and 
                    // response type have the expected values.
                    if (!receipt.sanityCheck(response)) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Response from " + response.getContact() 
                                    + " did not pass the sanity check");
                        }
                        return;
                    }
                    
                    // Set the Round Trip Time (RTT)
                    message.getContact().setRoundTripTime(receipt.time());
                    
                    // OK, all checks passed. We can remove the receipt now!
                    receiptMap.remove(messageId);
                }
            }
            
            // If it's a late response (the Receipt got evicted by the
            // cleanup task) and tagging of MessageIDs is NOT supported
            // than exit here as we're not able to verify if we've ever
            // sent a request to the given Node. In other words it's an
            // unsolicited response from our perspective!
            if (receipt == null && !messageId.isTaggingSupported()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(response.getContact() + " sent us an unsolicited response: " + response);
                }
                return;
            }
            
            processResponse(receipt, response);
            
        } else if (message instanceof RequestMessage) {
            
            // A Node that is marked as firewalled must not respond
            // to REQUESTS!
            if (context.getLocalNode().isFirewalled()
                    && NetworkSettings.DROP_REQUEST_IF_FIREWALLED.getValue()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Local Node is firewalled, dropping " + message);
                }
            } else {
                processRequest((RequestMessage)message);
            }
            
        } else {
            throw new IllegalArgumentException(message 
                    + " is neither instance of RequestMessage nor ResponseMessage!");
        }
    }
    
    /**
     * Starts a new ResponseProcessor.
     */
    private void processResponse(Receipt receipt, ResponseMessage response) {
        ResponseProcessor processor = new ResponseProcessor(receipt, response);
        if (response instanceof DHTSecureMessage) {
            verify((DHTSecureMessage)response, processor);
        } else {
            process(processor);
        }
    }
    
    /**
     * Starts a new RequestProcessor.
     */
    private void processRequest(RequestMessage request) {
        RequestProcessor processor = new RequestProcessor(request);
        if (request instanceof DHTSecureMessage) {
            verify((DHTSecureMessage)request, processor);
        } else {
            process(processor);
        }
    }
    
    /**
     * Called right after a Message has been sent to register
     * its ResponseHandler (if it's a RequestMessage).
     */
    protected void register(Tag tag) {
        Receipt receipt = tag.receipt();
        if (receipt != null) {
            process(new RegisterProcessor(receipt));
        }
        
        fireMessageSent(tag.getNodeID(), tag.getSocketAddress(), tag.getMessage());
    }
    
    /** Called to process a Task */
    protected abstract void process(Runnable runnable);
    
    /** Called to verify a SecureMessage */
    protected abstract void verify(SecureMessage secureMessage, SecureMessageCallback smc);
    
    /**
     * This method is called from the MessageDispatcher Thread to
     * determinate whether or not the DHTMessage should be accepted.
     * As it's running on the MessageDispatcher Thread it shouldn't
     * block for too long. Use the allow() method for heavyweight
     * checks!
     */
    protected boolean accept(DHTMessage message) {
        return true;
    }
    
    /** 
     * This method is called to determinate whether or not the
     * DHTMessage should be processed. 
     */
    protected boolean allow(DHTMessage message) {
        HostFilter hostFilter = context.getHostFilter();
        if (hostFilter != null) {
            return hostFilter.allow(message.getContact().getContactAddress());
        }
        return true;
    }
    
    /**
     * Clears the output queue and receipt map.
     */
    protected void clear() {
        synchronized (receiptMap) {
            receiptMap.clear();
        }
    }
    
    protected void handleError(Tag tag, IOException err) {
        process(new ErrorProcessor(tag, err));
    }
    
    protected void fireMessageSent(KUID nodeId, SocketAddress dst, DHTMessage message) {
        fireMessageDispatcherEvent(nodeId, dst, message, EventType.MESSAGE_SENT);
    }
    
    protected void fireMessageReceived(DHTMessage message) {
        fireMessageDispatcherEvent(null, null, message, EventType.MESSAGE_RECEIVED);
    }
    
    protected void fireMessageFiltered(DHTMessage message) {
        fireMessageDispatcherEvent(null, null, message, EventType.MESSAGE_FILTERED);
    }
    
    protected void fireLateResponse(DHTMessage message) {
        fireMessageDispatcherEvent(null, null, message, EventType.LATE_RESPONSE);
    }
    
    protected void fireReceiptTimeout(Receipt receipt) {
        fireMessageDispatcherEvent(receipt.getNodeID(), receipt.getSocketAddress(), 
                receipt.getRequestMessage(), EventType.RECEIPT_TIMEOUT);
    }
    
    protected void fireReceiptEvicted(Receipt receipt) {
        fireMessageDispatcherEvent(receipt.getNodeID(), receipt.getSocketAddress(), 
                receipt.getRequestMessage(), EventType.RECEIPT_EVICTED);
    }
    
    protected void fireMessageDispatcherEvent(KUID nodeId, SocketAddress dst, 
            DHTMessage message, EventType type) {
        if (listeners.isEmpty()) {
            return;
        }
        
        MessageDispatcherEvent evt = new MessageDispatcherEvent(
                this, nodeId, dst, message, type);
        
        for (MessageDispatcherListener listener : listeners) {
            listener.handleMessageDispatcherEvent(evt);
        }
    }
    
    /**
     * A map of MessageID -> Receipts.
     */
    @SuppressWarnings("serial")
    private class ReceiptMap extends FixedSizeHashMap<MessageID, Receipt> {
        
        public ReceiptMap(int maxSize) {
            super(maxSize);
        }

        public void add(Receipt receipt) {
            put(receipt.getMessageID(), receipt);
        }
        
        /**
         * Cleans up the Map and kicks off Ticks. Meant to be
         * called by a scheduled task.
         */
        public void cleanup() {
            for(Iterator<Receipt> it = values().iterator(); it.hasNext(); ) {
                Receipt receipt = it.next();
                
                if (receipt.isCancelled()) {
                    // The user cancelled the Future
                    it.remove();
                	
                } else if (receipt.timeout()) {
                    receipt.received();
                    it.remove();
                    
                    process(new TimeoutProcessor(receipt, true));
                } else {
                    process(new TickProcessor(receipt));
                }
            }
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<MessageID, Receipt> eldest) {
            Receipt receipt = eldest.getValue();
            
            boolean timeout = receipt.timeout();
            if (super.removeEldestEntry(eldest) || timeout) {
                receipt.received();
                process(new TimeoutProcessor(receipt, timeout));
                return true;
            }
            return false;
        }
    }
    
    /**
     * Calls submit(Tag) from the processor Thread.
     */
    private class SubmitProcessor implements Runnable {
        
        private final Tag tag;
        
        private SubmitProcessor(Tag tag) {
            this.tag = tag;
        }
        
        public void run() {
            submit(tag);
        }
    }
    
    /**
     * Calls ReceiptMap.add(Receipt) from the processor Thread.
     */
    private class RegisterProcessor implements Runnable {
        
        private final Receipt receipt;
        
        private RegisterProcessor(Receipt receipt) {
            this.receipt = receipt;
        }
        
        public void run() {
            synchronized (receiptMap) {
                receiptMap.add(receipt);                
            }
        }
    }
    
    /**
     * Calls ReceiptMap.cleanup() from the processor Thread.
     */
    private class CleanupProcessor implements Runnable {
        
        public void run() {
            synchronized (receiptMap) {
                receiptMap.cleanup();                
            }
        }
    }
    
    /**
     * An implementation of Runnable to handle Response Messages.
     */
    private class ResponseProcessor implements Runnable, SecureMessageCallback {
        
        private final Receipt receipt;
        private final ResponseMessage response;
        
        private ResponseProcessor(Receipt receipt, ResponseMessage response) {
            this.receipt = receipt;
            this.response = response;
        }
        
        public void handleSecureMessage(SecureMessage sm, boolean passed) {
            if (passed) {
                process(this);
            } else if (LOG.isErrorEnabled()) {
                LOG.error(response.getContact() 
                        + " send us a secure response Message but the signatures do not match!");
            }
        }
        
        public void run() {
            if (receipt != null) {
                processResponse();
            } else {
                processLateResponse();
            }
        }
        
        /**
         * Processes a regular response.
         */
        private void processResponse() {
            try {
                defaultHandler.handleResponse(response, receipt.time());
                receipt.getResponseHandler().handleResponse(response, receipt.time());
            } catch (IOException e) {
                receipt.handleError(e);
                LOG.error("An error occured dusring processing the response", e);
            }
        }
        
        /**
         * A late response is a response that arrived after a timeout.
         * We rely on the fact that MessageIDs are tagged with a AddressSecurityToken
         * so that we can still figure out if we've ever send a request
         * to the remote Node.
         * <p>
         * The fact that the remote Node respond is a valuable information
         * and we'll use a higher timeout next time.
         */
        private void processLateResponse() {
            
            Contact node = response.getContact();
            
            if (LOG.isTraceEnabled()) {
                if (response instanceof PingResponse) {
                    LOG.trace("Received a late Pong from " + node);
                } else if (response instanceof FindNodeResponse) {
                    LOG.trace("Received a late FindNode response from " + node);
                } else if (response instanceof FindValueResponse) {
                    LOG.trace("Received a late FindValue response from " + node);
                } else if (response instanceof StoreResponse) {
                    LOG.trace("Received a late Store response from " + node);
                } else if (response instanceof StatsResponse) {
                    LOG.trace("Received a late Stats response from " + node);
                }
            }
            
            fireLateResponse(response);
            
            defaultHandler.handleLateResponse(response);
        }
    }
    
    /**
     * An implementation of Runnable to handle Request Messages.
     */
    private class RequestProcessor implements Runnable, SecureMessageCallback {
        
        private final RequestMessage request;
        
        private RequestProcessor(RequestMessage request) {
            this.request = request;
        }
        
        public void handleSecureMessage(SecureMessage sm, boolean passed) {
            if (passed) {
                process(this);
            } else if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " send us a secure request Message but the signatures do not match!");
            }
        }
        
        public void run() {
            // Make sure a singe Node cannot monopolize our resources
            if (!allow(request)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(request.getContact() + " refused");
                }
                
                fireMessageFiltered(request);
                return;
            }
            
            RequestHandler requestHandler = null;
            
            if (request instanceof PingRequest) {
                requestHandler = pingHandler;
            } else if (request instanceof FindNodeRequest) {
                requestHandler = findNodeHandler;
            } else if (request instanceof FindValueRequest) {
                requestHandler = findValueHandler;
            } else if (request instanceof StoreRequest) {
                requestHandler = storeHandler;
            } else if (request instanceof StatsRequest) {
                requestHandler = statsHandler;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("There is no handler for " + request);
                }
            }
            
            if (requestHandler != null) {
                try {
                    // Call the default handler after the custom handler
                    // for two reasons: The first reason is security, the
                    // custom handler can do message specific checks and
                    // can throw an Exception if something is fishy and the
                    // second reason is a small optimization (return under
                    // certain conditions k-closest Nodes without the
                    // requester).
                    requestHandler.handleRequest(request);
                    defaultHandler.handleRequest(request);
                } catch (IOException e) {
                    LOG.error("An exception occured during processing the request", e);
                }
            }
        }
    }
    
    /**
     * An implementation of Runnable to handle Timeouts. The eviction
     * of ResponseHandlers (we send too many requests and we hit the
     * ReceiptMap limit) is also treated as a timeout.
     */
    private class TimeoutProcessor implements Runnable {
        
        private final Receipt receipt;
        
        private final boolean timeout;
        
        private TimeoutProcessor(Receipt receipt, boolean timeout) {
            this.receipt = receipt;
            this.timeout = timeout;
        }
        
        public void run() {
            
            if (timeout) {
                fireReceiptTimeout(receipt);
            } else {
                fireReceiptEvicted(receipt);
            }
            
            try {
                KUID nodeId = receipt.getNodeID();
                SocketAddress dst = receipt.getSocketAddress();
                RequestMessage msg = receipt.getRequestMessage();
                long time = receipt.time();
                
                defaultHandler.handleTimeout(nodeId, dst, msg, time);
                receipt.getResponseHandler().handleTimeout(nodeId, dst, msg, time);
            } catch (IOException e) {
                receipt.handleError(e);
                LOG.error("ReceiptMap removeEldestEntry error: ", e);
            }
        }
    }
    
    /**
     * An implementation of Runnable to handle Ticks.
     */
    private static class TickProcessor implements Runnable {
        
        private final Receipt receipt;
        
        private TickProcessor(Receipt receipt) {
            this.receipt = receipt;
        }
        
        public void run() {
            receipt.handleTick();
        }
    }
    
    /**
     * An implementation of Runnable to handle Errors.
     */
    private static class ErrorProcessor implements Runnable {
        
        private final Tag tag;
        
        private final IOException exception;
        
        private ErrorProcessor(Tag tag, IOException exception) {
            this.tag = tag;
            this.exception = exception;
        }
        
        public void run() {
            tag.handleError(exception);
        }
    }
    
    /**
     * The MessageDispatcherListener is called for every send or 
     * received Message.
     */
    public static interface MessageDispatcherListener {
        
        /**
         * Invoked when an event occurs.
         * 
         * @param evt the event that occurred
         */
        public void handleMessageDispatcherEvent(MessageDispatcherEvent evt);
    }
    
    /**
     * MessageDispatcherEvent are created and fired for various MessageDispatcher events.
     */
    public static class MessageDispatcherEvent {
        
        public static enum EventType {
            
            /** Fired if a DHTMessage was send */
            MESSAGE_SENT,
            
            /** Fired if a DHTMessage was received */
            MESSAGE_RECEIVED,
            
            /** Fired if a DHTMessage filtered */
            MESSAGE_FILTERED,
            
            /** Fired if a request timed out */
            RECEIPT_TIMEOUT,
            
            /** Fired if a request was evicted */
            RECEIPT_EVICTED,
            
            /** Fired if a late response was received */
            LATE_RESPONSE;
        }
        
        private MessageDispatcher messageDispatcher;
        
        private KUID nodeId;
        
        private SocketAddress dst;
        
        private DHTMessage message;
        
        private EventType type;
        
        public MessageDispatcherEvent(MessageDispatcher messageDispatcher, 
                KUID nodeId, SocketAddress dst, DHTMessage message, EventType type) {
            this.nodeId = nodeId;
            this.dst = dst;
            this.message = message;
            this.type = type;
        }
        
        public MessageDispatcher getMessageDispatcher() {
            return messageDispatcher;
        }
        
        public KUID getNodeID() {
            return nodeId;
        }
        
        public SocketAddress getSocketAddress() {
            return dst;
        }
        
        public DHTMessage getMessage() {
            return message;
        }
        
        public EventType getEventType() {
            return type;
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this, nodeId, dst, message, type);
        }
      
    }
}
