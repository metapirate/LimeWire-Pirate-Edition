package com.limegroup.gnutella.dht.messages;

import java.security.Signature;
import java.security.SignatureException;

import org.limewire.mojito.messages.StatsRequest;


public class StatsRequestWireImpl extends AbstractMessageWire<StatsRequest> 
        implements StatsRequest {

    StatsRequestWireImpl(StatsRequest delegate) {
        super(delegate);
    }

    public byte[] getSecureSignature() {
        return delegate.getSecureSignature();
    }

    public Status getSecureStatus() {
        return delegate.getSecureStatus();
    }

    public void setSecureStatus(Status secureStatus) {
        delegate.setSecureStatus(secureStatus);
    }

    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
        delegate.updateSignatureWithSecuredBytes(signature);
    }

    public StatisticType getType() {
        return delegate.getType();
    }

    public boolean isSecure() {
        return delegate.isSecure();
    }
}
