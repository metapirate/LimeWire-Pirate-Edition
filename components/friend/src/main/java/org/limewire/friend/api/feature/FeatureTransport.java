package org.limewire.friend.api.feature;

import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;

/**
 * Defines a way to send the data of a feature to a friend presence.
 */
public interface FeatureTransport<T> {
    /**
     * Sends the feature data to <code>presence<code> in an asynchronous fashion.
     */
    void sendFeature(FriendPresence presence, T featureData) throws FriendException;
    
    /**
     * A handler of received feature data.
     * <p>
     * The specific protocol layers notify handler of received feature data
     */
    interface Handler<T> {
        void featureReceived(String from, T featureData);
    }
}
