package org.limewire.friend.impl.feature;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
public class LibraryChangedNotifierFeatureInitializer implements FeatureInitializer {

    @Override
    public void register(FeatureRegistry registry) {
        registry.registerPublicInitializer(LibraryChangedNotifierFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence friendPresence) {
        friendPresence.addFeature(new LibraryChangedNotifierFeature(new LibraryChangedNotifier(){}));
    }

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(LibraryChangedNotifierFeature.ID);
    }
}

