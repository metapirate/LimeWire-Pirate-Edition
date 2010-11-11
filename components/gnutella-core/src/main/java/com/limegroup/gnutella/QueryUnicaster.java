package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Buffer;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Service;
import org.limewire.security.AddressSecurityToken;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

/** 
 * This class runs a single thread which sends unicast UDP queries to a master
 * list of unicast-enabled hosts every n milliseconds.  It interacts with
 * HostCatcher to find unicast-enabled hosts.  It also allows for stopping of
 * individual queries by reply counts.
 */ 
@EagerSingleton
public final class QueryUnicaster implements Service {
    
    private static final Log LOG = LogFactory.getLog(QueryUnicaster.class);

    /** The time in between successive unicast queries.
     */
    public static final int ITERATION_TIME = 100; // 1/10th of a second...

    /** The number of Endpoints where you should start sending pings to them.
     */
    public static final int MIN_ENDPOINTS = 25;

    /** The max number of unicast pongs to store.
     */
    //public static final int MAX_ENDPOINTS = 2000;
    public static final int MAX_ENDPOINTS = 30;

    /** One hour in milliseconds.
     */
    public static final long ONE_HOUR = 1000 * 60 * 60; // 60 minutes

    /** Actually sends any QRs via unicast UDP messages.
     */
    private final Thread _querier;

    /** 
     * The map of Queries I need to send every iteration.
     * The map is from GUID to QueryBundle.  The following invariant is
     * maintained:
     * GUID -> QueryBundle where GUID == QueryBundle._qr.getGUID()
     */
    private final Map<GUID, QueryBundle> _queries;

    /**
     * Maps leaf connections to the queries they've spawned.
     * The map is from ReplyHandler to a Set (of GUIDs).
     */
    private final Map<ReplyHandler, Set<GUID>> _querySets;

    /** 
     * The unicast enabled hosts I should contact for queries.  Add to the
     * front, remove from the end.  Therefore, the OLDEST entries are at the
     * end.
     */
    private final LinkedList<GUESSEndpoint> _queryHosts;

    /**
     * The Set of QueryKeys to be used for Queries.
     */
    private final Map<GUESSEndpoint, QueryKeyBundle> _queryKeys;

    /** The fixed size list of endpoints i've pinged.
     */
    private final Buffer<GUESSEndpoint> _pingList;

    /** A List of query GUIDS to purge.
     */
    private final List<GUID> _qGuidsToRemove;

    /** The last time I sent a broadcast ping.
     */
    private long _lastPingTime = 0;

	/** 
     * How many test pings have been sent out to determine 
	 * whether or not we can accept incoming connections.
	 */
	private int _testUDPPingsSent = 0;

	/**
	 * Records whether or not someone has called init on me....
	 */
	private boolean _initialized = false;
		
	private final NetworkManager networkManager;
	private final QueryRequestFactory queryRequestFactory;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<UDPService> udpService;

    private final PingRequestFactory pingRequestFactory;

