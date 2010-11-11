package org.limewire.friend.impl.feature;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.LibraryChanged;
import org.limewire.friend.api.LibraryChangedEvent;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;

/**
 * Registers the {@link LibraryChangedNotifierFeature} with the {@link FeatureRegistry}.
 * <p>
 * Fires {@link LibraryChangedEvent} when it receives feature data from the wire.
 */
@EagerSingleton
class LibraryChangedDispatcher implements FeatureTransport.Handler<LibraryChangedNotifier> {
   
    private final EventBean<FriendConnectionEvent> friendConnectionEvent;
    private final EventBroadcaster<LibraryChangedEvent> libChangedBroadcaster;

    @Inject
    public LibraryChangedDispatcher(EventBean<FriendConnectionEvent> friendConnectionEvent,
                                 EventBroadcaster<LibraryChangedEvent> libChangedBroadcaster,
                                 FeatureRegistry featureRegistry) {
        this.friendConnectionEvent = friendConnectionEvent;
        this.libChangedBroadcaster = libChangedBroadcaster;
        new LibraryChangedNotifierFeatureInitializer().register(featureRegistry);
    }

    @Override
    public void featureReceived(String from, LibraryChangedNotifier feature) {
        FriendConnectionEvent event = friendConnectionEvent.getLastEvent();
        if(event != null) {
            FriendConnection connection = event.getSource();
            Friend friend = connection.getFriend(PresenceUtils.parseBareAddress(from));
            if(friend != null) {
                FriendPresence friendPresence = friend.getPresences().get(from);
                if(friendPresence != null) {
                    libChangedBroadcaster.broadcast(new LibraryChangedEvent(friendPresence, LibraryChanged.LIBRARY_CHANGED));     
                } 
            }
        }
        
    }
}
