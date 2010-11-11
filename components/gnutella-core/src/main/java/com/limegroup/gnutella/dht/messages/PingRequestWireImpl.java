package com.limegroup.gnutella.dht.messages;

import org.limewire.mojito.messages.PingRequest;

public class PingRequestWireImpl extends AbstractMessageWire<PingRequest> 
        implements PingRequest {
    
    PingRequestWireImpl(PingRequest delegate) {
        super(delegate);
    }
}
