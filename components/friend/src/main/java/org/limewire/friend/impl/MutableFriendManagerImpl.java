package org.limewire.friend.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.MutableFriendManager;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Default implementation of {@link FriendManager} and {@link MutableFriendManager}.
 * <p>
 * When friends are added and removed, it fires events in the respective broadcasters,
 * @Named("known") FriendEvent and @Named("available") FriendEvent.
 * <p>
 * Also listens to {@link FriendConnectionEvent} and removes all available and known
 * friends, if it's a disconnect event. This will be have to be revisited when we
 * start supporting concurrent friend connections to different networks.
 * <p>
 * The class is threadsafe.
 */
@EagerSingleton
class MutableFriendManagerImpl implements MutableFriendManager {

    private static final Log LOG = LogFactory.getLog(MutableFriendManagerImpl.class);
    
    private final EventBroadcaster<FriendEvent> knownBroadcaster;
    private final EventBroadcaster<FriendEvent> availableBroadcaster;
    private final ConcurrentMap<String, Friend> knownFriends = new ConcurrentHashMap<String, Friend>();
    private final ConcurrentMap<String, Friend> availFriends = new ConcurrentHashMap<String, Friend>();
    
    @Inject
    public MutableFriendManagerImpl(@Named("known") EventBroadcaster<FriendEvent> knownBroadcaster,
            @Named("available") EventBroadcaster<FriendEvent> availableBroadcaster,
            EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster) {
        this.knownBroadcaster = knownBroadcaster;
        this.availableBroadcaster = availableBroadcaster;
    }
    
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    for (Friend user :  availFriends.values()) {
                        removeAvailableFriend(user);
                    }
                    for (Friend user :  knownFriends.values()) {
                        removeKnownFriend(user, false);
                    }
                    break;
                }
            }
        });
    }
    
    @Override
    public void addKnownFriend(Friend friend) {
        if (knownFriends.putIfAbsent(friend.getId(), friend) == null) {
            LOG.debugf("adding known friend: {0}", friend);
            knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.ADDED));
        } else {
            LOG.debugf("not adding known friend: {0}", friend);
        }
    }
    
    @Override
    public void removeKnownFriend(Friend friend, boolean delete) {
        if (knownFriends.remove(friend.getId()) != null) {
            LOG.debugf("removed known friend: {0}", friend);
            if(delete) {
                knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.DELETE));
            }
            knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.REMOVED));
        } else {
            LOG.debugf("known friend {0} already removed", friend);
        }
    }
    
    @Override
    public FriendPresence getMostRelevantFriendPresence(String id) {
        Friend friend = availFriends.get(id);
        if(friend == null) {
            return null;
        } else {
            Collection<FriendPresence> presences = friend.getPresences().values();
            //TODO: this is not guarenteed to return the correct FriendPresence
            // if the user is logged in through two LWs with the same ID
            // Not really able to fix this without modifying the Browse/File Request
            for(FriendPresence nextPresence : presences) {
                if(nextPresence.hasFeatures(LimewireFeature.ID)) {
                    return nextPresence;
                }
            }
            return null;
        }
    }
    
    Map<String, Friend> getKnownFriends() {
        return Collections.unmodifiableMap(knownFriends);        
    }

    Map<String, Friend> getAvailableFriends() {
        return Collections.unmodifiableMap(availFriends);
    }
    
    Set<String> getAvailableFriendIds() {
        return Collections.unmodifiableSet(availFriends.keySet());
    }

    @Override
    public void addAvailableFriend(Friend friend) {
        if (availFriends.putIfAbsent(friend.getId(), friend) == null) {
            LOG.debugf("adding avail friend: {0}", friend);
            availableBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.ADDED));
        } else {
            LOG.debugf("not adding avail friend: {0}", friend);
        }
    }

    @Override
    public void removeAvailableFriend(Friend friend) {
        if (availFriends.remove(friend.getId()) != null) {
            LOG.debugf("removed avail friend: {0}", friend);
            availableBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.REMOVED));
        } else {
            LOG.debugf("avail friend {0} already removed", friend);
        }
    }

}
