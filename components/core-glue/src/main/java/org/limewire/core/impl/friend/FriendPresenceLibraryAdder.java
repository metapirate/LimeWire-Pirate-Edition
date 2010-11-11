package org.limewire.core.impl.friend;

import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;

/**
 * Listens for new presences and for them to have features so they become
 * browsable and can be added to {@link RemoteLibraryManager}.
 */
@EagerSingleton
class FriendPresenceLibraryAdder {
    
    private static final Log LOG = LogFactory.getLog(FriendPresenceLibraryAdder.class);

    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    public FriendPresenceLibraryAdder(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Inject void register(ListenerSupport<FeatureEvent> featureSupport) {
        featureSupport.addListener(new FeatureListener());
    }

    class FeatureListener implements EventListener<FeatureEvent> {
        @Override
        @BlockingEvent
        public void handleEvent(FeatureEvent featureEvent) {
            FriendPresence presence = featureEvent.getSource();
            if (featureEvent.getType() == FeatureEvent.Type.ADDED) {
                if (presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                    LOG.debugf("adding presence library for: {0}", presence);
                    remoteLibraryManager.addPresenceLibrary(presence);
                }
            } else if (featureEvent.getType() == FeatureEvent.Type.REMOVED) {
                if (!presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                    LOG.debugf("removing presence library for: {0}", presence);
                    remoteLibraryManager.removePresenceLibrary(presence);
                }
            }
        }
    }
    
}