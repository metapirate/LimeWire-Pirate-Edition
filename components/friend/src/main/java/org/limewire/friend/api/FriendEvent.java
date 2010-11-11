package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

public class FriendEvent extends DefaultDataTypeEvent<Friend, FriendEvent.Type> {
    
    public static enum Type {
        /** The Friend was added. */
        ADDED,
        /** The Friend was removed. */
        REMOVED,
        /** The Friend was deleted (and will never be added again). */
        DELETE;
    }

    public FriendEvent(Friend data, Type event) {
        super(data, event);
    }
    
}
