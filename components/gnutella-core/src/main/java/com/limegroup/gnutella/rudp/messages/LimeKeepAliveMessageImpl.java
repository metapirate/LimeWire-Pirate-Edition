package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.KeepAliveMessage;

class LimeKeepAliveMessageImpl extends AbstractLimeRUDPMessage<KeepAliveMessage> implements
        KeepAliveMessage {

    LimeKeepAliveMessageImpl(KeepAliveMessage delegate) {
        super(delegate);
        // TODO Auto-generated constructor stub
    }

    public void extendWindowStart(long wStart) {
        delegate.extendWindowStart(wStart);
    }

    public int getWindowSpace() {
        return delegate.getWindowSpace();
    }

    public long getWindowStart() {
        return delegate.getWindowStart();
    }

}
