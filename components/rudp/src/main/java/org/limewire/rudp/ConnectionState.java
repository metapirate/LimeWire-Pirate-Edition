package org.limewire.rudp;

public enum ConnectionState {
    /** The state on first creation before connection is established */
    PRECONNECT, 
    
    /** The state while connecting is occurring. */
    CONNECTING, 
    
    /** The state after a connection is established */
    CONNECTED, 
    
    /** The state after user communication during shutdown */
    FIN
}
