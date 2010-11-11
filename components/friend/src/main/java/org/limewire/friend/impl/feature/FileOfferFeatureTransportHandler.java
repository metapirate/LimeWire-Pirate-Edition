package org.limewire.friend.impl.feature;

import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FileOffer;
import org.limewire.friend.api.FileOfferEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.FileOfferFeature;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;

@EagerSingleton
public class FileOfferFeatureTransportHandler implements FeatureTransport.Handler<FileMetaData>{

    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;

    @Inject
    public FileOfferFeatureTransportHandler(FeatureRegistry featureRegistry,
                                            EventBroadcaster<FileOfferEvent> fileOfferBroadcaster) {
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        new FileOfferInitializer().register(featureRegistry);
    }

    @Override
    public void featureReceived(String from, FileMetaData feature) {
        // TODO async?
        fileOfferBroadcaster.broadcast(new FileOfferEvent(new FileOffer(feature, from), FileOfferEvent.Type.OFFER));
        // TODO send acceptance or rejection;
        // TODO only needed for user feedback

    }

    private static class FileOfferInitializer implements FeatureInitializer {

        @Override
        public void register(FeatureRegistry registry) {
            registry.registerPublicInitializer(FileOfferFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new FileOfferFeature());
        }


        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(FileOfferFeature.ID);
        }

    }
}
