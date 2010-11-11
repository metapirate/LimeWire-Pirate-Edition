package org.limewire.xmpp.client.impl;

import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.io.UnresolvedIpPort;

/**
 * Uses the list of default servers in the XMPPConnectionConfiguration to return
 * a ConnectionConfiguration. 
 */
public class FallbackConnectionConfigurationFactory implements ConnectionConfigurationFactory {

    @Override
    public boolean hasMore(FriendConnectionConfiguration connectionConfiguration, RequestContext requestContext) {
        return requestContext.getNumRequests() < connectionConfiguration.getDefaultServers().size();
    }

    @Override
    public ConnectionConfiguration getConnectionConfiguration(FriendConnectionConfiguration connectionConfiguration,
                                                              RequestContext requestContext) {
        checkHasMore(connectionConfiguration, requestContext);
        List<UnresolvedIpPort> defaultServers = connectionConfiguration.getDefaultServers();
        UnresolvedIpPort defaultServer = defaultServers.get(requestContext.getNumRequests());
        return new ConnectionConfiguration(defaultServer.getAddress(),
                defaultServer.getPort(), connectionConfiguration.getServiceName());
    }

    private void checkHasMore(FriendConnectionConfiguration connectionConfiguration, RequestContext requestContext) {
        if(!hasMore(connectionConfiguration, requestContext)) {
            throw new IllegalStateException("no more ConnectionConfigurations");
        }
    }
}
