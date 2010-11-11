package com.limegroup.gnutella.messagehandlers;

import static com.limegroup.gnutella.MessageRouter.CLEAR_TIME;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.MessageSettings;
import org.limewire.io.GUID;
import org.limewire.security.SecurityToken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;

@Singleton
public class LimeACKHandler implements MessageHandler {
    
    
    /**
     * The lifetime of OOBs guids.
     */
    private static final long TIMED_GUID_LIFETIME = 25 * 1000;
    
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<UDPService> udpService;
    
    @Inject
    public LimeACKHandler(@Named("backgroundExecutor")ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter,
            Provider<UDPService> udpService) {
        this.backgroundExecutor = backgroundExecutor;
        this.messageRouter = messageRouter;
        this.udpService = udpService;
    }
    
    @Inject
    void scheduleExpirer() {
        backgroundExecutor.scheduleWithFixedDelay(new Expirer(), CLEAR_TIME, CLEAR_TIME, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Keeps track of QueryReplies to be sent after receiving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryResponseBundle.
     */
    private final Map<GUID.TimedGUID, QueryResponseBundle> _outOfBandReplies =
        new Hashtable<GUID.TimedGUID, QueryResponseBundle>();

    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        LimeACKVendorMessage ack = (LimeACKVendorMessage)msg;
        GUID.TimedGUID refGUID = new GUID.TimedGUID(new GUID(ack.getGUID()),
                TIMED_GUID_LIFETIME);
        QueryResponseBundle bundle = _outOfBandReplies.remove(refGUID);

        // token is null for old oob messages, it will just be ignored then
        SecurityToken securityToken = ack.getSecurityToken();

        if ((bundle != null) && (ack.getNumResults() > 0)) {
            final InetAddress iaddr = addr.getAddress();
            final int port = addr.getPort();

          // convert responses to QueryReplies, but only send as many as the
          // node wants
            Iterable<QueryReply> iterable;
            if (ack.getNumResults() < bundle.responses.length) {
              // TODO move selection to responseToQueryReplies methods for randomization
                Response[] desired = new Response[ack.getNumResults()];
                System.arraycopy(bundle.responses, 0, desired, 0, desired.length);
                iterable = messageRouter.get().responsesToQueryReplies(desired, bundle._query, 1, securityToken);
            } else { 
                iterable = messageRouter.get().responsesToQueryReplies(bundle.responses, 
                        bundle._query, 1, securityToken);
            }

          // send the query replies
            int i = 0;
            for(final QueryReply queryReply : iterable) {
                backgroundExecutor.schedule(new Runnable() {
                    public void run () {
                        udpService.get().send(queryReply, iaddr, port);
                    }
                }, (i++) * 200, TimeUnit.MILLISECONDS);
            }
        }
    }

    
    /** Stores (for a limited time) the resps for later out-of-band delivery -
     *  interacts with handleLimeACKMessage.
     *  @return true if the operation failed, false if not (i.e. too busy)
     */
    public boolean bufferResponsesForLaterDelivery(QueryRequest query,
            Response...responses) {
        // store responses by guid for later retrieval
        synchronized (_outOfBandReplies) {
            if (_outOfBandReplies.size() < MessageSettings.MAX_BUFFERED_OOB_REPLIES.getValue()) {
                GUID.TimedGUID tGUID = 
                    new GUID.TimedGUID(new GUID(query.getGUID()),
                                       TIMED_GUID_LIFETIME);
                _outOfBandReplies.put(tGUID, new QueryResponseBundle(query, 
                                                                     responses));
                return true;
            }
            return false;
        }
    }
    
    private static class QueryResponseBundle {
        public final QueryRequest _query;
        public final Response[] responses;
        
        public QueryResponseBundle(QueryRequest query, Response...responses) {
            _query = query;
            this.responses = responses;
        }
    }
    
    /** Can be run to invalidate out-of-band ACKs that we are waiting for....
     */
    private class Expirer implements Runnable {
        public void run() {
            Set<GUID.TimedGUID> toRemove = new HashSet<GUID.TimedGUID>();
            synchronized (_outOfBandReplies) {
                long now = System.currentTimeMillis();
                for(GUID.TimedGUID currQB : _outOfBandReplies.keySet()) {
                    if ((currQB != null) && (currQB.shouldExpire(now)))
                        toRemove.add(currQB);
                }
                // done iterating through _outOfBandReplies, remove the 
                // keys now...
                for(GUID.TimedGUID next : toRemove)
                    _outOfBandReplies.remove(next);
            }
        }
    }
}
