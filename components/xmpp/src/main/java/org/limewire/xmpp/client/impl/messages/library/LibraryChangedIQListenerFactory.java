package org.limewire.xmpp.client.impl.messages.library;

import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

public interface LibraryChangedIQListenerFactory {
    LibraryChangedIQListener create(XMPPFriendConnectionImpl connection);
}
