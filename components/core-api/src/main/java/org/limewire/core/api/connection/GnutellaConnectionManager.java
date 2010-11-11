package org.limewire.core.api.connection;

import java.beans.PropertyChangeListener;

import ca.odell.glazedlists.EventList;

/**
 * Defines the interface for Gnutella connection management.
 */
public interface GnutellaConnectionManager {

    public static final String CONNECTION_STRENGTH = "strength";

    /**
     * Adds the specified listener to the list that is notified when a 
     * property value is changed.  
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value is changed.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /** 
     * Returns true if we are connected to the Gnutella network. 
     */
    public boolean isConnected();   

    /** 
     * Returns true if the node is currently an ultrapeer. 
     */
    public boolean isUltrapeer();

    /** 
     * Connects to the Gnutella network. 
     */
    public void connect();   

    /** 
     * Disconnects from the Gnutella network. 
     */
    public void disconnect();   

    /** 
     * Disconnects & reconnects to Gnutella. 
     */
    public void restart();   
    
    /**
     * Returns the current strength of the Gnutella connections. 
     */
    public ConnectionStrength getConnectionStrength();

    /**
     * Returns the list of connections.
     */
    public EventList<ConnectionItem> getConnectionList();
    
    /**
     * Removes the specified connection from the list.
     */
    public void removeConnection(ConnectionItem item);
    
    /**
     * Attempts to establish a connection to the specified host and port.
     */
    public void tryConnection(String hostname, int portnum, boolean useTLS);

}
