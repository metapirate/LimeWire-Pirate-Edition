package org.limewire.core.api.browse;

import org.limewire.friend.api.FriendPresence;

/**
 * Factory for creating a {@link Browse} object from a {@link FriendPresence}.
 */
public interface BrowseFactory {
    /**
     * Creates a Browse object from the given FriendPresence.
     */
    Browse createBrowse(FriendPresence friendPresence);
}
