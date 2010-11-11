package com.limegroup.gnutella.dht.messages;

import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.FindNodeRequest;


public class FindNodeRequestWireImpl extends AbstractMessageWire<FindNodeRequest> 
        implements FindNodeRequest {

    FindNodeRequestWireImpl(FindNodeRequest delegate) {
        super(delegate);
    }

    public KUID getLookupID() {
        return delegate.getLookupID();
    }
}
