package org.limewire.friend.api;

import org.limewire.concurrent.ListeningFuture;


/**
 * Describes an interface for managing XMPP connections. Only one connection
 * can be logged in at a time.
 */
public interface FriendConnectionFactory {

    /**
     * Attempts to log in a connection using the specified configuration.
     * Any existing connections will be logged out first.
     * 
     * @param configuration the XMPPConnectionConfiguration to use; can not be null
     * @return a {@link ListeningFuture} of {@link FriendConnection}
     */
    public ListeningFuture<FriendConnection> login(FriendConnectionConfiguration configuration);

    public void register(FriendConnectionFactoryRegistry registry);
    
    ListeningFuture<String> requestLoginUrl(FriendConnectionConfiguration configuration);
    
}
