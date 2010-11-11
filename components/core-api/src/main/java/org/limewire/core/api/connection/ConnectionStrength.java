package org.limewire.core.api.connection;

/** The strength of your Gnutella connection. */
public enum ConnectionStrength {
    // THE IDs associated with each enum SHOULD NEVER CHANGE.
    // We are purposely NOT using the ordinal, because
    // ordinals can change over time.  These ids cannot,
    // because they are used by external code.
    
    /** You aren't connected to the Internet at all. */
    NO_INTERNET(0, false),
    
    /** You might be connected to the Internet, but not connected to Gnutella. */
    DISCONNECTED(1, false),
    
    /** You are attempting to connect to Gnutella. */
    CONNECTING(2, false),
    
    /** You have a weak connection to Gnutella. */
    WEAK(3, true),
    
    /** You have a slightly better than weak connection to Gnutella. **/
    WEAK_PLUS(4, true),
    
    /** Your connection to Gnutella is OK, but could be better. */
    MEDIUM(5, true), 
    
    /** Your connection to Gnutella is a little better than OK, but still, it could be better. */
    MEDIUM_PLUS(6, true),
    
    /** You are fully connected to Gnutella. */
    FULL(7, true), 
    
    /** You have a kickass connection to Gnutella. */
    TURBO(8, true);
    
    private final int strengthId;
    private final boolean online;
    
    private ConnectionStrength(int id, boolean online) {
        this.strengthId = id;
        this.online = online;
    }

    /**
     * Returns the ID associated with this strength. A given strength's ID will
     * never change.
     */
    public int getStrengthId() {
        return strengthId;
    }
    
    /**
     * Returns true if this connection strength indicates the computer has an
     * internet connection.
     */
    public boolean isOnline() {
        return online;
    }
}