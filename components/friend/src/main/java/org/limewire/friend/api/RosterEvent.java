package org.limewire.friend.api;

import java.util.Collection;

import org.limewire.listener.DefaultDataTypeEvent;

public class RosterEvent extends DefaultDataTypeEvent<Collection<Friend>, RosterEvent.Type> {

    public static enum Type {
        FRIENDS_ADDED,
        FRIENDS_UPDATED,
        FRIENDS_DELETED
    }

    public RosterEvent(Collection<Friend> data, Type event) {
        super(data, event);
    }
}