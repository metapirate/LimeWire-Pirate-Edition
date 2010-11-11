package org.limewire.xmpp.client.impl.messages.authtoken;

import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

public interface AuthTokenIQListenerFactory {
    AuthTokenIQListener create(XMPPFriendConnectionImpl connection);
}
