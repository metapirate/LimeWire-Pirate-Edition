package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.GUID;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;

/**
 * This class is a factory for creating <tt>QueryRequest</tt> instances for
 * dynamic queries. Dynamic queries adjust to the varying conditions of a query,
 * such as the number of results received, the number of nodes hit or
 * theoretically hit, etc. This class makes it convenient to rapidly generate
 * <tt>QueryRequest</tt>s with similar characteristics, such as guids, the
 * query itself, the xml query, etc, but with customized settings, such as the
 * TTL.
 */
final class QueryHandlerImpl implements QueryHandler {

    private static final Log LOG = LogFactory.getLog(QueryHandlerImpl.class);

    /**
     * Constant for the max TTL for a query.
     */
    private static final byte MAX_QUERY_TTL = (byte) 6;

    /**
     * The number of results to try to get for queries by hash -- really small
     * since you need relatively few exact matches.
     */
    private static final int HASH_QUERY_RESULTS = 10;

    /**
     * If Leaf Guidance is in effect, the maximum number of hits to route.
     */
    private static final int MAXIMUM_ROUTED_FOR_LEAVES = 75;

    /**
     * Constant for the number of results to look for.
     */
    private final int RESULTS;

    /**
     * The number of milliseconds to wait per query hop. So, if we send out a
     * TTL=3 query, we will then wait TTL*_timeToWaitPerHop milliseconds. As the
     * query continues and we gather more data regarding the popularity of the
     * file, this number may decrease.
     */
    private volatile long _timeToWaitPerHop = 2400;

    /**
     * Variable for the number of milliseconds to shave off of the time to wait
     * per hop after a certain point in the query. As the query continues, the
     * time to shave may increase as well.
     */
    private volatile long _timeToDecreasePerHop = 10;

    /**
     * Variable for the number of times we've decremented the per hop wait time.
     * This is used to determine how much more we should decrement it on this
     * pass.
     */
    private volatile int _numDecrements = 0;

    /** List of times since start of query that results were updated */
    private final List<Long> times = new ArrayList<Long>();

    /** Number of results reported each update */
    private final List<Integer> results = new ArrayList<Integer>();

    /**
     * Variable for the number of results the leaf reports it has.
     */
    private volatile int _numResultsReportedByLeaf = 0;

    /**
     * Variable for the next time after which a query should be sent.
     */
    private volatile long _nextQueryTime = 0;

    /**
     * The theoretical number of hosts that have been reached by this query.
     */
    private volatile int _theoreticalHostsQueried = 1;

    /**
     * Constant for the <tt>ResultCounter</tt> for this query -- used to
     * access the number of replies returned.
     */
    private final ResultCounter RESULT_COUNTER;

    /**
     * Constant list of connections that have already been queried.
     */
    private final List<RoutedConnection> QUERIED_CONNECTIONS = new ArrayList<RoutedConnection>();

    /**
     * <tt>List</tt> of TTL=1 probe connections that we've already used.
     */
    private final List<RoutedConnection> QUERIED_PROBE_CONNECTIONS = new ArrayList<RoutedConnection>();

    /**
     * The time the query started.
     */
    private volatile long _queryStartTime = 0;

    /**
     * The current time, taken each time the query is initiated again.
     */
    private volatile long _curTime = 0;

    /**
     * <tt>ReplyHandler</tt> for replies received for this query.
     */
    private final ReplyHandler REPLY_HANDLER;

    /**
     * Constant for the <tt>QueryRequest</tt> used to build new queries.
     */
    private final QueryRequest QUERY;

    /**
     * Boolean for whether or not the query has been forwarded to leaves of this
     * ultrapeer.
     */
    private volatile boolean _forwardedToLeaves = false;

    /**
     * Boolean for whether or not we've sent the probe query.
     */
    private boolean _probeQuerySent;

    /**
     * used to preference which connections to use when searching if the search
     * comes from a leaf with a certain locale preference then those connections
     * (of this ultrapeer) which match the locale will be used before the other
     * connections.
     */
    private final String _prefLocale;

    private final QueryRequestFactory queryRequestFactory;

    private final ConnectionManager connectionManager;

