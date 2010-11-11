package org.limewire.core.impl.connection;

import java.util.Properties;

import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.impl.friend.GnutellaPresence;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.ConnectableImpl;

import com.limegroup.gnutella.connection.RoutedConnection;

/**
 * Live implementation of a ConnectionItem.  This is the rough equivalent of 
 * the ConnectionDataLine class in the old gui.
 * 
 * <p>We no longer implement <code>getPercent[Sent|Received]Dropped()</code>,
 * which had the side-effect of altering the connection's statistics.  Instead,
 * we implement <code>getNum[Sent|Received]MessagesDropped()</code> and   
 * <code>getNumMessages[Sent|Received]()</code>.  This provides more accurate
 * statistics anyway, rather than a snapshot-erase-style number.</p>
 */
public class CoreConnectionItem implements ConnectionItem {
    
    /** Connection object. */
    private final RoutedConnection routedConnection;
    
    /** Cached host. */
    private volatile String host;

    /** Cached status. */
    private Status status;

    /** Time this connected or initialized. */
    private long time;

    /** Whether or not the host name has been resolved for this connection. */
    private boolean addressResolved = false;

    /** Whether or not this dataline is in the 'connecting' state. */
    private boolean connecting = true;

    /**
     * Constructs a CoreConnectionItem for the specified connection.
     */
    public CoreConnectionItem(RoutedConnection routedConnection) {
        this.routedConnection = routedConnection;

        // Initialize attributes.
        host = routedConnection.getAddress();
        status = Status.CONNECTING;
        time = System.currentTimeMillis();
    }
    
    /**
     * Returns true if the host address is resolved.
     */
    @Override
    public boolean isAddressResolved() {
        return addressResolved;
    }

    /**
     * Sets indicator to determine if the host address is resolved.
     */
    @Override
    public void setAddressResolved(boolean resolved) {
        addressResolved = resolved;
    }
    
    /**
     * Returns a FriendPresence for the connection.
     */
    @Override
    public FriendPresence getFriendPresence() {
        String id = routedConnection.getAddress() + ":" + routedConnection.getPort();
        // copy construct connectable to give it full equals semantics
        return new GnutellaPresence.GnutellaPresenceWithString(new ConnectableImpl(routedConnection), id);
    }

    @Override
    public Properties getHeaderProperties() {
        return routedConnection.getConnectionCapabilities().getHeadersRead().props();
    }
    
    @Override
    public String getHostName() {
        return host;
    }
    
    @Override
    public void setHostName(String hostName) {
        host = hostName;
    }
    
    @Override
    public void resetHostName() {
        host = routedConnection.getInetAddress().getHostAddress();
        addressResolved = false;
    }
    
    @Override
    public float getMeasuredDownstreamBandwidth() {
        return routedConnection.getMeasuredDownstreamBandwidth();
    }
    
    @Override
    public float getMeasuredUpstreamBandwidth() {
        return routedConnection.getMeasuredUpstreamBandwidth();
    }
    
    @Override
    public int getNumMessagesReceived() {
        return routedConnection.getConnectionMessageStatistics().getNumMessagesReceived();
    }
    
    @Override
    public int getNumMessagesSent() {
        return routedConnection.getConnectionMessageStatistics().getNumMessagesSent();
    }
    
    @Override
    public long getNumReceivedMessagesDropped() {
        return routedConnection.getConnectionMessageStatistics().getNumReceivedMessagesDropped();
    }
    
    @Override
    public int getNumSentMessagesDropped() {
        return routedConnection.getConnectionMessageStatistics().getNumSentMessagesDropped();
    }
    
    @Override
    public int getPort() {
        return routedConnection.getPort();
    }
    
    @Override
    public int getQueryRouteTableEmptyUnits() {
        return routedConnection.getRoutedConnectionStatistics().getQueryRouteTableEmptyUnits();
    }
    
    @Override
    public double getQueryRouteTablePercentFull() {
        return routedConnection.getRoutedConnectionStatistics().getQueryRouteTablePercentFull();
    }
    
    @Override
    public int getQueryRouteTableSize() {
        return routedConnection.getRoutedConnectionStatistics().getQueryRouteTableSize();
    }
    
    @Override
    public int getQueryRouteTableUnitsInUse() {
        return routedConnection.getRoutedConnectionStatistics().getQueryRouteTableUnitsInUse();
    }
    
    @Override
    public float getReadLostFromSSL() {
        return routedConnection.getConnectionBandwidthStatistics().getReadLostFromSSL();
    }
    
    @Override
    public float getReadSavedFromCompression() {
        return routedConnection.getConnectionBandwidthStatistics().getReadSavedFromCompression();
    }
    
    @Override
    public float getSentLostFromSSL() {
        return routedConnection.getConnectionBandwidthStatistics().getSentLostFromSSL();
    }
    
    @Override
    public float getSentSavedFromCompression() {
        return routedConnection.getConnectionBandwidthStatistics().getSentSavedFromCompression();
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public long getTime() {
        return time;
    }
    
    @Override
    public String getUserAgent() {
        return routedConnection.getConnectionCapabilities().getUserAgent();
    }
    
    /**
     * Returns true if the connection is connected.
     */
    @Override
    public boolean isConnected() {
        return !connecting;
    }
    
    /**
     * Returns true if the connection is in the process of connecting.
     */
    public boolean isConnecting() {
        return connecting;
    }

    /**
     * Returns true if the connection is a leaf.
     */
    @Override
    public boolean isLeaf() {
        return routedConnection.isSupernodeClientConnection();
    }

    /**
     * Returns true if the connection is outgoing.
     */
    @Override
    public boolean isOutgoing() {
        return routedConnection.isOutgoing();
    }

    /**
     * Returns true if the connection is a peer.
     */
    @Override
    public boolean isPeer() {
        return routedConnection.getConnectionCapabilities().isSupernodeSupernodeConnection();
    }

    /**
     * Returns true if the remote host is an ultrapeer.
     */
    @Override
    public boolean isUltrapeerConnection() {
        return routedConnection.getConnectionCapabilities().isSupernodeConnection();
    }
    
    /**
     * Returns true if the connection is an ultrapeer.
     */
    @Override
    public boolean isUltrapeer() {
        return routedConnection.getConnectionCapabilities().isClientSupernodeConnection();
    }
    
    /**
     * Updates this connection from a 'connecting' to a 'connected' state.
     */
    @Override
    public void update() {
        connecting = false;

        boolean outgoing = routedConnection.isOutgoing();

        status = outgoing ? Status.OUTGOING : Status.INCOMING;

        host = routedConnection.getInetAddress().getHostAddress();

        // Note that a successful connection is no longer added to the input 
        // field dictionary for "Add Connection". 

        time = routedConnection.getConnectionTime();
    }

    /**
     * Returns the connection object associated with this connection item. 
     */
    public RoutedConnection getRoutedConnection() {
        return routedConnection;
    }
    
}
