package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.lifecycle.Service;
import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;

public interface MessageRouter extends Service {

    public static final long CLEAR_TIME = 30 * 1000; // 30 seconds
    
    /**
     * Installs a MessageHandler for "regular" Messages.
     * 
     * @link #handleMessage(Message, RoutedConnection)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void setMessageHandler(Class<? extends Message> clazz,
            MessageHandler handler);

    /**
     * Adds the new handler as a handler in addition to other handlers.
     * 
     * @link #handleMessage(Message, RoutedConnection)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void addMessageHandler(Class<? extends Message> clazz,
            MessageHandler handler);

    /**
     * Returns a MessageHandler for the specified Message Class
     * or null if no such MessageHandler exists.
     */
    public MessageHandler getMessageHandler(Class<? extends Message> clazz);

    /**
     * Installs a MessageHandler for UDP Messages.
     * 
     * @link #handleUDPMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void setUDPMessageHandler(Class<? extends Message> clazz,
            MessageHandler handler);

    /**
     * Adds the new handler as a handler in addition to other handlers for UDP messages.
     * 
     * @link #handleUDPMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void addUDPMessageHandler(Class<? extends Message> clazz,
            MessageHandler handler);

    /**
     * Returns a MessageHandler for the specified Message Class
     * or null if no such MessageHandler exists.
     */
    public MessageHandler getUDPMessageHandler(Class<? extends Message> clazz);

    /**
     * Installs a MessageHandler for Multicast Messages.
     * 
     * @link #handleMulticastMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void setMulticastMessageHandler(Class<? extends Message> clazz,
            MessageHandler handler);

    /**
     * Adds the new handler as a handler in addition to other handlers for multicast messages.
     * 
     * @link #handleMulticastMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void addMulticastMessageHandler(Class<? extends Message> clazz,
            MessageHandler handler);

    /**
     * Returns a MessageHandler for the specified Message Class
     * or null if no such MessageHandler exists.
     */
    public MessageHandler getMulticastMessageHandler(
            Class<? extends Message> clazz);

    /**
     * Routes a query GUID to yourself.
     */
    public void originateQueryGUID(byte[] guid);

    /** Call this to inform us that a query has been killed by a user or
     *  whatever.  Useful for purging unneeded info.<br>
     *  Callers of this should make sure that they have purged the guid from
     *  their tables.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void queryKilled(GUID guid) throws IllegalArgumentException;

    /** Call this to inform us that a download is finished or whatever.  Useful
     *  for purging unneeded info.<br>
     *  If the caller is a Downloader, please be sure to clear yourself from the
     *  active and waiting lists in DownloadManager.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void downloadFinished(GUID guid) throws IllegalArgumentException;

    /** @returns a Set with GUESSEndpoints that had matches for the
     *  original query guid.  may be empty.
     *  @param guid the guid of the query you want endpoints for.
     */
    public Set<GUESSEndpoint> getQueryLocs(GUID guid);

    public String getPingRouteTableDump();

    public String getQueryRouteTableDump();

    public String getPushRouteTableDump();

    /**
     * The handler for all message types.  Processes a message based on the 
     * message type.
     *
     * @param m the <tt>Message</tt> instance to route appropriately
     * @param receivingConnection the <tt>ReplyHandler</tt> over which
     *  the message was received
     */
    public void handleMessage(Message msg, ReplyHandler receivingConnection);

    /**
     * The handler for all message types.  Processes a message based on the 
     * message type.
     *
     * @param msg the <tt>Message</tt> received
     * @param addr the <tt>InetSocketAddress</tt> containing the IP and 
     *  port of the client node
     */
    public void handleUDPMessage(Message msg, InetSocketAddress addr);

    /**
     * The handler for Multicast messages. Processes a message based on the
     * message type.
     *
     * @param msg the <tt>Message</tt> recieved.
     * @param addr the <tt>InetSocketAddress</tt> containing the IP and
     *  port of the client node.
     */
    public void handleMulticastMessage(Message msg, InetSocketAddress addr);

    /**
     * Adds the address of <code>handler</code> to the {@link BypassedResultsCache}
     * if it can receive unsolicited udp.
     * 
     * @return true if successfully added to the bypassed results cache
     */
    public boolean addBypassedSource(ReplyNumberVendorMessage reply,
            ReplyHandler handler);

