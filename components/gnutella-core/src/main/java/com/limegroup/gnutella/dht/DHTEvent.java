package com.limegroup.gnutella.dht;

import java.util.EventObject;

/**
 * <code>DHTEvent</code>s are fired for DHT state changes.
 */
public class DHTEvent extends EventObject {
    
    private static final long serialVersionUID = 912814275883336092L;

    /**
     * Defines the various type of <code>DHTEvent</code>s, either starting, 
     * connected or stopped.
     */
    public static enum Type {
        STARTING,
        CONNECTED,
        STOPPED;
    }
    
    private final Type type;

    public DHTEvent(DHTController source, Type type) {
        super(source);
        this.type = type;
    }

    public DHTController getDHTController() {
        return (DHTController)getSource();
    }
    
    public Type getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return type.toString();
    }
}
