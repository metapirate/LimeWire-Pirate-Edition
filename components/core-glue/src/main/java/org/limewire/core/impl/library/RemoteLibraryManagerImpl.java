package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.limewire.collection.MultiIterable;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.RemoteLibraryState;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.impl.ReadOnlyList;
import ca.odell.glazedlists.util.concurrent.LockFactory;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class keeps track of all friends libraries. As friend presences are found they are 
 * aggregated into a single friend library per friend. All RemoteFileItems found in the friend libraries
 * are also coalesced into a single FileList.
 */
@Singleton
public class RemoteLibraryManagerImpl implements RemoteLibraryManager {
    
    private static final Log LOG = LogFactory.getLog(RemoteLibraryManagerImpl.class, "friend-library");
    
    private final EventList<FriendLibrary> allFriendLibraries;
    private final EventListenerList<RemoteLibraryEvent> listeners = new EventListenerList<RemoteLibraryEvent>();
    private final AllFriendsLibraryImpl allFriendsLibrary = new AllFriendsLibraryImpl();
    private final PropagatingEventListener propagatingEventListener = new PropagatingEventListener(allFriendsLibrary, listeners);
    /**
     * Common lock for all glazed lists in here.
     */
    private final ReadWriteLock listLock = LockFactory.DEFAULT.createReadWriteLock();

    @Inject
    public RemoteLibraryManagerImpl() {
        allFriendLibraries = GlazedListsFactory.threadSafeList(new BasicEventList<FriendLibrary>(listLock));
    }
    
