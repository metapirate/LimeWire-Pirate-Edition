package com.limegroup.gnutella.connection;

import java.util.EventObject;

import org.limewire.core.api.connection.ConnectionLifecycleEventType;

public class ConnectionLifecycleEvent extends EventObject {
    
    private final RoutedConnection connection;
    private final ConnectionLifecycleEventType type;
    
    public ConnectionLifecycleEvent(Object source, ConnectionLifecycleEventType type, RoutedConnection c) {
        super(source);
        this.connection = c;
        this.type = type;
    }

    /**
     * Constructs a ConnectionLifecycleEvent with no connection associated.
     * This is useful for CONNECTED, DISCONNECTED, NO_INTERNET and
     * ADDRESS_CHANGED events.
     * 
     */
    public ConnectionLifecycleEvent(Object source, ConnectionLifecycleEventType type) {
        this(source, type, null);
    }

    public ConnectionLifecycleEventType getType() {
        return type;
    }

    public RoutedConnection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type.equals(ConnectionLifecycleEventType.CONNECTING)); 
    }
    
    public boolean isConnectedEvent() {
        return (type.equals(ConnectionLifecycleEventType.CONNECTED));
    }
    
    public boolean isDisconnectedEvent() {
        return (type.equals(ConnectionLifecycleEventType.DISCONNECTED));
    }
    
    public boolean isNoInternetEvent() {
        return (type.equals(ConnectionLifecycleEventType.NO_INTERNET));
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type.equals(ConnectionLifecycleEventType.CONNECTION_INITIALIZING));
    }
    
    public boolean isConnectionClosedEvent() {
        return (type.equals(ConnectionLifecycleEventType.CONNECTION_CLOSED));
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type.equals(ConnectionLifecycleEventType.CONNECTION_INITIALIZED));
    }
    
    public boolean isConnectionCapabilitiesEvent() {
        return (type.equals(ConnectionLifecycleEventType.CONNECTION_CAPABILITIES));
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("ConnectionLifecycleEvent: [event=");
        buffer.append(type);
        buffer.append(", connection=");
        if(connection == null) {
            buffer.append("null");
        } else {
            buffer.append(connection.toString());
        }
        return buffer.append("]").toString();
    }
}
