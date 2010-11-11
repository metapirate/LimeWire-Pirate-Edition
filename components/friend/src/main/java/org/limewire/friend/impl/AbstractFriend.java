package org.limewire.friend.impl;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

public abstract class AbstractFriend implements Friend {
    
    private final EventListenerList<PresenceEvent> presenceListeners = new EventListenerList<PresenceEvent>();

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        presenceListeners.addListener(presenceListener);
        for(FriendPresence presence : getPresences().values()) {
            presenceListener.handleEvent(new PresenceEvent(presence, PresenceEvent.Type.PRESENCE_NEW));
        }
    }
    
    protected void firePresenceEvent(PresenceEvent event) {
        presenceListeners.broadcast(event);
    }

}
