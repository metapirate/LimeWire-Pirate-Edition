package com.limegroup.gnutella.dht.messages;

import org.limewire.mojito.messages.StatsResponse;

public class StatsResponseWireImpl extends AbstractMessageWire<StatsResponse> 
        implements StatsResponse {

    StatsResponseWireImpl(StatsResponse delegate) {
        super(delegate);
    }

    public byte[] getStatistics() {
        return delegate.getStatistics();
    }
}
