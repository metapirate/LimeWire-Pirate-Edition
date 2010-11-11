package org.limewire.core.api.connection;

import java.util.Properties;

import org.limewire.friend.api.FriendPresence;

/**
 * Defines a current Gnutella connection.
 */
public interface ConnectionItem {
    /**
     * Defines status codes.
     */
    public enum Status {
        CONNECTING, OUTGOING, INCOMING
    }
    
    /**
     * Returns true if the host address is resolved.
     */
    public boolean isAddressResolved();
    
    /**
     * Sets indicator to determine if the host address is resolved.
     */
    public void setAddressResolved(boolean resolved);
    
    /**
     * Returns a FriendPresence for the connection.
     */
    public FriendPresence getFriendPresence();
    
    public Properties getHeaderProperties();

    public String getHostName();

    public void setHostName(String hostName);
    
    public void resetHostName();
    
    public float getMeasuredDownstreamBandwidth();
    
    public float getMeasuredUpstreamBandwidth();
    
    public int getNumMessagesReceived();
    
    public int getNumMessagesSent();
    
    public long getNumReceivedMessagesDropped();
    
    public int getNumSentMessagesDropped();
    
    public int getPort();
    
    public int getQueryRouteTableEmptyUnits();
    
    public double getQueryRouteTablePercentFull();
    
    public int getQueryRouteTableSize();
    
    public int getQueryRouteTableUnitsInUse();
    
    public float getReadLostFromSSL();

    public float getReadSavedFromCompression();
    
    public float getSentLostFromSSL();

    public float getSentSavedFromCompression();

    public Status getStatus();
    
    public long getTime();

    public String getUserAgent();
    
    /**
     * Returns true if the connection is connected.
     */
    public boolean isConnected();

    /**
     * Returns true if the connection is a leaf.
     */
    public boolean isLeaf();

    /**
     * Returns true if the connection is outgoing.
     */
    public boolean isOutgoing();

    /**
     * Returns true if the connection is a peer.
     */
    public boolean isPeer();

    /**
     * Returns true if the remote host is an ultrapeer.
     */
    public boolean isUltrapeerConnection();
    
    /**
     * Returns true if the connection is an ultrapeer.
     */
    public boolean isUltrapeer();
    
    /**
     * Updates the connection status.
     */
    public void update();

}
