package com.limegroup.gnutella;


import java.net.Socket;
import java.util.List;
import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventListener;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.SocketsManager.ConnectType;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.GnutellaConnectionEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * The list of all RoutedConnection's.  Provides a factory method for creating
 * user-requested outgoing connections, accepts incoming connections, and
 * fetches "automatic" outgoing connections as needed.  Creates threads for
 * handling these connections when appropriate.
 *
 * Because this is the only list of all connections, it plays an important role
 * in message broadcasting.  For this reason, the code is highly tuned to avoid
 * locking in the getInitializedConnections() methods.  Adding and removing
 * connections is a slower operation.<p>
 *
 * LimeWire follows the following connection strategy:<br>
 * As a leaf, LimeWire will ONLY connect to 'good' Ultrapeers.  The definition
 * of good is constantly changing.  For a current view of 'good', review
 * HandshakeResponse.isGoodUltrapeer().  LimeWire leaves will NOT deny
 * a connection to an ultrapeer even if they've reached their maximum
 * desired number of connections (currently 3).  This means that if 4
 * connections resolve simultaneously, the leaf will remain connected to all 4.
 * <br>
 * As an Ultrapeer, LimeWire will seek outgoing connections for 5 less than
 * the number of it's desired peer slots.  This is done so that newcomers
 * on the network have a better chance of finding an ultrapeer with a slot
 * open.  LimeWire ultrapeers will allow ANY other ultrapeer to connect to it,
 * and to ensure that the network does not become too LimeWire-centric, it
 * reserves 3 slots for non-LimeWire peers.  LimeWire ultrapeers will allow
 * ANY leaf to connect, so long as there are atleast 15 slots open.  Beyond
 * that number, LimeWire will only allow 'good' leaves.  To see what consitutes
 * a good leaf, view HandshakeResponse.isGoodLeaf().  To ensure that the
 * network does not remain too LimeWire-centric, it reserves 2 slots for
 * non-LimeWire leaves.<p>
 *
 * ConnectionManager has methods to get up and downstream bandwidth, but it
 * doesn't quite fit the BandwidthTracker interface.
 */
