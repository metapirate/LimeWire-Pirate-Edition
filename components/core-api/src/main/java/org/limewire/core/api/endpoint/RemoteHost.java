package org.limewire.core.api.endpoint;

import org.limewire.friend.api.FriendPresence;

/**
 * An interface that defines the key capabilities of a remote host.
 */
public interface RemoteHost {    
    boolean isBrowseHostEnabled();
    
    boolean isChatEnabled();
    
    boolean isSharingEnabled();
    
    public FriendPresence getFriendPresence();
}
