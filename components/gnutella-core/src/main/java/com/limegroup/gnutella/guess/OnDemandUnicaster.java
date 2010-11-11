package com.limegroup.gnutella.guess;


import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.security.AddressSecurityToken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

/** Utility class for sending GUESS queries.
 */
@EagerSingleton
public class OnDemandUnicaster {
    
    private static final Log LOG = LogFactory.getLog(OnDemandUnicaster.class);

    // time to store buffered hosts waiting for a QK to query. 
    private static final int CLEAR_TIME = 5 * 60 * 1000; // 5 minutes
    
    // time to store hosts we've sent a query to.
    private static final int QUERIED_HOSTS_CLEAR_TIME = 30 * 1000; // 30 seconds
    
    /** IpPorts that we've queried for this GUID. */
    private final Map<GUID.TimedGUID, Set<IpPort>> _queriedHosts;

    /** GUESSEndpoints => AddressSecurityToken. */
    private final Map<GUESSEndpoint, AddressSecurityToken> _queryKeys;
  
    /**
     * Short term store for queries waiting for query keys.
     * GUESSEndpoints => URNs
     */
    private final Map<GUESSEndpoint, SendLaterBundle> _bufferedURNs;
    
    private final QueryRequestFactory queryRequestFactory;
    private final UDPService udpService;

    private final Provider<MessageRouter> messageRouter;

    private final PingRequestFactory pingRequestFactory;

    @Inject
    public OnDemandUnicaster(QueryRequestFactory queryRequestFactory,
            UDPService udpService, 
            Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory) {
        this.queryRequestFactory = queryRequestFactory;
        this.udpService = udpService;
        this.messageRouter = messageRouter;
        this.pingRequestFactory = pingRequestFactory;
        
        // static initializers are only called once, right?
        _queryKeys = new Hashtable<GUESSEndpoint, AddressSecurityToken>(); // need sychronization
        _bufferedURNs = new Hashtable<GUESSEndpoint, SendLaterBundle>(); // synchronization handy
        _queriedHosts = new HashMap<GUID.TimedGUID, Set<IpPort>>();
     }        

    @Inject
    public void register(ServiceScheduler serviceScheduler, 
                         @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        // schedule a runner to clear various data structures
        serviceScheduler.scheduleWithFixedDelay("OnDemandUnicaster.Expirer", new Expirer(), CLEAR_TIME, CLEAR_TIME, TimeUnit.MILLISECONDS, backgroundExecutor);
        serviceScheduler.scheduleWithFixedDelay("OnDemandUnicaster.QueriedHostsExpirer", new QueriedHostsExpirer(), QUERIED_HOSTS_CLEAR_TIME, QUERIED_HOSTS_CLEAR_TIME, TimeUnit.MILLISECONDS, backgroundExecutor);
    }
    /** Feed me AddressSecurityToken pongs so I can query people....
     *  pre: pr.getQueryKey() != null
     */
    public void handleQueryKeyPong(PingReply pr) 
        throws NullPointerException, IllegalArgumentException {


        // validity checks
        // ------
        if (pr == null)
            throw new NullPointerException("null pong");

        AddressSecurityToken qk = pr.getQueryKey();
        if (qk == null)
            throw new IllegalArgumentException("no key in pong");
        // ------

        // create guess endpoint
        // ------
        InetAddress address = pr.getInetAddress();
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        // ------

        // store query key
        _queryKeys.put(endpoint, qk);

        // if a buffered query exists, send it...
        // -----
        SendLaterBundle bundle = _bufferedURNs.remove(endpoint);
        if (bundle != null)
            sendQuery(bundle._queryURN, qk, endpoint);
        // -----
    }

    /** Sends out a UDP query with the specified URN to the specified host.
     *  @throws IllegalArgumentException if ep or queryURN are null.
     *  @param ep the location you want to query.
     *  @param queryURN the URN you are querying for.
     */
    public void query(GUESSEndpoint ep, URN queryURN) 
        throws IllegalArgumentException {

        // validity checks
        // ------
        if (ep == null)
            throw new IllegalArgumentException("No Endpoint!");
        if (queryURN == null)
            throw new IllegalArgumentException("No urn to look for!");
        // ------

        // see if you have a AddressSecurityToken - if not, request one
        // ------
        AddressSecurityToken key = _queryKeys.get(ep);
        if (key == null) {
            GUESSEndpoint endpoint = new GUESSEndpoint(ep.getInetAddress(),
                                                       ep.getPort());
            SendLaterBundle bundle = new SendLaterBundle(queryURN);
            _bufferedURNs.put(endpoint, bundle);
            PingRequest pr = pingRequestFactory.createQueryKeyRequest();
            udpService.send(pr, ep.getInetAddress(), ep.getPort());
        }
        // ------
        // if possible send query, else buffer
        // ------
        else {
            sendQuery(queryURN, key, ep);
        }
        // ------
    }
    
