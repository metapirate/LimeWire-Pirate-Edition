package org.limewire.friend.impl.feature;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBroadcaster;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.ConnectBackRequestedEvent;

import com.google.inject.Inject;

@EagerSingleton
public class ConnectBackRequestFeatureTransportHandler implements FeatureTransport.Handler<ConnectBackRequest>{
    private final EventBroadcaster<ConnectBackRequestedEvent> connectBackRequestedEventBroadcaster;

    @Inject
    public ConnectBackRequestFeatureTransportHandler(FeatureRegistry featureRegistry,
                                                     EventBroadcaster<ConnectBackRequestedEvent> connectBackRequestedEventBroadcaster) {
        this.connectBackRequestedEventBroadcaster = connectBackRequestedEventBroadcaster;
        new ConnectBackRequestIQFeatureInitializer().register(featureRegistry);
    }

    @Override
    public void featureReceived(String from, ConnectBackRequest connectBackRequest) {
        connectBackRequestedEventBroadcaster.broadcast(new ConnectBackRequestedEvent(connectBackRequest));
    }

    private static class ConnectBackRequestIQFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.registerPublicInitializer(ConnectBackRequestFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new ConnectBackRequestFeature());
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(ConnectBackRequestFeature.ID);
        }
    }
}
