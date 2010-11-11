package com.limegroup.gnutella.http;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class FeaturesWriter {
    
    private final NetworkManager networkManager;
    
    @Inject
    public FeaturesWriter(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Utlity method for getting the currently supported features.
     */
    public Set<HTTPHeaderValue> getFeaturesValue() {
        Set<HTTPHeaderValue> features = new HashSet<HTTPHeaderValue>(4);
        features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
//        if (ChatSettings.CHAT_ENABLED.getValue())
//            features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
        
       	features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
       	
       	if (!networkManager.acceptedIncomingConnection() && networkManager.canDoFWT())
       	    features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        
        return features;
    }

}
