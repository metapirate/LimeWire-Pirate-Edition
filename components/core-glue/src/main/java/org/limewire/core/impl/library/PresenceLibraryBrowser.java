package org.limewire.core.impl.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.RemoteLibraryState;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.friend.FriendRemoteFileDescDeserializer;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.LibraryChangedEvent;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

@EagerSingleton
class PresenceLibraryBrowser implements EventListener<LibraryChangedEvent> {
   
    private static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final BrowseFactory browseFactory;
    private final RemoteLibraryManager remoteLibraryManager;
    private final SocketsManager socketsManager;
    
    /**
     * Keeps track of libraries that could not be browsed yet, because the local peer didn't have
     * enough connection capabilities.
     */
    final Set<PresenceLibrary> librariesToBrowse = Collections.synchronizedSet(new HashSet<PresenceLibrary>());

    /**
     * Is incremented when a new connectivity change event is received, should
     * only be modified holding the lock to {@link #librariesToBrowse}.
     * 
     * When address resolution fails, the revision that was used when resolution is started
     * can be compared to the latest revision to see if the client's connectivity
     * has changed in the meantime and resolution should be retried.
     * 
     * Still volatile, so it can be read without a lock.
     */
    @GuardedBy("librariesToBrowse")
    private volatile int latestConnectivityEventRevision = 0;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory, RemoteLibraryManager remoteLibraryManager,
            SocketsManager socketsManager, FriendRemoteFileDescDeserializer remoteFileDescDeserializer) {
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
        this.socketsManager = socketsManager;
    }

    @Inject 
    void register(ListenerSupport<LibraryChangedEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }

    @Inject
    void registerToSocksManager() {
        socketsManager.addListener(new ConnectivityChangeListener());
    }
    
    @Inject
    void registerToRemoteLibraryManager() {    
        remoteLibraryManager.getFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>() {
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    if(listChanges.getType() == ListEvent.INSERT) {
                        final FriendLibrary friendLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                        
                        new AbstractListEventListener<PresenceLibrary>() {
                            @Override
                            protected void itemAdded(PresenceLibrary presenceLibrary, int idx, EventList<PresenceLibrary> source) {
                                tryToResolveAndBrowse(presenceLibrary, latestConnectivityEventRevision);
                            }

                            @Override
                            protected void itemRemoved(PresenceLibrary item, int idx, EventList<PresenceLibrary> source) {
                                // TODO: This should cancel the browse, if it was in action.
                                librariesToBrowse.remove(item);
                            }

                            @Override
                            protected void itemUpdated(PresenceLibrary item, PresenceLibrary prior, int idx, EventList<PresenceLibrary> source) {
                            }
                        }.install(friendLibrary.getPresenceLibraryList());
                    }
                }
            }
        });
    }

    @Override
    public void handleEvent(LibraryChangedEvent event) {
        // The idea behind this is that we want to provide incremental updates to 
        // a PresenceLibrary, without requiring the entire library disappear
        // and reappear.  We need to know if adding the presence library succeeded,
        // but also need to trigger a browse if it didn't (because it already existed).
        FriendPresence friend = event.getData();
        PresenceLibrary existingLibrary = remoteLibraryManager.getPresenceLibrary(friend);
        if(!remoteLibraryManager.addPresenceLibrary(friend) && existingLibrary != null) {
            LOG.debugf("Library changed event for {0}, but existing library -- rebrowsing into existing library", friend);
            // the library already existed for this presence --
            // we need to trigger our own browse.
            // There's a small chance the existingLibrary is an older version of
            // a PresenceLibrary (not the current one) -- if that does happen,
            // the worst this will do is cause a second browse to happen.
            tryToResolveAndBrowse(existingLibrary, latestConnectivityEventRevision);
        }
    }
    
    void browse(final PresenceLibrary presenceLibrary) {
        
        // TODO: Is this needed again?  We should already be in loading
        presenceLibrary.setState(RemoteLibraryState.LOADING);
        
        final FriendPresence friendPresence = presenceLibrary.getPresence();
        
        AddressFeature addressFeature = ((AddressFeature)friendPresence.getFeature(AddressFeature.ID));
        if(addressFeature == null) {
            // happens during sign-off
            return;    
        }
        
        LOG.debugf("browsing {0} ...", friendPresence.getPresenceId());
        final Browse browse = browseFactory.createBrowse(friendPresence);
        
        // TODO: We need to capture the Browse and call stop on it when the library is removed,
        //       otherwise the browse can be lingering in the background.
        browse.start(new BrowseListener() {
            // Build an in-transit list and replace at the end, but only if there's
            // no existing list, or if we have enough memory to duplicate the list.
            private final List<SearchResult> transitList;
            
            // (anonymous constructor)
            {
                int size = presenceLibrary.size();
                if(size == 0) {
                    transitList = null;
                } else if (remoteLibraryManager.getAllFriendsLibrary().size() > 5000) {
                    // can run low on memory, so clear old list & add as new ones come.
                    presenceLibrary.clear();
                    transitList = null;
                } else {
                    transitList = new ArrayList<SearchResult>(size);
                }
            }
            
            public void handleBrowseResult(SearchResult searchResult) {
                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter)searchResult;
                // need to upgrade the RFD to be use the friendpresence.
                remoteFileDescAdapter = new RemoteFileDescAdapter(remoteFileDescAdapter, friendPresence);
                if(transitList != null) {
                    transitList.add(remoteFileDescAdapter);
                } else {
                    presenceLibrary.addNewResult(remoteFileDescAdapter);
                }
            }
            @Override
            public void browseFinished(boolean success) {
                if(transitList != null) {
                    LOG.debugf("Finished browse of {0}, setting resulting files into existing list", friendPresence);
                    presenceLibrary.setNewResults(transitList);
                } else {
                    LOG.debugf("Finished browse of {0}, no in-transit list.", friendPresence);
                }
                
                if(success) {
                    presenceLibrary.setState(RemoteLibraryState.LOADED);
                } else {
                    presenceLibrary.setState(RemoteLibraryState.FAILED_TO_LOAD);
                    LOG.debugf("browse failed: {0}", presenceLibrary);
                }
            }
        });
    }
    
    /**
     * Tries to resolve the address of <code>presenceLibrary<code> and browse it
     * after successful resolution and/or if it can connect to the address. Otherwise,
     * it handle the failure by calling {@link #handleFailedResolution(PresenceLibrary, int)}.
     * 
     * @param presenceLibrary the presence library whose address should be resolved
     * and browsed
     * @param startConnectivityRevision the revisions of {@link #latestConnectivityEventRevision}
     * when this method is called
     */
    void tryToResolveAndBrowse(final PresenceLibrary presenceLibrary, final int startConnectivityRevision) {
        presenceLibrary.setState(RemoteLibraryState.LOADING);
        final FriendPresence friendPresence = presenceLibrary.getPresence();
        AddressFeature addressFeature = (AddressFeature)friendPresence.getFeature(AddressFeature.ID);
        if (addressFeature == null) {
            LOG.debug("no address feature");
            handleFailedResolution(presenceLibrary, startConnectivityRevision);
            return;
        }
        Address address = addressFeature.getFeature();
        if (socketsManager.canResolve(address)) {
            socketsManager.resolve(address, new AddressResolutionObserver() {
                @Override
                public void resolved(Address address) {
                    if (socketsManager.canConnect(address)) {
                        LOG.debugf("resolved {0} for {1} and can connect", address, friendPresence);
                        browse(presenceLibrary);
                    } else {
                        LOG.debugf("resolved {0} for {1} and cannot connect", address, friendPresence);
                        handleFailedResolution(presenceLibrary, startConnectivityRevision);
                    }
                }
                @Override
                public void handleIOException(IOException iox) {
                    LOG.debug("resolve error", iox);
                    handleFailedResolution(presenceLibrary, startConnectivityRevision);
                }
                @Override
                public void shutdown() {
                }
            });
        } else if (socketsManager.canConnect(address)) {
            browse(presenceLibrary);
        } else {
            handleFailedResolution(presenceLibrary, startConnectivityRevision);
        }
    }
 
    /**
     * Called when resolution failed.
     * 
     * If {@link #latestConnectivityEventRevision} is greater then <code>startRevision</code>,
     * a new attempt at resolving the presence address is started, otherwise <code>presenceLibrary</code>
     * is queued up in libraries to browse.
     * 
     * @param presenceLibrary the library that could not be browsed
     * @param startRevision the revision under which the address resolution attempt
     * was started
     */
    private void handleFailedResolution(PresenceLibrary presenceLibrary, int startRevision) { 
        LOG.debugf("failed resolution for:{0} revision:{1}", presenceLibrary.getPresence().getPresenceId(), startRevision);
        presenceLibrary.setState(RemoteLibraryState.FAILED_TO_LOAD);
        
        boolean retry;
        synchronized (librariesToBrowse) {
            retry = latestConnectivityEventRevision > startRevision;
            if (!retry) {
                LOG.debugf("readding and not trying after fail {0}", presenceLibrary);
                boolean wasAdded = librariesToBrowse.add(presenceLibrary);
                assert(wasAdded);
            } else {
                // copy value under lock
                startRevision = latestConnectivityEventRevision;
                LOG.debugf("retrying with new revision {0}", startRevision);
            }
            LOG.debugf("libraries to browser after fail: {0}, size {1}", librariesToBrowse, librariesToBrowse.size());
        }
        if (retry) {
            tryToResolveAndBrowse(presenceLibrary, startRevision);
        }
    }
    
    /**
     * Is notified of better connection capabilities and iterates over the list of unbrowsable
     * presence libraries to see if they can be browsed now.
     */
    private class ConnectivityChangeListener implements EventListener<ConnectivityChangeEvent> {

        /**
         * Increments the {@link PresenceLibraryBrowser#latestConnectivityEventRevision}
         * copies and empties {@link PresenceLibraryBrowser#librariesToBrowse} and
         * tries calls {@link PresenceLibraryBrowser#tryToResolveAndBrowse(PresenceLibrary, int)}
         * for each with the new revision.
         */
        @Override
        public void handleEvent(ConnectivityChangeEvent event) {
            LOG.debug("connectivity change");
            List<PresenceLibrary> copy;
            int currentRevision;
            synchronized (librariesToBrowse) {
                currentRevision = ++latestConnectivityEventRevision;
                copy = new ArrayList<PresenceLibrary>(librariesToBrowse);
                librariesToBrowse.clear();
            }
            // outside of synchronized to avoid dead lock
            LOG.debugf("revision: {0}, libraries to browse again: {1}", currentRevision, copy);
            for (PresenceLibrary library : copy) {
                tryToResolveAndBrowse(library, currentRevision);
            }
        }
    }
}