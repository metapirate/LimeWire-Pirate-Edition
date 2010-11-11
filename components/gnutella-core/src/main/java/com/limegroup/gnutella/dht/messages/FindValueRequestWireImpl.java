package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.messages.FindValueRequest;


public class FindValueRequestWireImpl extends AbstractMessageWire<FindValueRequest> 
        implements FindValueRequest {

    FindValueRequestWireImpl(FindValueRequest delegate) {
        super(delegate);
    }

    public KUID getLookupID() {
        return delegate.getLookupID();
    }

    public Collection<KUID> getSecondaryKeys() {
        return delegate.getSecondaryKeys();
    }

    public DHTValueType getDHTValueType() {
        return delegate.getDHTValueType();
    }
}
