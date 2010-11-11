package org.limewire.friend.impl;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListenerList;

/**
 * Abstract implementation of {@link FriendPresence} providing
 * management of features and feature transports.
 */
public abstract class AbstractFriendPresence implements FriendPresence {
    
    private final Map<URI, Feature> features;
    private final Map<Class<? extends Feature<?>>, FeatureTransport> featureTransports;
    private final EventBroadcaster<FeatureEvent> featureBroadcaster;
    
    public AbstractFriendPresence() {
        this(new EventListenerList<FeatureEvent>());
    }

    public AbstractFriendPresence(EventBroadcaster<FeatureEvent> featureEventBroadcaster){
        this.features = new ConcurrentHashMap<URI, Feature>(5, 0.75f, 1);
        this.featureTransports = new ConcurrentHashMap<Class<? extends Feature<?>>, FeatureTransport>(5, 0.75f, 1);
        this.featureBroadcaster = featureEventBroadcaster;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return features.values();
    }

    @Override
    public Feature getFeature(URI id) {
        return features.get(id);
    }

    @Override
    public boolean hasFeatures(URI... id) {
        for(URI uri : id) {
            if(getFeature(uri) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
        featureBroadcaster.broadcast(new FeatureEvent(this, FeatureEvent.Type.ADDED, feature));
    }

    @Override
    public void removeFeature(URI id) {
        Feature feature = features.remove(id);
        if(feature != null) {
            featureBroadcaster.broadcast(new FeatureEvent(this, FeatureEvent.Type.REMOVED, feature));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Feature<U>, U> FeatureTransport<U> getTransport(Class<T> feature) {
        return featureTransports.get(feature);
    }

    @Override
    public <D, F extends Feature<D>> void addTransport(Class<F> clazz, FeatureTransport<D> transport) {
        featureTransports.put(clazz, transport);
    }
}
