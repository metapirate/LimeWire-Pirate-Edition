package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

public interface ConnectBackRequestIQListenerFactory {
    ConnectBackRequestIQListener create(XMPPFriendConnectionImpl connection);
}