    /**
     * Adds the address of <code>handler</code> to the {@link BypassedResultsCache}
     * if it is likely to not be firewalled.
     */
    public boolean addBypassedSource(QueryReply reply, ReplyHandler handler);

    /**
     * Returns the number of results to request from source of <code>reply</code>.
     * 
     * @return -1 if no results are desired
     */
    public int getNumOOBToRequest(ReplyNumberVendorMessage reply);

    /**
     * @return true if there is still a route for this reply
     */
    public boolean isQueryAlive(GUID guid);

    /**
     * Determines if we've sent a unicast OOB query to
     * the given host using the given query GUID.
     */
    public boolean isHostUnicastQueried(GUID guid, IpPort host);

    /**
     * Sends the ping request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest request,
            RoutedConnection connection);

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastPingRequest(PingRequest ping);

    /**
     * Generates a new dynamic query.  This method is used to send a new 
     * dynamic query from this host (the user initiated this query directly,
     * so it's replies are intended for this node).
     *
     * @param query the <tt>QueryRequest</tt> instance that generates
     *  queries for this dynamic query
     */
    public void sendDynamicQuery(QueryRequest query);

    /**
     * Forwards the query request to any leaf connections.
     *
     * @param request the query to forward
     * @param handler the <tt>ReplyHandler</tt> that responds to the
     *  request appropriately
     * @param manager the <tt>ConnectionManager</tt> that provides
     *  access to any leaf connections that we should forward to
     */
    public void forwardQueryRequestToLeaves(QueryRequest query,
            ReplyHandler handler);

    /**
     * Used to send the first request to a specific ultrapeer
     * when dynamic querying.
     *
     * @param request The query to send.
     * @param mc The RoutedConnection to send the query along
     * @return false if the query was not sent, true if so
     */
    public boolean sendInitialQuery(QueryRequest query, RoutedConnection mc);

    /**
     * The default handler for QueryReplies.  This implementation
     * uses the query route table to route a query reply.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the QueryReply count is incremented.<p>
     *
     * Override as desired, but you probably want to call super.handleQueryReply
     * if you do.  This is public for testing purposes.
     */
    public void handleQueryReply(QueryReply queryReply, ReplyHandler handler);

    /**
     * Uses the push route table to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPushRequest(PushRequest push) throws IOException;

    /**
     * Sends a push request to the multicast network.  No lookups are
     * performed in the push route table, because the message will always
     * be broadcast to everyone.
     */
    public void sendMulticastPushRequest(PushRequest push);

    /**
     * Converts the passed responses to QueryReplies. Each QueryReply can
     * accomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in case the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt have any side effect, 
     * and does not modify the state of this object
     * @param responses The responses to be converted
     * @param queryRequest The query request corresponding to which we are
     * generating query replies.
     * @return Iterable of QueryReply
     */
    public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
            QueryRequest queryRequest);
    
    public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
            QueryRequest queryRequest, int replyLimit, SecurityToken token);

    /**
     * Accessor for the most recently calculated <tt>QueryRouteTable</tt>
     * for this node.  If this node is an Ultrapeer, the table will include
     * all data for leaf nodes in addition to data for this node's files.
     *
     * @return the <tt>QueryRouteTable</tt> for this node
     */
    public QueryRouteTable getQueryRouteTable();

    /**
     * Adds the specified MessageListener for messages with this GUID.
     * You must manually unregister the listener.
     *
     * This works by replacing the necessary maps & lists, so that 
     * notifying doesn't have to hold any locks.
     */
    public void registerMessageListener(byte[] guid, MessageListener ml);

    /**
     * Unregisters this MessageListener from listening to the GUID.
     *
     * This works by replacing the necessary maps & lists so that
     * notifying doesn't have to hold any locks.
     */
    public void unregisterMessageListener(byte[] guid, MessageListener ml);

    /**
     * Time after which an OOB session should be expired.
     * @return
     */
    public long getOOBExpireTime();

    /**
     * Returns the push handler registered for the <code>guid</code>, could also
     * be {@link ForMeReplyHandler} or any of the leaves that are push proxied.
     * 
     * @param guid the client guid
     * 
     * @return null if no reply handler is registered for the guid
     */
    ReplyHandler getPushHandler(byte[] guid);
}