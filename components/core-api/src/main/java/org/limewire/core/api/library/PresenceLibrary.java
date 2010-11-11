package org.limewire.core.api.library;

import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.FriendPresence;

/**
 * A library specific to a presence of a friend. Multiple
 * {@link PresenceLibrary PresenceLibraries} are coalesced into a single
 * {@link FriendLibrary}.
 */
public interface PresenceLibrary extends RemoteLibrary {
    /** The {@link FriendPresence} associated with this library. */
    FriendPresence getPresence();

    /** Returns the current state of this presence library. */
    RemoteLibraryState getState();

    /** Sets the current state. */
    void setState(RemoteLibraryState newState);
    
    /** Returns the search result at <code>index</code> or throws {@link IndexOutOfBoundsException}. */
    SearchResult get(int index);
}
