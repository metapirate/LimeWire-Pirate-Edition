package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.BypassedResultsCache;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

/**
 * Handles {@link ReplyNumberVendorMessage} and {@link QueryReply} for 
 * out-of-band search results and manages a cache of session objects 
 * to keep track of the results that have alreay been received.
 */
@Singleton
public class OOBHandler implements MessageHandler, Runnable {
    
    private static final Log LOG = LogFactory.getLog(OOBHandler.class);
    
    /** How long to remember the port associated with each address */
    private static final int RESPONDER_PORT_LIFETIME = 60 * 1000;
    /** How long to remember ignored addresses */
    private static final int IGNORED_ADDRESS_LIFETIME = 10 * 60 * 1000;
    /** Magic port number that means an address should be ignored */
    private static final int IGNORE = -1;
    /** Don't ignore localhost (used for testing) */
    private static final int LOCALHOST =
        ByteUtils.leb2int(new byte[]{127, 0, 0, 1}, 0);

	private final MessageRouter router;
	
	private final MACCalculatorRepositoryManager MACCalculatorRepositoryManager;
    
    private final ScheduledExecutorService executor;
    
    private final OutOfBandStatistics outOfBandStatistics;
    
    private final NetworkInstanceUtils networkInstanceUtils;
	
    private final Map<Integer,OOBSession> sessions =
        Collections.synchronizedMap(new HashMap<Integer,OOBSession>());
    
    /**
     * The port associated with each responding address and the time at which
     * it was recorded. This is used to detect addresses that respond from
     * multiple ports and also to record misbehaving addresses; if the port
     * is IGNORE, RNVMs from the address should be ignored.
     */
    private final Map<Integer,ResponderPort> responderPorts =
        Collections.synchronizedMap(new HashMap<Integer,ResponderPort>());
    
    @Inject
	public OOBHandler(MessageRouter router, 
            MACCalculatorRepositoryManager MACCalculatorRepositoryManager,
            @Named("backgroundExecutor") ScheduledExecutorService executor,
            OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
		this.router = router;
		this.MACCalculatorRepositoryManager = MACCalculatorRepositoryManager;
        this.executor = executor;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
	}

