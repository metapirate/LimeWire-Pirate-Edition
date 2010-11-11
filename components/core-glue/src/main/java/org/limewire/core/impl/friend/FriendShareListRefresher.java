package org.limewire.core.impl.friend;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.collection.Periodic;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Clock;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryStatusEvent;

/**
 * Sends library changed messages to friends when:<BR>
 * 1) File manager is finished loading<BR>
 * OR<BR>
 * 2) A friend's sharelist changes
 */

@EagerSingleton
class FriendShareListRefresher {
    
    private static final Log LOG = LogFactory.getLog(FriendShareListRefresher.class);

    private final Clock clock;
    private final BrowseTracker tracker;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<String, Friend> friendMap;

    /** A map of change senders per each id. */
    private final ConcurrentMap<String, LibraryChangedSender> changeSenders;
    
    // Package private for testing
    final AtomicBoolean fileManagerLoaded = new AtomicBoolean(false);

    @Inject
    FriendShareListRefresher(Clock clock, BrowseTracker tracker,
                             @Named("backgroundExecutor")ScheduledExecutorService scheduledExecutorService,
                             @Named("available") Map<String, Friend> friendMap) {
        this.clock = clock;
        this.tracker = tracker;
        this.scheduledExecutorService = scheduledExecutorService;
        this.changeSenders = new ConcurrentHashMap<String, LibraryChangedSender>();
        this.friendMap = friendMap;
    }

    @Inject void register(Library library) {
        library.addManagedListStatusListener(new FinishedLoadingListener());
    }

    @Inject void register(final FileViewManager fileViewManager, @Named("available") ListenerSupport<FriendEvent> friendSupport) {
        fileViewManager.addListener(new EventListener<FileViewChangeEvent>() {
            public void handleEvent(FileViewChangeEvent event) {
                switch(event.getType()) {
                case FILE_ADDED:
                case FILE_REMOVED:
                case FILE_META_CHANGED:
                case FILE_CHANGED:
                case FILES_CLEARED:
                    triggerChangeSender(event.getSource().getName());
                    break;
                }
            }
        });
        
        friendSupport.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {
                LOG.debugf("Received friend event {0}", event);
                switch(event.getType()) {
                case DELETE:
                case REMOVED:
                    removeChangeSender(event.getData().getId());
                    break;
                case ADDED:
                    fileViewManager.getFileViewForId(event.getData().getId());
                    break;
                }
            }
        });
    }
    
    private void triggerChangeSender(String id) {
        LOG.debugf("Change triggered for id {0}", id);
        if(fileManagerLoaded.get()) {
            Friend friend = friendMap.get(id);
            if(friend != null) {
                LOG.debugf("Triggering library change for friend {0}", friend);
                LibraryChangedSender sender = changeSenders.get(id);
                if(sender == null) {
                    LOG.debugf("No existing sender for friend {0}, creating a new one", friend);
                    LibraryChangedSender newSender = new LibraryChangedSender(friend);
                    sender = changeSenders.putIfAbsent(id, newSender);
                    if(sender == null) {
                        sender = newSender;
                    }
                }
                sender.scheduleSendRefreshCheck();
            } else {
                LOG.debugf("Not triggering library change for id {0} because no friend was available", id);
            }
        }
    }
    
    private void removeChangeSender(String id) {
        LibraryChangedSender sender = changeSenders.remove(id);
        if(sender != null) {
            LOG.debugf("Removing change trigger for id {0}", id);
            sender.cancel();
        }
    }
    
    /**
     * Sends refresh notifications to the presences that have the {@link LibraryChangedNotifierFeature}.
     */
    void sendRefreshNotificationsToPresences(Iterable<FriendPresence> presences) {
        for(FriendPresence presence : presences) {
            @SuppressWarnings("unchecked")
            Feature<LibraryChangedNotifier> feature = presence.getFeature(LibraryChangedNotifierFeature.ID);
            if (feature != null) {
                FeatureTransport<LibraryChangedNotifier> transport = presence.getTransport(LibraryChangedNotifierFeature.class);
                try {
                    transport.sendFeature(presence, feature.getFeature());
                } catch (FriendException e) {
                    LOG.error("library changed notification failed", e);
                }
            } else {
                LOG.debugf("no library refresh for presence: {0}", presence);
            }
        }   
    }
    
    private class FinishedLoadingListener implements EventListener<LibraryStatusEvent> {
        @BlockingEvent
        public void handleEvent(LibraryStatusEvent evt) {
            switch(evt.getType()) {
            case LOAD_COMPLETE:
                fileManagerLoaded.set(true);
                for(Friend friend : friendMap.values()) {
                    tracker.sentRefresh(friend.getId());
                    sendRefreshNotificationsToPresences(friend.getPresences().values());
                }
                break;
            }
        }            
    }
    
    private class LibraryChangedSender {        
        private final Friend friend;        
        private final Periodic libraryRefreshPeriodic;
        
        LibraryChangedSender(Friend friend){
            this.friend = friend;
            this.libraryRefreshPeriodic = new Periodic(new ScheduledLibraryRefreshSender(), scheduledExecutorService, clock);
        }
        
        /**
         * Schedules an immediate check if a library refresh should be sent
         * to the friend.
         */
        void scheduleSendRefreshCheck() {
            libraryRefreshPeriodic.rescheduleIfLater(5000);
        }
        
        void cancel() {
            libraryRefreshPeriodic.unschedule();
        }
    
        private class ScheduledLibraryRefreshSender implements Runnable {    
            @Override
            public void run() {
                BrowseTracker browseTracker = FriendShareListRefresher.this.tracker;
                Date lastBrowseTime = browseTracker.lastBrowseTime(friend.getId());
                Date lastRefreshTime = browseTracker.lastRefreshTime(friend.getId());
                LOG.debugf("Running library periodic for friend {0}, lastBrowseTime {1}, lastRefreshTime {2}", friend, lastBrowseTime, lastRefreshTime);
                if(lastBrowseTime != null && (lastRefreshTime == null || lastBrowseTime.after(lastRefreshTime))) {
                    browseTracker.sentRefresh(friend.getId());
                    sendRefreshNotificationsToPresences(friend.getPresences().values());
                }
            }
        }
    }
}