    private final MessageRouter messageRouter;

    /**
     * Private constructor to ensure that only this class creates new
     * <tt>QueryFactory</tt> instances.
     * 
     * @param request the <tt>QueryRequest</tt> to construct a handler for
     * @param results the number of results to get -- this varies based on the
     *        type of servant sending the request and is respeceted unless it's
     *        a query for a specific hash, in which case we try to get far fewer
     *        matches, ignoring this parameter
     * @param handler the <tt>ReplyHandler</tt> for routing replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how many
     *        results have been returned for this query
     */
    QueryHandlerImpl(QueryRequest query, int results, ReplyHandler handler, ResultCounter counter,
            QueryRequestFactory queryRequestFactory, ConnectionManager connectionManager,
            MessageRouter messageRouter) {
        this.connectionManager = connectionManager;
        this.messageRouter = messageRouter;
        if (query == null)
            throw new IllegalArgumentException("null query");
        if (handler == null)
            throw new IllegalArgumentException("null reply handler");
        if (counter == null)
            throw new IllegalArgumentException("null result counter");

        this.queryRequestFactory = queryRequestFactory;

        boolean isHashQuery = !query.getQueryUrns().isEmpty();
        QUERY = query;
        if (isHashQuery) {
            RESULTS = HASH_QUERY_RESULTS;
        } else {
            RESULTS = results;
        }

        REPLY_HANDLER = handler;
        RESULT_COUNTER = counter;
        _prefLocale = handler.getLocalePref();
    }

    /** Returns the connections that have already been queried. */
    List<RoutedConnection> getQueriedConnections() {
        return QUERIED_CONNECTIONS;
    }

    /**
     * Factory method for creating new <tt>QueryRequest</tt> instances with
     * the same guid, query, xml query, urn types, etc.
     * 
     * @param ttl the time to live of the new query
     * @return a new <tt>QueryRequest</tt> instance with all of the
     *         pre-defined parameters and the specified TTL
     * @throw <tt>IllegalArgumentException</tt> if the ttl is not within what
     *        is considered reasonable bounds
     * @throw NullPointerException if the <tt>query</tt> argument is
     *        <tt>null</tt>
     */
    private QueryRequest createQuery(QueryRequest query, byte ttl) {
        if (ttl < 1 || ttl > MAX_QUERY_TTL)
            throw new IllegalArgumentException("ttl too high: " + ttl);
        if (query == null) {
            throw new NullPointerException("null query");
        }

        return queryRequestFactory.createQuery(query, ttl);
    }