	@Inject
    public QueryUnicaster(NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter,
            Provider<UDPService> udpService,
            PingRequestFactory pingRequestFactory) {
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.messageRouter = messageRouter;
        this.udpService = udpService;
        this.pingRequestFactory = pingRequestFactory;
        
        _queries = new Hashtable<GUID, QueryBundle>();
        _queryHosts = new LinkedList<GUESSEndpoint>();
        _queryKeys = new Hashtable<GUESSEndpoint, QueryKeyBundle>();
        _pingList = new Buffer<GUESSEndpoint>(25);
        _querySets = new Hashtable<ReplyHandler, Set<GUID>>();
        _qGuidsToRemove = new Vector<GUID>();
    
        // start service...
        _querier = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                queryLoop();
            }
        });
        
        _querier.setName("QueryUnicaster");
        _querier.setDaemon(true);
    }
	
    /** Returns the number of Queries unicasted by this guy...
     */
    int getQueryNumber() {
        return _queries.size();
    }

    /** 
     * Returns a List of unicast Endpoints.  These Endpoints are the NEWEST 
     * we've seen.
     */
    public List<GUESSEndpoint> getUnicastEndpoints() {
        List<GUESSEndpoint> retList = new ArrayList<GUESSEndpoint>();
        synchronized (_queryHosts) {
            int size = _queryHosts.size();
            if (size > 0) {
                int max = (size > 10 ? 10 : size);
                for (int i = 0; i < max; i++)
                    retList.add(_queryHosts.get(i));
            }
        }
        return retList;
    }

	/** 
     * Returns a <tt>GUESSEndpoint</tt> from the current cache of 
	 * GUESS endpoints.
	 *
	 * @return a <tt>GUESSEndpoint</tt> from the list of GUESS hosts
	 *  to query, or <tt>null</tt> if there are no available hosts
	 *  to return
	 */
	public GUESSEndpoint getUnicastEndpoint() {
		synchronized(_queryHosts) {
			if(_queryHosts.isEmpty())
                return null;
            else
                return _queryHosts.getFirst();
		}
	}
	
	@Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    /**
     * Starts the query unicaster thread.
     */
    public synchronized void start() {
        if (!_initialized) {
            _querier.start();
            
            QueryKeyExpirer expirer = new QueryKeyExpirer();
            backgroundExecutor.scheduleWithFixedDelay(expirer, 0, 3 * ONE_HOUR,
                    TimeUnit.MILLISECONDS); // Expire query keys every 3 hours

            _initialized = true;
        }
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Directed Querier");
    }
    
    public void initialize() {
    }
    
    public void stop() {
    }
    
    /** 
     * The main work to be done.
     * If there are queries, get a unicast enabled UP, and send each Query to
     * it.  Then sleep and try some more later...
     */
    private void queryLoop() {
        while (true) {
            try {
                waitForQueries();
                GUESSEndpoint toQuery = getUnicastHost();
                // no query key to use in my query!
                if (!_queryKeys.containsKey(toQuery)) {
                    // send a AddressSecurityToken Request
                    PingRequest pr = pingRequestFactory.createQueryKeyRequest();
                    udpService.get().send(pr,toQuery.getInetAddress(), toQuery.getPort());
                    // DO NOT RE-ADD ENDPOINT - we'll do that if we get a
                    // AddressSecurityToken Reply!!
                    continue; // try another up above....
                }
                AddressSecurityToken addressSecurityToken = _queryKeys.get(toQuery)._queryKey;

                purgeGuidsInternal(); // in case any were added while asleep
				boolean currentHostUsed = false;
                synchronized (_queries) {
                    for(QueryBundle currQB : _queries.values()) {
                        if (currQB._hostsQueried.size() > QueryBundle.MAX_QUERIES)
                            // query is now stale....
                            _qGuidsToRemove.add(new GUID(currQB._qr.getGUID()));
                        else if (currQB._hostsQueried.contains(toQuery))
                            ; // don't send another....
                        else {
							InetAddress ip = toQuery.getInetAddress();
							QueryRequest qrToSend = 
							    queryRequestFactory.createQueryKeyQuery(currQB._qr, 
																 addressSecurityToken);
                            udpService.get().send(qrToSend, 
                                            ip, toQuery.getPort());
							currentHostUsed = true;
							currQB._hostsQueried.add(toQuery);
                        }
                    }
                }

				// add the current host back to the list if it was not used for 
				// any query
				if(!currentHostUsed) {
					addUnicastEndpoint(toQuery);
				}
                
                // purge stale queries, hold lock so you don't miss any...
                synchronized (_qGuidsToRemove) {
                    purgeGuidsInternal();
                    _qGuidsToRemove.clear();
                }

                Thread.sleep(ITERATION_TIME);
            }
            catch (InterruptedException ignored) {}
        }
    }
 
    /** 
     * A quick purging of query GUIDS from the _queries Map.  The
     * queryLoop uses this to so it doesn't have to hold the _queries
     * lock for too long.
     */
    private void purgeGuidsInternal() {
        synchronized (_qGuidsToRemove) {
            for(GUID currGuid : _qGuidsToRemove)
                _queries.remove(currGuid);
        }
    }

    private void waitForQueries() throws InterruptedException {
        LOG.trace("Waiting for queries");
        synchronized (_queries) {
            if (_queries.isEmpty()) {
                // i'll be notifed when stuff is added...
                _queries.wait();
			}
        }
        if(LOG.isTraceEnabled())
            LOG.trace("Got " + _queries.size() + " queries");
    }

    /** 
     * @return true if the query was added (maybe false if it existed).
     * @param query The Query to add, to start unicasting.
     * @param reference The originating connection.  OK if NULL.
     */
    public boolean addQuery(QueryRequest query, ReplyHandler reference) {
        LOG.trace("Adding query");
        boolean added = false;
        GUID guid = new GUID(query.getGUID());
        // first map the QueryBundle using the guid....
        synchronized (_queries) {
            if (!_queries.containsKey(guid)) {
                QueryBundle qb = new QueryBundle(query);
                _queries.put(guid, qb);
                _queries.notifyAll();
                added = true;
            }
        }

		// return if this node originated the query
        if (reference == null)
            return added;

        // then record the guid in the set of leaf's queries...
        synchronized (_querySets) {
            Set<GUID> guids = _querySets.get(reference);
            if (guids == null) {
                guids = new HashSet<GUID>();
                _querySets.put(reference, guids);
            }
            guids.add(guid);
        }
        return added;
    }

    /** Just feed me ExtendedEndpoints - I'll check if I could use them or not.
     */
    public void addUnicastEndpoint(InetAddress address, int port) {
        if (!SearchSettings.GUESS_ENABLED.getValue()) return;
        if (notMe(address, port) && NetworkUtils.isValidPort(port) &&
          NetworkUtils.isValidAddress(address)) {
			GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
			addUnicastEndpoint(endpoint);
        }
    }

	/** Adds the <tt>GUESSEndpoint</tt> instance to the host data.
	 *
	 *  @param endpoint the <tt>GUESSEndpoint</tt> to add
	 */
	private void addUnicastEndpoint(GUESSEndpoint endpoint) {
		synchronized (_queryHosts) {
			if (_queryHosts.size() == MAX_ENDPOINTS) {
                LOG.trace("Evicting old unicast host to make room");
				_queryHosts.removeLast();
            }
            LOG.trace("Adding new unicast host");
			_queryHosts.addFirst(endpoint);
			_queryHosts.notify();
            // Consider sending a test ping
			if(udpService.get().isListening() &&
			        !networkManager.isGUESSCapable() &&
			        _testUDPPingsSent < 10 &&
			        !(ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && 
			                NetworkUtils.isCloseIP(networkManager.getAddress(),
			                        endpoint.getInetAddress().getAddress()))) {
                LOG.info("Sending a UDP test ping");
                byte[] guid = udpService.get().getSolicitedGUID().bytes();
				PingRequest pr = 
				    pingRequestFactory.createPingRequest(guid, (byte)1, (byte)0);
                udpService.get().send(pr, endpoint.getInetAddress(), endpoint.getPort());
				_testUDPPingsSent++;
			}
		}
	}

    /** 
     * Returns whether or not the Endpoint refers to me!  True if it doesn't,
     * false if it does (NOT not me == me).
     */
    private boolean notMe(InetAddress address, int port) {
        if((port == networkManager.getPort()) &&
                Arrays.equals(address.getAddress(), 
                        networkManager.getAddress())) {			
            return false;
        }
        return true;
    }

    /** 
     * Gets rid of a Query according to ReplyHandler.  
     * Use this if a leaf connection dies and you want to stop the query.
     */
    void purgeQuery(ReplyHandler reference) {
        LOG.trace("Purging query by ReplyHandler");
        if (reference == null)
            return;
        synchronized (_querySets) {
            Set<GUID> guids = _querySets.remove(reference);
            if (guids == null)
                return;
            for(GUID guid : guids)
                purgeQuery(guid);
        }
    }

    /** 
     * Gets rid of a Query according to GUID.  Use this if a leaf connection
     * dies and you want to stop the query.
     */
    void purgeQuery(GUID queryGUID) {
        LOG.trace("Purging query by GUID");
        _qGuidsToRemove.add(queryGUID);
    }

    /** Feed me QRs so I can keep track of stuff.
     */
    public void handleQueryReply(QueryReply qr) {
        addResults(new GUID(qr.getGUID()), qr.getResultCount());
    }


    /** Feed me AddressSecurityToken pongs so I can query people....
     *  pre: pr.getQueryKey() != null
     */
    public void handleQueryKeyPong(PingReply pr) {
        Objects.nonNull(pr, "pong");
        AddressSecurityToken qk = pr.getQueryKey();
        Objects.nonNull(qk, "query key");
        InetAddress address = pr.getInetAddress();
        int port = pr.getPort();
        GUESSEndpoint endpoint = new GUESSEndpoint(address, port);
        _queryKeys.put(endpoint, new QueryKeyBundle(qk));
        addUnicastEndpoint(endpoint);
    }

    /** 
     * Add results to a query so we can invalidate it when enough results are
     * received.
     */
    private void addResults(GUID queryGUID, int numResultsToAdd) {
        LOG.trace("Adding results to query");
        synchronized (_queries) {
            QueryBundle qb = _queries.get(queryGUID);
            if (qb != null) {// add results if possible...
                qb._numResults += numResultsToAdd;
                
                //  This code moved from queryLoop() since that ftn. blocks before
                //      removing stale queries, when out of hosts to query.
                if(qb._numResults > QueryBundle.MAX_RESULTS) {
                    LOG.trace("Query has enough results, removing");
                    synchronized(_qGuidsToRemove) {
                        _qGuidsToRemove.add(new GUID(qb._qr.getGUID()));
                        purgeGuidsInternal();
                        _qGuidsToRemove.clear();                        
                    }
                }
            }
        }
    }

    /** May block if no hosts exist.
     */
    private GUESSEndpoint getUnicastHost() throws InterruptedException {
        LOG.trace("Waiting for hosts");
        synchronized (_queryHosts) {
            while (_queryHosts.isEmpty()) {
                // don't sent too many pings
                if (System.currentTimeMillis() - _lastPingTime > 20000) {
                    // first send a Ping, hopefully we'll get some pongs....
                    byte ttl = ConnectionSettings.TTL.getValue();
                    PingRequest pr = pingRequestFactory.createPingRequest(ttl);
                    LOG.info("Broadcasting a ping");
                    messageRouter.get().broadcastPingRequest(pr);
                    _lastPingTime = System.currentTimeMillis();
                }
				// now wait, what else can we do?
				_queryHosts.wait();
            }
        }
        if(LOG.isTraceEnabled())
            LOG.trace("Got " + _queryHosts.size() + " hosts");

        if (_queryHosts.size() < MIN_ENDPOINTS) {
            // send a ping to the guy you are popping if cache too small
            GUESSEndpoint toReturn = _queryHosts.removeLast();
            // if i haven't pinged him 'recently', then ping him...
            synchronized (_pingList) {
                if (!_pingList.contains(toReturn)) {
                    LOG.trace("Pinging unicast host before removing");
                    PingRequest pr = pingRequestFactory.createPingRequest((byte)1);
                    InetAddress ip = toReturn.getInetAddress();
                    udpService.get().send(pr, ip, toReturn.getPort());
                    _pingList.add(toReturn);
                }
            }
            return toReturn;
        }
        return _queryHosts.removeLast();
    }
    
    /** removes all Unicast Endpoints, reset associated members
     */
    void resetUnicastEndpointsAndQueries() {
        LOG.trace("Resetting unicast hosts and queries");        
        synchronized (_queries) {
            _queries.clear();
            _queries.notifyAll();
        }

        synchronized (_queryHosts) {
            _queryHosts.clear();
            _queryHosts.notifyAll();
        }
        
        synchronized (_queryKeys) {
            _queryKeys.clear();
            _queryKeys.notifyAll();
        }
        
        synchronized (_pingList) {
            _pingList.clear();
            _pingList.notifyAll();
        }

        _lastPingTime=0;        
        _testUDPPingsSent=0;
    }


    private static class QueryBundle {
        public static final int MAX_RESULTS = 250;
        public static final int MAX_QUERIES = 1000;
        final QueryRequest _qr;
        // the number of results received per Query...
        int _numResults = 0;
        /** The Set of Endpoints queried for this Query.
         */
        final Set<GUESSEndpoint> _hostsQueried = new HashSet<GUESSEndpoint>();

        public QueryBundle(QueryRequest qr) {
            _qr = qr;
        }
		
		// overrides toString to provide more information
		@Override
        public String toString() {
			return "QueryBundle: "+_qr;
		}
    }

    private static class QueryKeyBundle {
        public static final long QUERY_KEY_LIFETIME = 2 * ONE_HOUR; // 2 hours
        
        final long _birthTime;
        final AddressSecurityToken _queryKey;
        
        public QueryKeyBundle(AddressSecurityToken qk) {
            _queryKey = qk;
            _birthTime = System.currentTimeMillis();
        }

        /** Returns true if this AddressSecurityToken hasn't been updated in a while and
         *  should be expired.
         */
        public boolean shouldExpire() {
            if (System.currentTimeMillis() - _birthTime >= QUERY_KEY_LIFETIME)
                return true;
            return false;
        }

        @Override
        public String toString() {
            return "{QueryKeyBundle: " + _queryKey + " BirthTime = " +
            _birthTime;
        }
    }

    /**
     * Schedule this class to run every so often and rid the Map of Bundles that
     * are stale.
     */ 
    private class QueryKeyExpirer implements Runnable {
        public void run() {
            synchronized (_queryKeys) {
                Iterator<QueryKeyBundle> iter = _queryKeys.values().iterator();
                while(iter.hasNext()) {
                    if(iter.next().shouldExpire())
                        iter.remove();
                }
            }
        }
    }
}
