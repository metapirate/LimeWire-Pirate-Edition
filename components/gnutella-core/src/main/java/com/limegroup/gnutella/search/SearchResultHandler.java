package com.limegroup.gnutella.search;

import java.util.List;

import org.limewire.io.Address;
import org.limewire.io.GUID;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.response.ResponseFilter;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
public interface SearchResultHandler {
    
    /**
     * The "delay" between responses to wait to send a QueryStatusResponse.
     */
    public static final int REPORT_INTERVAL = 15;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * basically sent to say 'shut off query'.
     */
    public static final int MAX_RESULTS = 65535;

    /**
     * Adds the Query to the list of queries kept track of.  You should do this
     * EVERY TIME you start a query so we can leaf guide it when possible.
     * Also adds the query to the Spam Manager to adjust percentages.
     *
     * @param qr The query that has been started.  We really just access the guid.
     */ 
    public void addQuery(QueryRequest qr);

    /**
     * Removes the Query from the list of queries kept track of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid);

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, based on the number of results they've had and/or
     * whether or not they're new enough.
     */
    public List<QueryRequest> getQueriesToReSend();

    /**
     * Use this to see how many results have been displayed to the user for the
     * specified query.
     *
     * @param guid the guid of the query.
     *
     * @return the number of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
     */    
    public int getNumResultsForQuery(GUID guid);
    
    /** 
     * Handles the given query reply. Only one thread may call it at a time.
     * 
     * @param address the common address for all responses of that query reply, 
     * can be null. If it is null {@link Response#toRemoteFileDesc(QueryReply, Address, com.limegroup.gnutella.downloader.RemoteFileDescFactory, com.limegroup.gnutella.PushEndpointFactory)}
     * will construct a new address for the {@link RemoteFileDesc}.
     * @return <tt>true</tt> if the GUI will (probably) display the results,
     *  otherwise <tt>false</tt> 
     */
    public void handleQueryReply(final QueryReply qr, Address address);
    
    /** Sets the new filter to use for response filters. */
    void setResponseFilter(ResponseFilter responseFilter);

}
