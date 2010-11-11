package org.limewire.friend.impl.feature;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FeatureRegistryImpl implements FeatureRegistry {
    
    private final Object lock = new Object();
    
    private final Map<URI, FeatureInitializer> featureInitializers = new HashMap<URI, FeatureInitializer>();
    
    private final Set<URI> publicFeatureUris = new HashSet<URI>();
    
    @Inject
    FeatureRegistryImpl() {
    }

    @Override
    public FeatureInitializer get(URI uri) {
        synchronized (lock) {
            return featureInitializers.get(uri);
        }
    }

    @Override
    public Iterable<URI> getPublicFeatureUris() {
        synchronized (lock) {
            return new ArrayList<URI>(publicFeatureUris);
        }
    }

    @Override
    public Iterable<URI> getAllFeatureUris() {
        synchronized (lock) {
            return new ArrayList<URI>(featureInitializers.keySet());
        }        
    }

    @Override
    public void registerPrivateInitializer(URI uri, FeatureInitializer featureInitializer) {
        synchronized (lock) {
            featureInitializers.put(uri, featureInitializer);
        }
    }

    @Override
    public void registerPublicInitializer(URI uri, FeatureInitializer featureInitializer) {
        synchronized (lock) {
            featureInitializers.put(uri, featureInitializer);
            publicFeatureUris.add(uri);
        }
    }

    @Override
    public void deregisterInitializer(URI uri) {
        synchronized (lock) {
            featureInitializers.remove(uri);
            publicFeatureUris.remove(uri);
        }
    }
}
