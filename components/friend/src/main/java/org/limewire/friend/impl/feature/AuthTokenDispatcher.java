package org.limewire.friend.impl.feature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;

/**
 * Installs {@link AuthTokenFeatureInitializer} in the feature registry
 * and attaches {@link AuthTokenFeature auth token features} to presences
 * when {@link #featureReceived(String, AuthToken)} is called.
 */
@EagerSingleton
class AuthTokenDispatcher implements FeatureTransport.Handler<AuthToken>{
    
    private static final Log LOG = LogFactory.getLog(AuthTokenDispatcher.class);
    
    private final Map<String, AuthToken> pendingAuthTokens;
    private final AuthTokenRegistry authenticator;
    private final Set<FriendConnection> connections;
    
    @Inject
    AuthTokenDispatcher(AuthTokenRegistry authenticator,
                     FeatureRegistry featureRegistry) {
        this.authenticator = authenticator;
        this.connections = new HashSet<FriendConnection>();
        this.pendingAuthTokens = new HashMap<String, AuthToken>();
        new AuthTokenFeatureInitializer().register(featureRegistry);
    }

    @Inject
    void register(ListenerSupport<FriendConnectionEvent> connectionEventListenerSupport) {
        connectionEventListenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                    case CONNECTED:
                        connections.add(event.getSource()); 
                        break;
                    case DISCONNECTED:
                        connections.remove(event.getSource());
                }
            }
        });
    }
    
    @Override
    public void featureReceived(String from, final AuthToken feature) {
        for(FriendConnection connection : connections) {
            synchronized (this) {
                Friend user = connection.getFriend(PresenceUtils.parseBareAddress(from));
                if (user != null) {
                    FriendPresence presence = user.getPresences().get(from);
                    if(presence != null) {
                        LOG.debugf("updating auth token on presence {0} to {1}", presence, feature);
                        presence.addFeature(new AuthTokenFeature(feature));
                    }  else {
                        LOG.debugf("auth token {0} for presence {1} is pending", feature, from);
                        pendingAuthTokens.put(from, feature);
                    }
                }
            }
        }
    }
    
    private class AuthTokenFeatureInitializer implements FeatureInitializer {

        @Override
        public void register(FeatureRegistry registry) {
            registry.registerPublicInitializer(AuthTokenFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            synchronized (AuthTokenDispatcher.this) {
                try {
                    final AuthToken authToken = authenticator.getAuthToken(PresenceUtils.parseBareAddress(friendPresence.getPresenceId()));
                    FeatureTransport<AuthToken> transport = friendPresence.getTransport(AuthTokenFeature.class);
                    if (transport != null) {
                        transport.sendFeature(friendPresence, authToken);
                    } else {
                        LOG.debugf("no auth transport for: {0}", friendPresence);
                    }
                } catch (FriendException e) {
                    LOG.debugf(e, "couldn't send auth token to: {0}", friendPresence);
                }
                AuthToken authToken = pendingAuthTokens.remove(friendPresence.getPresenceId());
                if (authToken != null) {
                    LOG.debugf("updating auth token on presence {0} to {1}", friendPresence, authToken);
                    friendPresence.addFeature(new AuthTokenFeature(authToken));
                }
            }
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(AuthTokenFeature.ID);
        }
    }
}
