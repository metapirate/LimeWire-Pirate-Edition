package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import org.limewire.mojito.messages.StoreResponse;


public class StoreResponseWireImpl extends AbstractMessageWire<StoreResponse> 
        implements StoreResponse {

    StoreResponseWireImpl(StoreResponse delegate) {
        super(delegate);
    }

    public Collection<StoreStatusCode> getStoreStatusCodes() {
        return delegate.getStoreStatusCodes();
    }
}
