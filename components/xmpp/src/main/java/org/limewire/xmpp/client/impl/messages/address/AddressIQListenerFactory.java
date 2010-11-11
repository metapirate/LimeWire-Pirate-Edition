package org.limewire.xmpp.client.impl.messages.address;

import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;
import org.limewire.net.address.AddressFactory;

public interface AddressIQListenerFactory {
    AddressIQListener create(XMPPFriendConnectionImpl connection, AddressFactory factory);
}
