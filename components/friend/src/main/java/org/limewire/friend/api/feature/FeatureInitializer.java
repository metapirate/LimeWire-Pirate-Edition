package org.limewire.friend.api.feature;

import org.limewire.friend.api.FriendPresence;

import com.google.inject.Inject;

public interface FeatureInitializer {

    /**
     * Adds feature to the {@link FeatureRegistry}. The impl would
     * typically add the feature to the FeatureRegistry so the feature can
     * get looked up and initialized when the appropriate packets
     * are received over the network.
     *
     * @param registry FeatureRegistry to register with
     */
    @Inject
    void register(FeatureRegistry registry);

    /**
     * Initialize the feature. This
     * usually includes creating and adding the feature to
     * the {@link FriendPresence}.
     *
     * @param friendPresence FriendPresence
     */
    void initializeFeature(FriendPresence friendPresence);

    /**
     * Remove the feature. This usually means
     * removing the feature from the parameter FriendPresence
     *
     * @param friendPresence FriendPresence
     */
    void removeFeature(FriendPresence friendPresence);

}