	public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
		if (msg instanceof ReplyNumberVendorMessage)
			handleRNVM((ReplyNumberVendorMessage)msg, handler);
		else if (msg instanceof QueryReply)
			handleOOBReply((QueryReply)msg, handler);
		else 
			throw new IllegalArgumentException("can't handle this type of message");
	}
	
    /**
     * Handles the reply number message, verifying the query for it is still alive
     * and more results are wanted and sending a {@link LimeACKVendorMessage} in
     * that case. Otherwise the source of the <code>msg</code> is added to the 
     * {@link BypassedResultsCache}.
     */
	private void handleRNVM(ReplyNumberVendorMessage msg, final ReplyHandler handler) {
		GUID g = new GUID(msg.getGUID());

        if(LOG.isDebugEnabled()) {
            LOG.debug("Received RNVM from " + handler.getAddress() +
                    ":" + handler.getPort() +
                    " with " + msg.getNumResults() + " results");
        }
        
        // Only allow responses from one port per address
        byte[] handlerAddress = handler.getInetAddress().getAddress();
        if(shouldIgnore (handlerAddress, handler.getPort()))
            return;
        
        int toRequest;
        
        if(!router.isQueryAlive(g) ||
                (toRequest = router.getNumOOBToRequest(msg)) <= 0) {
            // remember as possible GUESS source though
            LOG.debug("Bypassing source");
            router.addBypassedSource(msg, handler);
            outOfBandStatistics.addBypassedResponse(msg.getNumResults());
            return;
        }
				
		LimeACKVendorMessage ack = null;
        if (msg.isOOBv3()) {
            SecurityToken t = new OOBSecurityToken(new OOBSecurityToken.OOBTokenData(handler, msg.getGUID(), toRequest), 
                    MACCalculatorRepositoryManager); 
            int hash = Arrays.hashCode(t.getBytes());
            synchronized(sessions) {
                if(!sessions.containsKey(hash)) {
                    sessions.put(hash, new OOBSession(t, toRequest, new GUID(msg.getGUID())));
                    ack = new LimeACKVendorMessage(g, toRequest, t);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Sending OOBv3 LimeACK to " +
                                handler.getAddress() + ":" + handler.getPort());
                    }
                } else {
                    LOG.debug("RNVM has already been acked");
                }
            }
        } else {
            ack = new LimeACKVendorMessage(g, toRequest);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Sending OOBv2 LimeACK to " +
                        handler.getAddress() + ":" + handler.getPort());
            }
        }
        
        if (ack != null) {
            outOfBandStatistics.addRequestedResponse(toRequest);
            handler.reply(ack);
            if (MessageSettings.OOB_REDUNDANCY.getValue()) {
                LOG.debug("Sending redundant LimeACK");
                final LimeACKVendorMessage ackf = ack;
                executor.schedule(new Runnable() {
                    public void run() {
                        handler.reply(ackf);
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }
        }
	}
    
    /**
     * Handles an out-of-band query reply verifying if its security token is valid
     * and creating a session object that keeps track of the number of results
     * received for that security token.
     * 
     * Invalid messages with invalid security token or without token or duplicate
     * messages are ignored.
     * 
     * If the query is not alive messages are discarded and added to the
     *  {@link BypassedResultsCache}.
     */
    private void handleOOBReply(QueryReply reply, ReplyHandler handler) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Handling OOB reply from " + handler.getAddress() +
                    ":" + handler.getPort() +
                    " with " + reply.getResultCount() + " results");
        }
        
        // check if ip address of reply and sender of reply match
        // and update address of reply if necessary
        byte[] handlerAddress = handler.getInetAddress().getAddress();
        if (!Arrays.equals(handlerAddress, reply.getIPBytes())) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Reply has wrong address " + reply.getIP() +
                        ":" + reply.getPort());
            }
            // override address in packet
            try {
                // needs a push, we can update: works for fw-fw case and classic push
                // or not private, we can update
                if (reply.getNeedsPush() || !networkInstanceUtils.isPrivateAddress(reply.getIPBytes())) {
                    reply.setOOBAddress(handler.getInetAddress(), handler.getPort());
                }
                else {
                    // messed up case: doesn't want a push, but has a private address
                }
            }
            catch (BadPacketException bpe) {
                // invalid packet, don't handle it
                LOG.debug("Error overriding address");
                return;
            }
        }
        
        SecurityToken token = null;
        try {
            token = getVerifiedSecurityToken(reply, handler);
        } catch(InvalidSecurityTokenException e) {
            LOG.debug("Invalid security token");
            return;
        }
        if(token == null) {
            LOG.debug("No security token");
            if (!SearchSettings.DISABLE_OOB_V2.getBoolean()) {
                LOG.debug("Handling as an OOBv2 reply");
                router.handleQueryReply(reply, handler);
            }
            return;
        }
        
        int numResps = reply.getResultCount();
        outOfBandStatistics.addReceivedResponse(numResps);
        
        /*
         * Router will handle the reply if it
         * it has a route && we still expect results for this OOB session
         */
        // if query is not of interest anymore return
        GUID queryGUID = new GUID(reply.getGUID());
        if (!router.isQueryAlive(queryGUID)) {
            LOG.debug("Query is dead - bypassing source");
            router.addBypassedSource(reply, handler);
        }
        else {
            synchronized(sessions) {
                int hashKey = Arrays.hashCode(token.getBytes());
                OOBSession session = sessions.get(hashKey);
                if(session == null) {
                    LOG.debug("Query is alive but OOB session has expired");
                    return;
                }

                int remaining = session.getRemainingResultsCount() - numResps;
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Reply has " + numResps + " results, " +
                            remaining + " remaining");
                }
                if(remaining >= 0) {
                    // parsing of query reply already done here in message dispatcher thread
                    try {
                        int added = session.countAddedResponses(reply.getResultsArray());
                        if(LOG.isDebugEnabled())
                            LOG.debug("Reply has " + added + " new results");                        
                        if(added > 0) {
                            LOG.debug("Handling as an OOBv3 reply");
                            router.handleQueryReply(reply, handler);
                        }
                    } 
                    catch (BadPacketException e) {
                        LOG.debug("Error getting results");
                        // ignore packet
                    }
                } else {
                    tooManyResults(handlerAddress);
                }
            }
        }
    }
    
    /**
     * Returns true if a message from the given address and port
     * should be ignored because the address is responding from
     * multiple ports.
     */
    private boolean shouldIgnore(byte[] addr, int port) {
        if(!SearchSettings.OOB_IGNORE_MULTIPLE_PORTS.getValue())
            return false;
        Integer address = ByteUtils.leb2int(addr, 0);
        if(address == LOCALHOST)
            return false;
        long now = System.currentTimeMillis();
        synchronized(responderPorts) {
            ResponderPort rp = responderPorts.get(address);
            if(rp == null || rp.hasExpired(now)) {
                // No port is known for this address
                rp = new ResponderPort(port, now);
                responderPorts.put(address, rp);
                return false;
            } else if(rp.port == IGNORE) {
                // Continue ignoring the address
                rp.timestamp = now;
                return true;
            } else if(rp.port != port) {
                if(LOG.isInfoEnabled()) {
                    String ip = NetworkUtils.ip2string(addr);
                    LOG.info("Ignoring " + ip + " - too many ports");
                }
                // Too many ports - ignore the address for a while
                rp = new ResponderPort(IGNORE, now);
                responderPorts.put(address, rp);
                return true;
            }
            else {
                // Same port as before
                return false;
            }
        }
    }
    
    /**
     * Ignores an address that sent more results than it offered.
     */
    private void tooManyResults(byte[] addr) {
        if(!SearchSettings.OOB_IGNORE_EXCESS_RESULTS.getValue())
            return;
        Integer address = ByteUtils.leb2int(addr, 0);
        long now = System.currentTimeMillis();
        synchronized(responderPorts) {
            ResponderPort rp = responderPorts.get(address);
            if(rp == null || rp.port != IGNORE) {
                if(LOG.isInfoEnabled()) {
                    String ip = NetworkUtils.ip2string(addr);
                    LOG.info("Ignoring " + ip + " - too many results");
                }
                // Too many results - ignore the address for a while
                rp = new ResponderPort(IGNORE, now);
                responderPorts.put(address, rp);
            } else {
                // Continue ignoring the address
                rp.timestamp = now;
            }
        }
    }
    
    /**
     * Reconstructs the security token from the query reply and verifies it
     * against the handler, the number of results requested and the GUID of
     * the reply.
     *
     * @return the security token, or null if there is no security token
     * @throws InvalidSecurityTokenException if the security token is invalid
     */
    private SecurityToken getVerifiedSecurityToken(QueryReply reply, ReplyHandler handler)
    throws InvalidSecurityTokenException {
        byte[] securityBytes = reply.getSecurityToken();
        if(securityBytes == null)
            return null;
        OOBSecurityToken oobKey = new OOBSecurityToken(securityBytes,
                MACCalculatorRepositoryManager);
        OOBSecurityToken.OOBTokenData data = 
            new OOBSecurityToken.OOBTokenData(handler, reply.getGUID(),
                    securityBytes[0] & 0xFF);
        if(oobKey.isFor(data))
            return oobKey;
        else
            throw new InvalidSecurityTokenException("invalid token");
    }

	private void expire() {
	    synchronized(sessions) {
            if(LOG.isDebugEnabled())
                LOG.debug(sessions.size() + " OOB sessions");
	        Iterator<Map.Entry<Integer,OOBSession>> iter =
                sessions.entrySet().iterator();
	        while(iter.hasNext()) {
	            if(!router.isQueryAlive(iter.next().getValue().getGUID()))
	                iter.remove();
	        }
        }
        long now = System.currentTimeMillis();
        synchronized(responderPorts) {
            if(LOG.isDebugEnabled())
                LOG.debug(responderPorts.size() + " responder ports");
            Iterator<Map.Entry<Integer,ResponderPort>> iter =
                responderPorts.entrySet().iterator();
            while(iter.hasNext()) {
                if(iter.next().getValue().hasExpired(now))
                    iter.remove();
            }
	    }
	}
	
	public void run() {
		expire();
	}
    
    private static class ResponderPort {
        final int port;
        long timestamp;
        
        ResponderPort(int port, long timestamp) {
            this.port = port;
            this.timestamp = timestamp;
        }
        
        boolean hasExpired(long now) {
            if(port == IGNORE)
                return now - timestamp > IGNORED_ADDRESS_LIFETIME;
            else
                return now - timestamp > RESPONDER_PORT_LIFETIME;
        }
    }
}
