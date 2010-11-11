package org.limewire.xmpp.client.impl;

import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.friend.api.FriendConnectionConfiguration;

/**
 * Creates XMPPConnectionImpls.  Used i conjunction with @Inject
 */
public interface XMPPConnectionImplFactory {
    XMPPFriendConnectionImpl createConnection(FriendConnectionConfiguration configuration,
                                        ListeningExecutorService executorService);
        
}