    /**
     * Determines if the given host was sent a direct UDP URN query
     * in the last 30 seconds.
     */
    public boolean isHostQueriedForGUID(GUID guid, IpPort host) {
        synchronized(_queriedHosts) {
            Set<IpPort> hosts = _queriedHosts.get(new GUID.TimedGUID(guid));
            return hosts != null ? hosts.contains(host) : false;
        }
    }
    
    private void sendQuery(URN urn, AddressSecurityToken qk, IpPort ipp) {
        QueryRequest query = queryRequestFactory.createQueryKeyQuery(urn, qk);
        // store the query's GUID -> IPP so that when we get replies over
        // UDP we can allow them without requiring the whole ReplyNumber/ACK
        // thing.
        GUID qGUID = new GUID(query.getGUID());
        synchronized(_queriedHosts) {
            GUID.TimedGUID guid = new GUID.TimedGUID(qGUID, QUERIED_HOSTS_CLEAR_TIME);
            Set<IpPort> hosts = _queriedHosts.get(guid);
            if(hosts == null)
                hosts = new IpPortSet();
            hosts.add(ipp);
            // Always re-add, so the TimedGUID will last longer
            _queriedHosts.put(guid, hosts);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Sending query with GUID: " + qGUID + " for URN: " + urn + " to host: " + ipp);
        
        messageRouter.get().originateQueryGUID(query.getGUID());
        udpService.send(query, ipp.getInetAddress(), ipp.getPort());
    }

    private static class SendLaterBundle {

        private static final int MAX_LIFETIME = 60 * 1000;

        public final URN _queryURN;
        private final long _creationTime;

        public SendLaterBundle(URN urn) {
            _queryURN = urn;
            _creationTime = System.currentTimeMillis();
        }
                               
        public boolean shouldExpire() {
            return ((System.currentTimeMillis() - _creationTime) >
                    MAX_LIFETIME);
        }
    }

    /**
     * This method has been disaggregated from the Expirer class for ease of
     * testing.
     * @return true if the Query Key data structure was cleared.
     * @param lastQueryKeyClearTime The last time query keys were cleared.
     * @param queryKeyClearInterval how often you like query keys to be
     * cleared.
     */ 
    private boolean clearDataStructures(long lastQueryKeyClearTime,
                                               long queryKeyClearInterval) {
        boolean clearedQueryKeys = false;

        // Clear the QueryKeys if needed
        // ------
        if ((System.currentTimeMillis() - lastQueryKeyClearTime) >
            queryKeyClearInterval) {
            clearedQueryKeys = true;
            // we just indiscriminately clear all the query keys - we
            // could just expire 'old' ones, but the benefit is marginal
            _queryKeys.clear();
        }
        // ------

        // Get rid of all the buffered URNs that should be expired
        // ------
        synchronized (_bufferedURNs) {
            for(Iterator<SendLaterBundle> iter =  _bufferedURNs.values().iterator(); iter.hasNext(); ) {
                SendLaterBundle bundle = iter.next();
                if (bundle.shouldExpire())
                    iter.remove();
            }
        }
        // ------

        return clearedQueryKeys;
    }


    /** This is run to clear various data structures used.
     *  Made package access for easy test access.
     */
    private class Expirer implements Runnable {

        // 24 hours
        private static final int QUERY_KEY_CLEAR_TIME = 24 * 60 * 60 * 1000;

        private long _lastQueryKeyClearTime;

        public Expirer() {
            _lastQueryKeyClearTime = System.currentTimeMillis();
        }

        public void run() {
            if (clearDataStructures(_lastQueryKeyClearTime, 
                                        QUERY_KEY_CLEAR_TIME))
                    _lastQueryKeyClearTime = System.currentTimeMillis();
        }
    }
    
    /** This is run to clear various data structures used.
     *  Made package access for easy test access.
     */
    private class QueriedHostsExpirer implements Runnable {
        public void run() {
            synchronized(_queriedHosts) {
                long now = System.currentTimeMillis();
                for(Iterator<GUID.TimedGUID> iter = _queriedHosts.keySet().iterator(); iter.hasNext(); ) {
                    GUID.TimedGUID guid = iter.next();
                    if(guid.shouldExpire(now))
                        iter.remove();
                }
            }
        }
    }
}