    public QueryRequest getTemplateQueryRequest() {
        return QUERY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#createQuery(byte)
     */
    public QueryRequest createQuery(byte ttl) {
        return createQuery(QUERY, ttl);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#sendQuery()
     */
    public void sendQuery() {
        if (hasEnoughResults())
            return;

        _curTime = System.currentTimeMillis();
        if (_curTime < _nextQueryTime)
            return;

        if (LOG.isTraceEnabled())
            LOG.trace("Query = " + QUERY.getQuery() + ", numHostsQueried: "
                    + _theoreticalHostsQueried);

        if (_queryStartTime == 0) {
            _queryStartTime = _curTime;
        }

        // handle 3 query cases

        // 1) If we haven't sent the query to our leaves, send it
        if (!_forwardedToLeaves) {

            _forwardedToLeaves = true;
            QueryRouteTable qrt = messageRouter.getQueryRouteTable();

            QueryRequest query = createQuery(QUERY, (byte) 1);

            _theoreticalHostsQueried += 25;

            // send the query to our leaves if there's a hit and wait,
            // otherwise we'll move on to the probe
            if (qrt != null && qrt.contains(query)) {
                messageRouter.forwardQueryRequestToLeaves(query, REPLY_HANDLER);
                _nextQueryTime = System.currentTimeMillis() + _timeToWaitPerHop;
                return;
            }
        }

        // 2) If we haven't sent the probe query, send it
        if (!_probeQuerySent) {
            ProbeQuery pq = new ProbeQuery(connectionManager.getInitializedConnections(), this);
            long timeToWait = pq.getTimeToWait();
            _theoreticalHostsQueried += pq.sendProbe();
            _nextQueryTime = System.currentTimeMillis() + timeToWait;
            _probeQuerySent = true;
            return;
        }

        // 3) If we haven't yet satisfied the query, keep trying
        else {
            // Otherwise, just send a normal query -- make a copy of the
            // connections because we'll be modifying it.
            int newHosts = sendQuery(new ArrayList<RoutedConnection>(connectionManager
                    .getInitializedConnections()));
            if (newHosts == 0) {
                // if we didn't query any new hosts, wait awhile for new
                // connections to potentially appear
                _nextQueryTime = System.currentTimeMillis() + 6000;
            }
            _theoreticalHostsQueried += newHosts;

            // if we've already queried quite a few hosts, not gotten
            // many results, and have been querying for awhile, start
            // decreasing the per-hop wait time
            if (_timeToWaitPerHop > 100 && (System.currentTimeMillis() - _queryStartTime) > 6000) {
                _timeToWaitPerHop -= _timeToDecreasePerHop;

                int resultFactor = Math.max(1, (RESULTS / 2)
                        - (30 * RESULT_COUNTER.getNumResults()));

                int decrementFactor = Math.max(1, (_numDecrements / 6));

                // the current decrease is weighted based on the number
                // of results returned and on the number of connections
                // we've tried -- the fewer results and the more
                // connections, the more the decrease
                int currentDecrease = resultFactor * decrementFactor;

                currentDecrease = Math.max(5, currentDecrease);
                _timeToDecreasePerHop += currentDecrease;

                _numDecrements++;
                if (_timeToWaitPerHop < 100) {
                    _timeToWaitPerHop = 100;
                }
            }
        }
    }

    /**
     * Sends a query to one of the specified <tt>List</tt> of connections.
     * This is the heart of the dynamic query. We dynamically calculate the
     * appropriate TTL to use based on our current estimate of how widely the
     * file is distributed, how many connections we have, etc. This is static to
     * decouple the algorithm from the specific <tt>QueryHandler</tt>
     * instance, making testing significantly easier.
     * 
     * @param handler the <tt>QueryHandler</tt> instance containing data for
     *        this query
     * @param list the <tt>List</tt> of Gnutella connections to send queries
     *        over
     * @return the number of new hosts theoretically reached by this query
     *         iteration
     * 
     * Default access for testing
     */
    int sendQuery(List<? extends RoutedConnection> ultrapeersAll) {

        // we want to try to use all connections in ultrapeersLocale first.
        List<? extends RoutedConnection> ultrapeers = // method returns a copy
        connectionManager.getInitializedConnectionsMatchLocale(_prefLocale);

        QUERIED_CONNECTIONS.retainAll(ultrapeersAll);
        QUERIED_PROBE_CONNECTIONS.retainAll(ultrapeersAll);

        // if we did get a list of connections that matches the locale
        // of the query
        if (!ultrapeers.isEmpty()) {
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
            // at this point ultrapeers could become empty
        }

        if (ultrapeers.isEmpty()) {
            ultrapeers = ultrapeersAll;
            // now, remove any connections we've used from our current list
            // of connections to try
            ultrapeers.removeAll(QUERIED_CONNECTIONS);
            ultrapeers.removeAll(QUERIED_PROBE_CONNECTIONS);
        }

        int length = ultrapeers.size();
        if (LOG.isTraceEnabled())
            LOG.trace("potential querier size: " + length);
        byte ttl = 0;
        RoutedConnection mc = null;

        // add randomization to who we send our queries to
        Collections.shuffle(ultrapeers);

        // weed out all connections that aren't yet stable
        for (int i = 0; i < length; i++) {
            RoutedConnection curConnection = ultrapeers.get(i);

            // if the connection hasn't been up for long, don't use it,
            // as the replies will never make it back to us if the
            // connection is dropped, wasting bandwidth
            if (!curConnection.isStable(_curTime))
                continue;
            mc = curConnection;
            break;
        }

        int remainingConnections = Math.max(length + QUERIED_PROBE_CONNECTIONS.size(), 0);

        // return if we don't have any connections to query at this time
        if (remainingConnections == 0)
            return 0;

        // pretend we have fewer connections than we do in case we
        // lose some
        if (remainingConnections > 4)
            remainingConnections -= 4;

        boolean probeConnection = false;

        // mc can still be null if the list of connections was empty.
        if (mc == null) {
            // if we have no connections to query, simply return for now
            if (QUERIED_PROBE_CONNECTIONS.isEmpty()) {
                return 0;
            }

            // we actually remove this from the list to make sure that
            // QUERIED_CONNECTIONS and QUERIED_PROBE_CONNECTIONS do
            // not have any of the same entries, as this connection
            // will be added to QUERIED_CONNECTIONS
            mc = QUERIED_PROBE_CONNECTIONS.remove(0);
            probeConnection = true;
        }

        int reported = _numResultsReportedByLeaf;
        if(reported <= 0)
            reported = RESULT_COUNTER.getNumResults();
        double resultsPerHost = (double) reported / _theoreticalHostsQueried;

        int resultsNeeded = RESULTS - reported;
        int hostsToQuery = 40000;
        if (resultsPerHost != 0) {
            hostsToQuery = (int) (resultsNeeded / resultsPerHost);
        }

        int hostsToQueryPerConnection = hostsToQuery / remainingConnections;

        ttl = calculateNewTTL(hostsToQueryPerConnection, mc.getConnectionCapabilities()
                .getNumIntraUltrapeerConnections(), mc.getConnectionCapabilities().getHeadersRead()
                .getMaxTTL());

        // If we're sending the query down a probe connection and we've
        // already used that connection, or that connection doesn't have
        // a hit for the query, send it at TTL=2. In these cases,
        // sending the query at TTL=1 is pointless because we've either
        // already sent this query, or the Ultrapeer doesn't have a
        // match anyway
        if (ttl == 1
                && ((mc.isUltrapeerQueryRoutingConnection() && !mc.shouldForwardQuery(QUERY)) || probeConnection)) {
            ttl = 2;
        }
        QueryRequest query = createQuery(QUERY, ttl);

        // send out the query on the network, returning the number of new
        // hosts theoretically reached
        return sendQueryToHost(query, mc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#sendQueryToHost(com.limegroup.gnutella.messages.QueryRequest,
     *      com.limegroup.gnutella.connection.RoutedConnection)
     */
    public int sendQueryToHost(QueryRequest query, RoutedConnection mc) {

        // send the query directly along the connection, but if the query didn't
        // go through send back 0....
        if (!messageRouter.sendInitialQuery(query, mc))
            return 0;

        byte ttl = query.getTTL();

        // add the reply handler to the list of queried hosts if it's not
        // a TTL=1 query or the connection does not support probe queries

        // adds the connection to the list of probe connections if it's
        // a TTL=1 query to a connection that supports probe extensions,
        // otherwise add it to the list of connections we've queried
        if (ttl == 1 && mc.getConnectionCapabilities().supportsProbeQueries()) {
            this.QUERIED_PROBE_CONNECTIONS.add(mc);
        } else {
            this.QUERIED_CONNECTIONS.add(mc);
            if (LOG.isTraceEnabled())
                LOG.trace("QUERIED_CONNECTIONS.size() = " + this.QUERIED_CONNECTIONS.size());
        }

        if (LOG.isTraceEnabled())
            LOG.trace("Querying host " + mc.getAddress() + " with ttl " + query.getTTL());

        this._nextQueryTime = System.currentTimeMillis() + (ttl * this._timeToWaitPerHop);

        return calculateNewHosts(mc, ttl);
    }

    /**
     * Calculates the new TTL to use based on the number of hosts per connection
     * we still need to query.
     * 
     * @param hostsToQueryPerConnection the number of hosts we should reach on
     *        each remaining connections, to the best of our knowledge
     * @param degree the out-degree of the next connection
     * @param maxTTL the maximum TTL the connection will allow
     * @return the TTL to use for the next connection
     */
    static byte calculateNewTTL(int hostsToQueryPerConnection, int degree, byte maxTTL) {

        if (maxTTL > MAX_QUERY_TTL)
            maxTTL = MAX_QUERY_TTL;

        // not the most efficient algorithm -- should use Math.log, but
        // that's ok
        for (byte i = 1; i < MAX_QUERY_TTL; i++) {

            // biased towards lower TTLs since the horizon expands so
            // quickly
            int hosts = (int) (16.0 * calculateNewHosts(degree, i));
            if (hosts >= hostsToQueryPerConnection) {
                if (i > maxTTL)
                    return maxTTL;
                return i;
            }
        }
        return maxTTL;
    }

    /**
     * Calculate the number of new hosts that would be added to the theoretical
     * horizon if a query with the given ttl were sent down the given
     * connection.
     * 
     * @param conn the <tt>Connection</tt> that will received the query
     * @param ttl the TTL of the query to add
     */
    static int calculateNewHosts(RoutedConnection conn, byte ttl) {
        return calculateNewHosts(
                conn.getConnectionCapabilities().getNumIntraUltrapeerConnections(), ttl);
    }

    /**
     * Calculate the number of new hosts that would be added to the theoretical
     * horizon if a query with the given ttl were sent to a node with the given
     * degree. This is not precise because we're assuming that the nodes
     * connected to the node in question also have the same degree, but there's
     * not much we can do about it!
     * 
     * @param degree the degree of the node that will received the query
     * @param ttl the TTL of the query to add
     */
    static int calculateNewHosts(int degree, byte ttl) {
        double newHosts = 0;
        for (; ttl > 0; ttl--) {
            newHosts += Math.pow((degree - 1), ttl - 1);
        }
        return (int) newHosts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#hasEnoughResults()
     */
    public boolean hasEnoughResults() {
        // return false if the query hasn't started yet
        if (_queryStartTime == 0)
            return false;

        // ----------------
        // NOTE: as agreed, _numResultsReportedByLeaf is the number of results
        // the leaf has received/consumed by a filter DIVIDED by 4 (4 being the
        // number of UPs connection it maintains). That is why we don't divide
        // it here or anything. We aren't sure if this mixes well with
        // BearShare's use but oh well....
        // ----------------
        // if leaf guidance is in effect, we have different criteria.
        if (_numResultsReportedByLeaf > 0) {
            // we shouldn't route too much regardless of what the leaf says
            if (RESULT_COUNTER.getNumResults() >= MAXIMUM_ROUTED_FOR_LEAVES)
                return true;
            // if the leaf is happy, so are we....
            if (_numResultsReportedByLeaf > RESULTS)
                return true;
        }
        // leaf guidance is not in effect or we are doing our own query
        else if (RESULT_COUNTER.getNumResults() >= RESULTS)
            return true;

        // if our theoretical horizon has gotten too high, consider
        // it enough results
        // precisely what this number should be is somewhat hard to determine
        // because, while connection have a specfic degree, the degree of
        // the connections on subsequent hops cannot be determined
        if (_theoreticalHostsQueried > 110000) {
            return true;
        }

        // return true if we've been querying for longer than the specified
        // maximum
        int queryLength = (int) (System.currentTimeMillis() - _queryStartTime);
        if (queryLength > QueryHandler.MAX_QUERY_TIME) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#updateLeafResults(int)
     */
    public void updateLeafResults(int numResults) {
        if (numResults > _numResultsReportedByLeaf) {
            // record up to the first 20 updates
            if (times.size() < 20) {
                times.add(System.currentTimeMillis() - _queryStartTime);
                results.add(numResults);
            }
            _numResultsReportedByLeaf = numResults;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#getNumResultsReportedByLeaf()
     */
    public int getNumResultsReportedByLeaf() {
        return _numResultsReportedByLeaf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#getReplyHandler()
     */
    public ReplyHandler getReplyHandler() {
        return REPLY_HANDLER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#getTimeToWaitPerHop()
     */
    public long getTimeToWaitPerHop() {
        return _timeToWaitPerHop;
    }

    // overrides Object.toString
    @Override
    public String toString() {
        return "QueryHandler: QUERY: " + QUERY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.search.QueryHandler#getGUID()
     */
    public GUID getGUID() {
        return new GUID(QUERY.getGUID());
    }
}
