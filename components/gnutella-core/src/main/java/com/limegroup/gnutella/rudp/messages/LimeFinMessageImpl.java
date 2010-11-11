package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.FinMessage;

class LimeFinMessageImpl extends AbstractLimeRUDPMessage<FinMessage> implements FinMessage {

    LimeFinMessageImpl(FinMessage delegate) {
        super(delegate);
    }

}
