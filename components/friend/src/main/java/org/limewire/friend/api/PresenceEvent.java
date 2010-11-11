package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

/**
 * This event is dispatched when a chat presence is added or updated.
 */
public class PresenceEvent extends DefaultDataTypeEvent<FriendPresence, PresenceEvent.Type> {

    public static enum Type {
        /**
         * Indicates that this is the first time we're seeing this presence.
         */
        PRESENCE_NEW,

        /**
         * Indicates that this is an update to the presence. For the exact kind
         * of update, see the {@link FriendPresence.Type}
         */
        PRESENCE_UPDATE
    }

    public PresenceEvent(FriendPresence data, Type event) {
        super(data, event);
    }
}