    @Override
    public RemoteLibrary getAllFriendsLibrary() {
        return allFriendsLibrary;
    }
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return allFriendLibraries;
    }
    
    @Override
    public boolean addPresenceLibrary(FriendPresence presence) {
        assert !presence.getFriend().isAnonymous();
        listLock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getOrCreateFriendLibrary(presence.getFriend());
            return friendLibrary.addPresenceLibrary(presence);
        } finally {
            listLock.writeLock().unlock();
        }
    }
    
    @Override
    public PresenceLibrary getPresenceLibrary(FriendPresence presence) {
        listLock.readLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibrary(presence.getFriend());
            if(friendLibrary != null) {
                return friendLibrary.getPresenceLibrary(presence);
            } else {
                return null;
            }
        } finally {
            listLock.readLock().unlock();
        }
    }
    
    private void removeFriendLibrary(FriendLibraryImpl friendLibrary) {
        LOG.debugf("removing friend library for {0}", friendLibrary.getFriend());
        listLock.writeLock().lock();
        try {
            friendLibrary.removeListener(propagatingEventListener);
            allFriendLibraries.remove(friendLibrary);
        } finally {
            listLock.writeLock().unlock();
        }
    }

    @Override
    public void removePresenceLibrary(FriendPresence presence) {
        listLock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibrary(presence.getFriend());
            if (friendLibrary != null) {
                friendLibrary.removePresenceLibrary(presence); 
                if (friendLibrary.getPresenceLibraryList().isEmpty()) {
                    removeFriendLibrary(friendLibrary);
                }
            }
        } finally {
            listLock.writeLock().unlock();
        }
    }

    private FriendLibraryImpl getOrCreateFriendLibrary(Friend friend) {
        listLock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibrary(friend);
            if(friendLibrary == null) {
                LOG.debugf("adding friend library for {0}", friend);
                friendLibrary = new FriendLibraryImpl(friend);
                friendLibrary.addListener(propagatingEventListener);
                allFriendLibraries.add(friendLibrary);
            }
            return friendLibrary;
        } finally {
            listLock.writeLock().unlock();
        }
    }

    @Override
    public boolean hasFriendLibrary(Friend friend) {
        return getFriendLibrary(friend) != null;
    }
    
    @Override
    public FriendLibraryImpl getFriendLibrary(Friend friend) {
        listLock.readLock().lock();
        try { 
            for(FriendLibrary library : allFriendLibraries) {
                if(library.getFriend().getId().equals(friend.getId())) {
                    return (FriendLibraryImpl)library;
                }
            }
            return null;
        } finally {
            listLock.readLock().unlock();
        }
    }

    
    private class AllFriendsLibraryImpl implements ParentRemoteLibrary {
        
        private volatile RemoteLibraryState state = RemoteLibraryState.LOADING;
        
        @Override
        public int size() {
            listLock.readLock().lock();
            try {
                int sum = 0;
                for (RemoteLibrary remoteLibrary : allFriendLibraries) {
                    sum += remoteLibrary.size();
                }
                return sum;
            } finally {
                listLock.readLock().unlock();
            }
        }
        
        public void addNewResult(SearchResult file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNewResults(Collection<SearchResult> files) {
            throw new UnsupportedOperationException();   
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RemoteLibraryState getState() {
            return state;
        }
        
        public void setState(RemoteLibraryState state) {
            this.state = state;
            listeners.broadcast(RemoteLibraryEvent.createStateChangedEvent(this));
        }

        @Override
        public Iterator<SearchResult> iterator() {
            return new MultiIterable<SearchResult>(allFriendLibraries.toArray(new RemoteLibrary[0])).iterator();
        }

        @Override
        public void addListener(EventListener<RemoteLibraryEvent> listener) {
            listeners.addListener(listener);
        }

        @Override
        public boolean removeListener(EventListener<RemoteLibraryEvent> listener) {
            return listeners.removeListener(listener);
        }

        @Override
        public void updateState() {
            setState(calculateState(allFriendLibraries));
        }
    }

    private class FriendLibraryImpl implements FriendLibrary, ParentRemoteLibrary {

        private final Friend friend;
        private final EventList<PresenceLibrary> allPresenceLibraries;
        private final ReadOnlyList<PresenceLibrary> readOnlyPresenceLibraries;
        private volatile RemoteLibraryState state = RemoteLibraryState.LOADING;
        private final EventListenerList<RemoteLibraryEvent> listeners = new EventListenerList<RemoteLibraryEvent>();
        // note: this call is exposing this in constructor, but is locally confined
        private final PropagatingEventListener propagatingEventListener = new PropagatingEventListener(this, listeners);
        
        public FriendLibraryImpl(Friend friend) {
            this.friend = friend;
            allPresenceLibraries = GlazedListsFactory.threadSafeList(new BasicEventList<PresenceLibrary>(listLock));
            readOnlyPresenceLibraries = GlazedListsFactory.readOnlyList(allPresenceLibraries);
        }
        
        @Override
        public EventList<PresenceLibrary> getPresenceLibraryList() {
            return readOnlyPresenceLibraries;
        }
        
        @Override
        public RemoteLibraryState getState() {
            return state;
        }

        private void setState(RemoteLibraryState state) {
            this.state = state;
            listeners.broadcast(RemoteLibraryEvent.createStateChangedEvent(this));
        }
        
        public void updateState() {
            setState(calculateState(allPresenceLibraries));
        }
        
        public Friend getFriend() {
            return friend;
        }
        
        private PresenceLibraryImpl getPresenceLibrary(FriendPresence presence) {
            listLock.readLock().lock();
            try {
                for(PresenceLibrary library : allPresenceLibraries) {
                    if(library.getPresence().getPresenceId().equals(presence.getPresenceId())) {
                        return (PresenceLibraryImpl)library;
                    }
                }
                return null;
            } finally {
                listLock.readLock().unlock();
            }
        }
        
        private boolean addPresenceLibrary(FriendPresence presence) {
            listLock.writeLock().lock();
            try {
                PresenceLibraryImpl library = getPresenceLibrary(presence);
                if(library == null) {
                    LOG.debugf("adding presence library for {0}", presence);
                    library = new PresenceLibraryImpl(presence);
                    allPresenceLibraries.add(library);
                    library.addListener(propagatingEventListener);
                    return true;
                } else {
                    return false;
                }
            } finally {
               listLock.writeLock().unlock();
            }
        }

        private void removePresenceLibrary(FriendPresence presence) {
           listLock.writeLock().lock();
            try {
                PresenceLibraryImpl presenceLibrary = getPresenceLibrary(presence);
                if(presenceLibrary != null) {
                    LOG.debugf("removing presence library for {0}", presence);
                    presenceLibrary.removeListener(propagatingEventListener);
                    allPresenceLibraries.remove(presenceLibrary);
                }
            } finally {
                listLock.writeLock().unlock();
            }
        }

        @Override
        public int size() {
            listLock.readLock().lock();
            try {
                int sum = 0;
                for (PresenceLibrary presenceLibrary : readOnlyPresenceLibraries) {
                    sum += presenceLibrary.size();
                }
                return sum;
            } finally {
                listLock.readLock().unlock();
            }
        }

        @Override
        public void addNewResult(SearchResult file) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void setNewResults(Collection<SearchResult> file) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return StringUtils.toString(this);
        }

        @Override
        public Iterator<SearchResult> iterator() {
            return new MultiIterable<SearchResult>(readOnlyPresenceLibraries.toArray(new RemoteLibrary[0])).iterator();
        }

        @Override
        public void addListener(EventListener<RemoteLibraryEvent> listener) {
            listeners.addListener(listener);
        }

        @Override
        public boolean removeListener(EventListener<RemoteLibraryEvent> listener) {
            return listeners.removeListener(listener);
        }
    }

    static class PresenceLibraryImpl implements PresenceLibrary {

        private final FriendPresence presence;
        private volatile RemoteLibraryState state = RemoteLibraryState.LOADING;
        private final List<SearchResult> results = Collections.synchronizedList(new ArrayList<SearchResult>());
        private final EventListenerList<RemoteLibraryEvent> listeners = new EventListenerList<RemoteLibraryEvent>();
        private final List<AddOnlyListIterator> iterators = new ArrayList<AddOnlyListIterator>(2);
        
        PresenceLibraryImpl(FriendPresence presence) {
            this.presence = presence;
        }

        @Override
        public String toString() {
            return StringUtils.toString(this, presence);
        }
        
        public FriendPresence getPresence() {
            return presence;
        }

        @Override
        public void addNewResult(SearchResult file) {
            int startIndex;
            synchronized (results) {
                startIndex = results.size();
                results.add(file);
            }
            listeners.broadcast(RemoteLibraryEvent.createResultsAddedEvent(this, Collections.singleton(file), startIndex));
        }

        @Override
        public void setNewResults(Collection<SearchResult> files) {
            clear();
            int startIndex;
            synchronized (results) {
                startIndex = results.size();
                results.addAll(files);
            }
            listeners.broadcast(RemoteLibraryEvent.createResultsAddedEvent(this, files, startIndex));
        }
        
        @Override
        public void clear() {
            synchronized (results) {
                results.clear();
                for (AddOnlyListIterator iterator : iterators) {
                    iterator.cleared = true;
                }
                iterators.clear();
            }
            listeners.broadcast(RemoteLibraryEvent.createResultsClearedEvent(this));
        }

        @Override
        public int size() {
            return results.size();
        }

        @Override
        public RemoteLibraryState getState() {
            return state;
        }
        
        @Override
        public void setState(RemoteLibraryState newState) {
            this.state = newState;
            listeners.broadcast(RemoteLibraryEvent.createStateChangedEvent(this));
        }

        @Override
        public Iterator<SearchResult> iterator() {
            synchronized (results) {
                AddOnlyListIterator iterator = new AddOnlyListIterator();
                iterators.add(iterator);
                return iterator;
            }
        }

        private class AddOnlyListIterator implements Iterator<SearchResult> {

            private int currentIndex = 0;
            private boolean cleared = false;
            private SearchResult next = null;
            
            public AddOnlyListIterator() {
                setNext();
            }
            
            private void setNext() {
                synchronized (results) {
                    next = currentIndex < results.size() ? results.get(currentIndex) : null;
                    ++currentIndex;
                }
            }
            
            @Override
            public boolean hasNext() {
                if (cleared) {
                    return false;
                }
                return next != null;
            }

            @Override
            public SearchResult next() {
                SearchResult result = next;
                setNext();
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        }

        @Override
        public void addListener(EventListener<RemoteLibraryEvent> listener) {
            listeners.addListener(listener);
        }

        @Override
        public boolean removeListener(EventListener<RemoteLibraryEvent> listener) {
            return listeners.removeListener(listener);
        }

        @Override
        public SearchResult get(int index) {
            return results.get(index);
        }
    }
    
    
    static RemoteLibraryState calculateState(EventList<? extends RemoteLibrary> children) {
        children.getReadWriteLock().readLock().lock();
        try {
            boolean oneCompleted = false;
            for(RemoteLibrary library : children) {
                switch (library.getState()) {
                case LOADING:
                    return RemoteLibraryState.LOADING;
                case LOADED:
                    oneCompleted = true;
                    break;
                }
            }
            if(oneCompleted) {
                return RemoteLibraryState.LOADED;
            } else {
                return RemoteLibraryState.FAILED_TO_LOAD;
            }
        } finally {
            children.getReadWriteLock().readLock().unlock();
        }
    }
    
    /**
     * Helper interface used by {@link PropagatingEventListener} and 
     * implemented by remote libraries that contain other remote libraries.
     */
    static interface ParentRemoteLibrary extends RemoteLibrary {
        /**
         * Called when the state of one of the contained remote libraries
         * changed to allow the parent to recalculate its compound state.
         */
        void updateState();
    }
    
    static class PropagatingEventListener implements EventListener<RemoteLibraryEvent> {
    
        private final ParentRemoteLibrary parent;
        private final EventListenerList<RemoteLibraryEvent> listeners;
            
        public PropagatingEventListener(ParentRemoteLibrary parent, EventListenerList<RemoteLibraryEvent> listeners) {
            this.parent = parent;
            this.listeners = listeners;
        }
        
        @Override
        public void handleEvent(RemoteLibraryEvent event) {
            switch (event.getType()) {
            case STATE_CHANGED:
                parent.updateState();
                break;
            case RESULTS_ADDED:
                listeners.broadcast(RemoteLibraryEvent.createResultsAddedEvent(parent, event.getAddedResults(),
                        // it's difficult to know the exact offset in the parent library, so let's not expose it
                        -1));
                break;
            case RESULTS_CLEARED:
                if (parent.size() == 0) {
                    listeners.broadcast(RemoteLibraryEvent.createResultsClearedEvent(parent));
                } else {
                    listeners.broadcast(RemoteLibraryEvent.createResultsRemovedEvent(parent));
                }
                break;
            case RESULTS_REMOVED:
                listeners.broadcast(RemoteLibraryEvent.createResultsRemovedEvent(parent));
                break;
            }
        }
    }
}
