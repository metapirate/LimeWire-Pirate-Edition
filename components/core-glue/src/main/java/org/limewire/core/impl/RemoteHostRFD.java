package org.limewire.core.impl;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A RemoteFileDesc implementation for RemoteHosts. 
 */
public class RemoteHostRFD implements RemoteHost {
    
    private RemoteFileDesc remoteFileDesc;

    private FriendPresence friendPresence;
    
    public RemoteHostRFD(RemoteFileDesc remoteFileDesc, FriendPresence friendPresence) {
        this.remoteFileDesc = remoteFileDesc;
        this.friendPresence = friendPresence;
    }
    
    @Override
    public FriendPresence getFriendPresence() {
        return friendPresence;
    }

    @Override
    public boolean isBrowseHostEnabled() {
        if(friendPresence.getFriend().isAnonymous()) {
            return remoteFileDesc.isBrowseHostEnabled();
        } else {
            //ensure friend/user still logged in through LW
            return friendPresence.hasFeatures(LimewireFeature.ID);
        }
    }

    @Override
    public boolean isChatEnabled() {
        if(friendPresence.getFriend().isAnonymous()) {
            return false;
        }else { //TODO: this isn't entirely correct. Friend could have signed
            // ouf of LW but still be logged in through other service allowing chat
            return friendPresence.hasFeatures(LimewireFeature.ID);
        }
    }

    @Override
    public boolean isSharingEnabled() {
        if(friendPresence.getFriend().isAnonymous()) {
            return false;
        } else {
            //ensure friend/user still logged in through LW
            return friendPresence.hasFeatures(LimewireFeature.ID);
        }
    }

}
