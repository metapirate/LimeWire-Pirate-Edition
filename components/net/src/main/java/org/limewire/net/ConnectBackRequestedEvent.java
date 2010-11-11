package org.limewire.net;

import org.limewire.listener.DefaultDataEvent;

/**
 * Event that an external connect back request has arrived.
 */
public class ConnectBackRequestedEvent extends DefaultDataEvent<ConnectBackRequest> {
    
    public ConnectBackRequestedEvent(ConnectBackRequest connectBackRequest) {
        super(connectBackRequest);
    }
    
}
