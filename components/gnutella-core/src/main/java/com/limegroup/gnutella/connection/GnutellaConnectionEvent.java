package com.limegroup.gnutella.connection;

import org.limewire.listener.DefaultSourceTypeEvent;

public class GnutellaConnectionEvent extends DefaultSourceTypeEvent<GnutellaConnection, GnutellaConnection.EventType> {
    public GnutellaConnectionEvent(GnutellaConnection source, GnutellaConnection.EventType event) {
        super(source, event);
    }
}
