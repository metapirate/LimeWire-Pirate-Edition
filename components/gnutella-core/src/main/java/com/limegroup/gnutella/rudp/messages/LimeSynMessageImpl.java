package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.SynMessage;

class LimeSynMessageImpl extends AbstractLimeRUDPMessage<SynMessage> implements SynMessage {

    LimeSynMessageImpl(SynMessage delegate) {
        super(delegate);
    }

    public int getProtocolVersionNumber() {
        return delegate.getProtocolVersionNumber();
    }

    public byte getSenderConnectionID() {
        return delegate.getSenderConnectionID();
    }

    @Override
    public Role getRole() {
        return delegate.getRole();
    }

}
