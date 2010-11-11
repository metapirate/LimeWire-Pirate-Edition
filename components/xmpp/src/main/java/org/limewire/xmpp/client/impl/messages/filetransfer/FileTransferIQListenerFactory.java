package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

public interface FileTransferIQListenerFactory {
    FileTransferIQListener create(XMPPFriendConnectionImpl connection);
}
