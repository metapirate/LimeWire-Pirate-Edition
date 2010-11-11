package org.limewire.friend.impl;

import java.util.Collection;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.MutableFriendManager;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.RosterEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

import com.google.inject.Inject;

@EagerSingleton
class FriendListListeners {
    
    private final PresenceListener presenceListener = new PresenceListener();
    private final MutableFriendManager friendManager;
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    
    @Inject
    FriendListListeners(MutableFriendManager friendManager, 
                   EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster) {
        this.friendManager = friendManager;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster; 
    }
    
    @Inject void register(ListenerSupport<RosterEvent> rosterListeners) {
        rosterListeners.addListener(new EventListener<RosterEvent>() {
            @Override
            public void handleEvent(RosterEvent event) {
                Collection<Friend> friends = event.getData();

                switch(event.getType()) {
                case FRIENDS_ADDED:
                    addKnownFriends(friends);
                    break;
                case FRIENDS_UPDATED:
                    updateFriends(friends);
                    break;
                case FRIENDS_DELETED:
                    removeKnownFriends(friends);
                    break;
                }
            }
        });
    }

    
    private void addKnownFriends(Collection<Friend> friends) {
        for (Friend friend : friends) {
            addKnownFriend(friend);
        }
    }

    private void addKnownFriend(Friend friend) {
            if (friend.isSubscribed()) {
            friend.addPresenceListener(presenceListener);
            friendManager.addKnownFriend(friend);
        }
    }

    private void updateFriends(Collection<Friend> friends) {
        for (Friend friend : friends) {
            if (friend.isSubscribed()) {
                addKnownFriend(friend);
            } else {
                friendManager.removeKnownFriend(friend, true);
    }
        }
    }
    
    private void removeKnownFriends(Collection<Friend> friends) {
        for (Friend friend : friends) {
            friendManager.removeKnownFriend(friend, true);
        }
    }
    
    private void updatePresence(FriendPresence presence) {
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.UPDATE));
    }
    
    private void addPresence(FriendPresence presence) {
        Friend friend = presence.getFriend();
        if(friend.getPresences().size() == 1) {
            friendManager.addAvailableFriend(friend);
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
    }
    
    private void removePresence(FriendPresence presence) {
        Friend friend = presence.getFriend();
        if(!friend.isSignedIn()) {
            friendManager.removeAvailableFriend(friend);
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
    }


    private class PresenceListener implements EventListener<PresenceEvent> {
        @Override
        public void handleEvent(PresenceEvent event) {
            switch (event.getData().getType()) {
                case available:
                    switch (event.getType()) {
                        case PRESENCE_NEW:
                            addPresence(event.getData());
                            break;
                        case PRESENCE_UPDATE:
                            updatePresence(event.getData());
                            break;
                    }
                    break;
                case unavailable:
                    removePresence(event.getData());
                    break;
            }
        }
    }
}
