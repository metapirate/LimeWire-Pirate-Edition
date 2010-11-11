package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

public class FriendPresenceEvent extends DefaultDataTypeEvent<FriendPresence, FriendPresenceEvent.Type> {
    
    public static enum Type {
        /** This is a new presence. */
        ADDED, 
        /** This presence is no longer available. */
        REMOVED,
        /** This presence has new information. */
        UPDATE;
    }

    public FriendPresenceEvent(FriendPresence data, Type event) {
        super(data, event);
    }
    
}
