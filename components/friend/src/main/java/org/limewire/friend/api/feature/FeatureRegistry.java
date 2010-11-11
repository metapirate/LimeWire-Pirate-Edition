package org.limewire.friend.api.feature;

import java.net.URI;

/**
 * A registry of all of the locally supported features.
 *
 * For each <cod>Feature</code> there is a <code>FeatureInitializer</code> that
 * is invoked on a <code>FriendPresence</code> after it is discovered
 * that that presence supports the feature.<p>
 *
 * A <code>FeatureInitializer</code> might, for example, send the
 * local address to the friend presence.
 */
public interface FeatureRegistry {

    /**
     * Adds a <code>FeatureInitializer</code> to the registry for
     * a specific <code>URI</code> id. The URI is meant to be broacast and published
     * to other clients as a supported feature.
     * @param uri the id of the <code>Feature</code>
     * @param featureInitializer the entity to initialize the feature when needed.
     */
    void registerPublicInitializer(URI uri, FeatureInitializer featureInitializer);

    /**
     * Adds a <code>FeatureInitializer</code> to the registry for a 
     * specific URI. The URI is not meant to be broadcast to other clients.
     */
    void registerPrivateInitializer(URI uri, FeatureInitializer featureInitializer);
    /**
     * Deregisters a feature initializer for a uri.
     */
    void deregisterInitializer(URI uri);
    /**
     * Retrieve the {@link FeatureInitializer} based on the identifying URI.
     * @param uri identifies the feature being retrieved
     * @return the <code>FeatureInitializer</code the <code>uri</code>
     * or null if it does not exist
     */
    FeatureInitializer get(URI uri);
    
    /**
     * @return the URIs that are meant to be published to other clients
     */
    Iterable<URI> getPublicFeatureUris();

    /**
     * @return URIs for all features
     */
    Iterable<URI> getAllFeatureUris();
}
