package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;

/**
 * Extends {@link Connection} to provide more fine-grained control over a
 * Gnutella connection. <code>RoutedConnection</code> defines the interface to
 * allow a Connection to act asynchronously, receiving messages and handshaking
 * in the background. Additional methods are provided to react
 * {@link RouteTableMessage RouteTableMessages}, poll for bandwidth, and more
 * detailed Gnutella functionality.
 */
public interface RoutedConnection extends Connection, ReplyHandler {

    /**
     * Sends the message through this connection. This will return immediately
     * and the message will be sent asynchronously.
     */
    public void send(Message m);

    /**
     * Handles core Gnutella request/reply protocol. This immediately returns.
     */
    void startMessaging();

    /**
     * Attempts to initialize the connection. This will return immediately.
     */
    public void initialize(GnetConnectObserver observer) throws IOException;

    /**
     * Resets the query route table for this connection. The new table will be
     * of the size specified in <tt>rtm</tt> and will contain no data. If
     * there is no <tt>QueryRouteTable</tt> yet created for this connection,
     * this method will create one.
     * 
     * @param rtm the <tt>ResetTableMessage</tt>
     */
    public void resetQueryRouteTable(ResetTableMessage rtm);

    /**
     * Patches the <tt>QueryRouteTable</tt> for this connection.
     * 
     * @param ptm the patch with the data to update
     */
    public void patchQueryRouteTable(PatchTableMessage ptm);

    /**
     * Returns true iff this connection is a shielded leaf connection, and has
     * signalled that it does not want to receive routed queries (no upload
     * slots or some other reason). If so, we will not include its QRT table in
     * last hop QRT tables we send out (if we are an Ultrapeer).
     * 
     * @return true iff this connection is a busy leaf (don't include his QRT
     *         table)
     */
    public boolean isBusyLeaf();

    /**
     * Determines whether or not the specified <tt>QueryRequest</tt> instance
     * should be sent to the connection. The method takes a couple factors into
     * account, such as QRP tables, type of query, etc.
     * 
     * @param query the <tt>QueryRequest</tt> to check against the data
     * @return <tt>true</tt> if the <tt>QueryRequest</tt> should be sent to
     *         this connection, otherwise <tt>false</tt>
     */
    public boolean shouldForwardQuery(QueryRequest query);

    /**
     * This is a specialized send method for queries that we originate, either
     * from ourselves directly, or on behalf of one of our leaves when we're an
     * Ultrapeer. These queries have a special sending queue of their own and
     * are treated with a higher priority.
     * 
     * @param query the <tt>QueryRequest</tt> to send
     */
    public void originateQuery(QueryRequest query);

    /**
     * @modifies this
     * @effects sets the underlying routing filter. Note that most filters are
     *          not thread-safe, so they should not be shared among multiple
     *          connections.
     */
    public void setRouteFilter(SpamFilter filter);

    /**
     * Returns whether or not this connection is a push proxy for me.
     */
    public boolean isMyPushProxy();

    /**
     * Returns whether or not I'm a push proxy for this connection.
     */
    public boolean isPushProxyFor();

    /**
     * Sets whether or not I'm a push proxy for this connection.
     */
    public void setPushProxyFor(boolean pushProxyFor);

    public Object getQRPLock();

    /**
     * set preferencing for the responder (The preference of the Responder is
     * used when creating the response (in Connection.java: conclude..)).
     */
    public void setLocalePreferencing(boolean b);

    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * 
     * @see BandwidthTracker#measureBandwidth
     */
    public void measureBandwidth();

    /**
     * Returns the upstream bandwidth between the last two calls to
     * measureBandwidth.
     * 
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredUpstreamBandwidth();

    /**
     * Returns the downstream bandwidth between the last two calls to
     * measureBandwidth.
     * 
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredDownstreamBandwidth();

    public ConnectionRoutingStatistics getRoutedConnectionStatistics();

    public ConnectionMessageStatistics getConnectionMessageStatistics();

}