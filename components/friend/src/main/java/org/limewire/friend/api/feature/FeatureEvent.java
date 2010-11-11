package org.limewire.friend.api.feature;

import org.limewire.friend.api.FriendPresence;
import org.limewire.listener.DefaultDataSourceTypeEvent;

public class FeatureEvent extends DefaultDataSourceTypeEvent<FriendPresence, FeatureEvent.Type, Feature> {

    public static enum Type {
        ADDED, 
        REMOVED 
    }
    
    public FeatureEvent(FriendPresence source, Type type, Feature feature) {
        super(source, type, feature);
    }
}


