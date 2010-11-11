package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

public class FriendRequestEvent extends DefaultDataTypeEvent<FriendRequest, FriendRequestEvent.Type> {

    public enum Type {REQUESTED}

    public FriendRequestEvent(FriendRequest data, Type event) {
        super(data, event);
    }
}
