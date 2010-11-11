package com.limegroup.gnutella.search;

import org.limewire.io.GUID;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.QueryRequest;

public interface QueryHandler {

    /**
     * Constant for the maximum number of milliseconds the entire query
     * can last.  The query expires when this limit is reached.
     */
    public static final int MAX_QUERY_TIME = 200 * 1000;

    /** Gets the query used for building new queries. */
    public QueryRequest getTemplateQueryRequest();

    /**
     * Convenience method for creating a new query with the given TTL
     * with this <tt>QueryHandler</tt>.
     *
     * @param ttl the time to live for the new query
     */
    public QueryRequest createQuery(byte ttl);

    /**
     * Sends the query to the current connections.  If the query is not
     * yet ready to be processed, this returns immediately.
     */
    public void sendQuery();

    /**
     * Sends a query to the specified host.
     *
     * @param query the <tt>QueryRequest</tt> to send
     * @param mc the <tt>RoutedConnection</tt> to send the query to
     * @return the number of new hosts theoretically hit by this query
     */
    public int sendQueryToHost(QueryRequest query, RoutedConnection mc);

    /**
     * Returns whether or not this query has received enough results.
     *
     * @return <tt>true</tt> if this query has received enough results,
     *  <tt>false</tt> otherwise
     */
    public boolean hasEnoughResults();

    /**
     * Use this to modify the number of results as reported by the leaf you are
     * querying for.
     */
    public void updateLeafResults(int numResults);

    /**
     * Returns the number of results as reported by the leaf.  At least 0.
     */
    public int getNumResultsReportedByLeaf();

    /**
     * Accessor for the <tt>ReplyHandler</tt> instance for the connection
     * issuing this request.
     *
     * @return the <tt>ReplyHandler</tt> for the connection issuing this 
     *  request
     */
    public ReplyHandler getReplyHandler();

    /**
     * Accessor for the time to wait per hop, in milliseconds,
     * for this QueryHandler.
     *
     * @return the time to wait per hop in milliseconds for this
     *  QueryHandler
     */
    public long getTimeToWaitPerHop();

    /** @return simply returns the guid of the query this is handling.
     */
    public GUID getGUID();

}