public interface ConnectionManager extends ConnectionAcceptor, 
        EventDispatcher<ConnectionLifecycleEvent, ConnectionLifecycleListener>, EventListener<GnutellaConnectionEvent> {
    
    /** How many connect back requests to send if we have a single connection */
    public static final int CONNECT_BACK_REDUNDANT_REQUESTS = 3;
    /**
     * The number of leaf connections reserved for non LimeWire clients.
     * This is done to ensure that the network is not solely LimeWire centric.
     */
    public static final int RESERVED_NON_LIMEWIRE_LEAVES = 2;
    
   

    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void start();
    
    /**
     * Create a new connection, allowing it to initialize and loop for messages on a new thread.
     */
    public void createConnectionAsynchronously(String hostname, int portnum, ConnectType type);

    /**
     * Create an incoming connection.
     * 
     * If the connection can support asynchronous messaging, this method will return
     * immediately.  Otherwise, this will block forever while the connection handshakes
     * and then loops for messages (it will return when the connection dies).
     */
     void acceptConnection(Socket socket);


    /**
     * Removes the specified connection from currently active connections, also
     * removing this connection from routing tables and modifying active
     * connection fetchers accordingly.
     *
     * @param mc the <tt>RoutedConnection</tt> instance to remove
     */
    public void remove(RoutedConnection mc); 
    
    /**
     * True if this is currently or wants to be a supernode,
     * otherwise false.
     */
    public boolean isSupernode();
    
    /** Return true if we are not a private address, have been ultrapeer capable
     *  in the past, and are not being shielded by anybody, we don't have UP
     *  mode disabled AND we are not exclusively a DHT node.
     */
    public boolean isSupernodeCapable();
    
    
    /**
     * @return if we are currently using a http or socks4/5 proxy to connect.
     */
    public boolean isBehindProxy();
    
    /**
     * Tells whether or not we're actively being a supernode to anyone.
     */
    public boolean isActiveSupernode();

    /**
     * Returns true if this is a leaf node with a connection to a ultrapeer.  It
     * is not required that the ultrapeer support query routing, though that is
     * generally the case.
     */
    public boolean isShieldedLeaf();

    /**
     * Returns true if this is a super node with a connection to a leaf.
     */
    public boolean hasSupernodeClientConnection();

    /**
     * Returns whether or not this node has any available connection
     * slots.  This is only relevant for Ultrapeers -- leaves will
     * always return <tt>false</tt> to this call since they do not
     * accept any incoming connections, at least for now.
     *
     * @return <tt>true</tt> if this node is an Ultrapeer with free
     *  leaf or Ultrapeer connections slots, otherwise <tt>false</tt>
     */
    public boolean hasFreeSlots();

    /**
     * Returns whether this (probably) has a connection to the given host.  This
     * method is currently implemented by iterating through all connections and
     * comparing addresses but not ports.  (Incoming connections use ephemeral
     * ports.)  As a result, this test may conservatively return true even if
     * this is not connected to <tt>host</tt>.  Likewise, it may it mistakenly
     * return false if <tt>host</tt> is a multihomed system.  In the future,
     * additional connection headers may make the test more precise.
     *
     * @return true if this is probably connected to <tt>host</tt>
     */
    boolean isConnectedTo(String hostName);
    
    /**
     * Returns true if we're currently attempting to connect to a particular host.
     * This checks both the Ip & Port.
     */
    boolean isConnectingTo(IpPort host);
    
    /**
     * @return the number of connections, which is greater than or equal
     *  to the number of initialized connections.
     */
    public int getNumConnections();

    /**
     * @return the number of initialized connections, which is less than or
     *  equals to the number of connections.
     */
    public int getNumInitializedConnections();

    /**
     * @return the number of initializedclient connections, which is less than
     * or equals to the number of connections.
     */
    public int getNumInitializedClientConnections();

    /**
     *@return the number of initialized connections for which
     * isClientSupernodeConnection is true.
     */
    public int getNumClientSupernodeConnections();

    /**
     *@return the number of ultrapeer -> ultrapeer connections.
     */
    public int getNumUltrapeerConnections();

    /**
     *@return the number of old unrouted connections.
     */
    public int getNumOldConnections();

    /**
     * @return the number of free leaf slots.
     */
    public int getNumFreeLeafSlots();

    /**
     * @return the number of free leaf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireLeafSlots();


    /**
     * @return the number of free non-leaf slots.
     */
    public int getNumFreeNonLeafSlots();

    /**
     * @return the number of free non-leaf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireNonLeafSlots();
    
    /**
     * Returns true if we've made a locale-matching connection (or don't
     * want any at all).
     */
    public boolean isLocaleMatched();
    
    /**
     * @return the number of locale reserved slots to be filled
     *
     * An ultrapeer may not have Free LimeWire Non Leaf Slots but may still
     * have free slots that are reserved for locales
     */
    public int getNumLimeWireLocalePrefSlots();
    
    /**
     * Determines if we've reached our maximum number of preferred connections.
     */
    public boolean isFullyConnected();
    
	/**
	 * Returns whether or not the client has an established connection with
	 * another Gnutella client.
	 *
	 * @return <tt>true</tt> if the client is currently connected to
	 *  another Gnutella client, <tt>false</tt> otherwise
	 */
	public boolean isConnected();
	
	/**
	 * Returns whether or not we are currently attempting to connect to the
	 * network.
	 */
	public boolean isConnecting();

    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public void measureBandwidth();

    /**
     * Returns the upstream bandwidth in kbytes/sec between the last two calls
     * to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredUpstreamBandwidth();

    /**
     * Returns the downstream bandwidth in kbytes/sec between the last two calls
     * to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredDownstreamBandwidth();

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer,
     * temporary etc).
     * @param c The connection we received, for which to
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    public HandshakeStatus allowConnectionAsLeaf(HandshakeResponse hr);

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer,
     * temporary etc).
     * @param c The connection we received, for which to
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
     public HandshakeStatus allowConnection(HandshakeResponse hr);


    /**
     * Checks if there is any available slot of any kind.
     * @return true, if we have incoming slot of some kind,
     * false otherwise
     */
    public boolean allowAnyConnection();
    
    /**
     * Returns true if this has slots for an incoming connection, <b>without
     * accounting for this' ultrapeer capabilities</b>.  More specifically:
     * <ul>
     * <li>if ultrapeerHeader==null, returns true if this has space for an
     *  unrouted old-style connection.
     * <li>if ultrapeerHeader.equals("true"), returns true if this has slots
     *  for a leaf connection.
     * <li>if ultrapeerHeader.equals("false"), returns true if this has slots
     *  for an ultrapeer connection.
     * </ul>
     *
     * <tt>useragentHeader</tt> is used to prefer LimeWire and certain trusted
     * vendors.  <tt>outgoing</tt> is currently unused, but may be used to
     * prefer incoming or outgoing connections in the forward.
     *
     * @param outgoing true if this is an outgoing connection; true if incoming
     * @param ultrapeerHeader the value of the X-Ultrapeer header, or null
     *  if it was not written
     * @param useragentHeader the value of the User-Agent header, or null if
     *  it was not written
     * @return true if a connection of the given type is allowed
     */
    public HandshakeStatus allowConnection(HandshakeResponse hr, boolean leaf);
    
    /**
     * Tells if this node thinks that more ultrapeers are needed on the
     * network. This method should be invoked on a ultrapeer only, as
     * only ultrapeer may have required information to make informed
     * decision.
     * @return true, if more ultrapeers needed, false otherwise
     */
    public boolean supernodeNeeded();

    /**
     * Returns a list of this' initialized connections.
     */
    public List<RoutedConnection> getInitializedConnections();

    /**
     * return a list of initialized connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List<RoutedConnection> getInitializedConnectionsMatchLocale(String loc);

    /**
     * Returns a list of this' initialized connections.
     */
    public List<RoutedConnection> getInitializedClientConnections();

    /**
     * return a list of initialized client connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List<RoutedConnection> getInitializedClientConnectionsMatchLocale(String loc);

    /**
     * @return all of this' connections.
     */
    public List<RoutedConnection> getConnections();

    /**
     * Accessor for the <tt>Set</tt> of push proxies for this node.  If
     * there are no push proxies available, this will return an empty <tt>Set</tt>.
     *
     * Callers can take ownership of the returned set; the set might be immutable.
     *
     * @return a <tt>Set</tt> of push proxies with a maximum size of 4
     *
     *  TODO: should the set of pushproxy UPs be cached and updated as
     *  connections are killed and created?
     */
    public Set<Connectable> getPushProxies();

    /**
     * Sends a TCPConnectBack request to (up to) 2 connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendTCPConnectBackRequests();

    /**
     * Sends a UDPConnectBack request to (up to) 4 (and at least 2)
     * connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendUDPConnectBackRequests(GUID cbGuid);
    
    /**
     * Sends a QueryStatusResponse message to as many Ultrapeers as possible.
     *
     * @param
     */
    public void updateQueryStatus(QueryStatusResponse stat);
    
	/**
	 * Returns the <tt>Endpoint</tt> for an Ultrapeer connected via TCP,
	 * if available.
	 *
	 * @return the <tt>Endpoint</tt> for an Ultrapeer connected via TCP if
	 *  there is one, otherwise returns <tt>null</tt>
	 */
	public Endpoint getConnectedGUESSUltrapeer();


    /** Returns a <tt>List<tt> of Ultrapeers connected via TCP that are GUESS
     *  enabled.
     *
     * @return A non-null List of GUESS enabled, TCP connected Ultrapeers.  The
     * are represented as ManagedConnections.
     */
	public List<RoutedConnection> getConnectedGUESSUltrapeers();
    
    /**
     * Adds an incoming connection to the list of connections. Note that
     * the incoming connection has already been initialized before
     * this method is invoked.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection, for
     * incoming connections
     * 
     * Default access for testing.
     */
    void connectionInitializingIncoming(RoutedConnection c);

    /**
     * Marks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initialization.
     * Should only be called from a thread that has this' monitor.
     * 
     * Default access for testing.
     */
    boolean connectionInitialized(RoutedConnection c);
        
    /**
     * Iterates over all the connections and sends the updated CapabilitiesVM
     * down every one of them.
     */
    public void sendUpdatedCapabilities();

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     * 
     * @param willTryToReconnect Whether or not this is only a temporary disconnection
     */
    public void disconnect(boolean willTryToReconnect);
    
    /**
     * Returns this node's average connection time - in ms - including the current session.
     * 
     */
    public long getCurrentAverageUptime();
    
    /**
     * Connects to the network.  Ensures the number of messaging connections
     * is non-zero and recontacts the pong server as needed.
     */
    public void connect();
    
    /**
     * Returns true if this can safely switch from Ultrapeer to leaf mode.
	 * Typically this means that we are an Ultrapeer and have no leaf
	 * connections.
	 *
	 * @return <tt>true</tt> if we will allow ourselves to become a leaf,
	 *  otherwise <tt>false</tt>
     */
    public boolean allowLeafDemotion();

	/**
	 * Notifies the connection manager that it should attempt to become an
	 * Ultrapeer.  If we already are an Ultrapeer, this will be ignored.
	 *
	 * @param demotionLimit the number of attempts by other Ultrapeers to
	 *  demote us to a leaf that we should allow before giving up in the
	 *  attempt to become an Ultrapeer
	 */
	public void tryToBecomeAnUltrapeer(int demotionLimit);

    
    /**
     * Gets the number of preferred connections to maintain.
     */
    public int getPreferredConnectionCount();

    /**
     * Determines if we're attempting to maintain the idle connection count.
     */
    public boolean isConnectionIdle();

    /**
     * This method notifies the connection manager that the user does not have
     * a live connection to the Internet to the best of our determination.
     * In this case, we notify the user with a message and maintain any
     * Gnutella hosts we have already tried instead of discarding them.
     */
    public void noInternetConnection();
        
    /**
     * Count how many connections have already received N messages
     */
    public int countConnectionsWithNMessages(int messageThreshold);
    
    /**
     * Count up all the messages on active connections
     */
    public int getActiveConnectionMessages();
    
    /** 
     * @return true if a connect back request can be sent on the provided network
     */
    public boolean canSendConnectBack(Network network);
    
    /**
     * notification that a connect back request has been sent on the given network 
     */
    public void connectBackSent(Network network);

    /** Returns the number of connections that are currently being fetched. */
    public int getNumFetchingConnections();
    
}
