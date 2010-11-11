package org.limewire.friend.impl.feature;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.LimewireFeature;

public class LimewireFeatureInitializer implements FeatureInitializer{
    @Override
    public void register(FeatureRegistry registry) {
        registry.registerPublicInitializer(LimewireFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence friendPresence) {
        friendPresence.addFeature(new LimewireFeature());
    }

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(LimewireFeature.ID);
    }
}
