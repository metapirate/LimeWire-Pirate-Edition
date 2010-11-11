package org.limewire.friend.api;

import org.limewire.listener.DefaultDataTypeEvent;

public class LibraryChangedEvent extends DefaultDataTypeEvent<FriendPresence, LibraryChanged> {

    public LibraryChangedEvent(FriendPresence data, LibraryChanged event) {
        super(data, event);
    }
}
