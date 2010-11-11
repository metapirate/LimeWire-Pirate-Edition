package org.limewire.core.api.library;

import org.limewire.friend.api.Friend;

import ca.odell.glazedlists.EventList;

/**
 * A {@link RemoteLibrary} specifically for a friend. This is the coalesced
 * version of multiple {@link PresenceLibrary presence libraries}.
 */
public interface FriendLibrary extends RemoteLibrary {

    /** Returns the friend this library is for. */
    Friend getFriend();

    /**
     * @return the event list of contained presence libraries
     */
    EventList<PresenceLibrary> getPresenceLibraryList();
}
