package org.limewire.core.api.connection;

/** Defines the various events during connection. */
public enum ConnectionLifecycleEventType {

    CONNECTING, 
    CONNECTED, 
    DISCONNECTED, 
    NO_INTERNET, 
    CONNECTION_INITIALIZING, 
    CONNECTION_INITIALIZED, 
    CONNECTION_CLOSED,
    CONNECTION_CAPABILITIES;
    
}