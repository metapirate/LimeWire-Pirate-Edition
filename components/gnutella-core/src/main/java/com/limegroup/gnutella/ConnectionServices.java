package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.Collection;

import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager.ConnectType;

import com.limegroup.gnutella.connection.RoutedConnection;

public interface ConnectionServices {

    /**
     * Accessor for whether or not this node is a shielded leaf.
     *
     * @return <tt>true</tt> if this node is a shielded leaf, 
     *  <tt>false</tt> otherwise
     */
    public boolean isShieldedLeaf();

    /**
     * Tells whether the node is currently connected to the network
     * as a supernode or not.
     * @return true, if active supernode, false otherwise
     */
    public boolean isActiveSuperNode();

    /**
     * Tells whether the node is a supernode or not.
     * NOTE: This will return true if this node is capable
     * of being a supernode but is not yet connected to 
     * the network as one (and is not a shielded leaf either).
     * 
     * @return true, if supernode, false otherwise
     */
    public boolean isSupernode();

    /**
     * Returns whether or not this client is attempting to connect.
     */
    public boolean isConnecting();

    /**
     * Returns whether or not this client currently has any initialized 
     * connections.
     *
     * @return <tt>true</tt> if the client does have initialized connections,
     *  <tt>false</tt> otherwise
     */
    public boolean isConnected();

    /**
     * Returns whether or not this client currently has any initialized 
     * connections.
     *
     * @return <tt>true</tt> if the client does have initialized connections,
     *  <tt>false</tt> otherwise
     */
    public boolean isFullyConnected();

    /**
     * Returns a collection of IpPorts, preferencing hosts with open slots.
     * If isUltrapeer is true, this preferences hosts with open ultrapeer slots,
     * otherwise it preferences hosts with open leaf slots.
     *
     * Preferences via locale, also.
     * 
     * @param num How many endpoints to try to get
     */
    public Collection<IpPort> getPreferencedHosts(boolean isUltrapeer,
            String locale, int num);

    /**
     * Count how many connections have already received N messages
     */
    public int countConnectionsWithNMessages(int messageThreshold);

    /**
     * Count up all the messages on active connections
     */
    public int getActiveConnectionMessages();

    /**
     * Closes and removes the given connection.
     */
    public void removeConnection(RoutedConnection c);

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public void disconnect();

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public void connect();

    /**
     * Determines if you're connected to the given host.
     */
    public boolean isConnectedTo(InetAddress addr);

    /**
     * Creates a new outgoing messaging connection to the given host and port. 
     * Returns immediately without blocking.  If hostname would connect
     * us to ourselves, returns immediately.
     */
    public void connectToHostAsynchronously(String hostname, int portnum,
            ConnectType type);

    /**
     *  Returns the number of initialized messaging connections.
     */
    public int getNumInitializedConnections();

}