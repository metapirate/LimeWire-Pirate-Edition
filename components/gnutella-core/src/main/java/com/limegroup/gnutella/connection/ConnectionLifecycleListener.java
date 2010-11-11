package com.limegroup.gnutella.connection;

import java.util.EventListener;


public interface ConnectionLifecycleListener extends EventListener{

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt);
    
}
