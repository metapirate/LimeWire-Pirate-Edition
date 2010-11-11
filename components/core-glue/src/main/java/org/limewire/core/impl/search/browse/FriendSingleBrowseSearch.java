package org.limewire.core.impl.search.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.RemoteLibraryState;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;
import org.limewire.listener.EventListener;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class FriendSingleBrowseSearch extends AbstractBrowseSearch {

    private final Friend friend;
    private final RemoteLibraryManager remoteLibraryManager;
    private final ExecutorService executorService;
    private final ListEventListener<FriendLibrary> friendLibraryListEventListener = new FriendLibraryListEventListener();
    private final EventListener<RemoteLibraryEvent> eventAdapter = new RemoteLibraryToBrowseEventAdapter();
    
    private final AtomicReference<FriendLibrary> currentLibrary = new AtomicReference<FriendLibrary>();
    private boolean hasRegisteredListener = false;
    private final AtomicBoolean hasStopped = new AtomicBoolean(false);
    private Future startFuture = null;

    /**
     * @param friend the person to be browsed - can not be anonymous or null
     */
    public FriendSingleBrowseSearch(RemoteLibraryManager remoteLibraryManager, Friend friend, ExecutorService executorService) {
        assert(friend != null && !friend.isAnonymous());
        this.friend = friend;
        this.remoteLibraryManager = remoteLibraryManager;
        this.executorService = executorService;
    }


    @Override
    public void start() {
        startFuture = executorService.submit(new Runnable() {
            public void run() {
                for (SearchListener listener : searchListeners) {
                    if(hasStopped.get())
                        return;
                    listener.searchStarted(FriendSingleBrowseSearch.this);
                }

                if(!hasStopped.get()) {
                    synchronized(FriendSingleBrowseSearch.this) {
                        installListener();
                    }
                }
                if(!hasStopped.get()) {
                    startFriendBrowse();
                }
            }
        });
    }

    @Override
    public void stop() {
        if(startFuture != null && !startFuture.isDone()) {
            startFuture.cancel(true);
        }
        hasStopped.set(true);
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        }       
        synchronized(this) {
            removeListener();
        }
    }


    private void startFriendBrowse() {
        FriendLibrary library = remoteLibraryManager.getFriendLibrary(friend);
        
        if (library == null) {
            // Failed!
           fireBrowseStatusChanged(BrowseState.OFFLINE, friend);
           for (SearchListener listener : searchListeners) {
               listener.searchStopped(FriendSingleBrowseSearch.this);
           }  
            
        } else {
            setLibrary(library);
            if(library.getState() == RemoteLibraryState.LOADING){
                library.addListener(eventAdapter);                
            } else {
                loadLibrary();
            }
        }
 
    }
    
    /**Loads a snapshot of the available files, alerts BrowseStatusListeners that we have loaded, 
     * and SearchListeners that the search has stopped.*/
    private void loadLibrary(){
        FriendLibrary friendLibrary = remoteLibraryManager.getFriendLibrary(friend);
        List<SearchResult> searchResults = new ArrayList<SearchResult>(friendLibrary.size());
        for (SearchResult result : friendLibrary) {
            searchResults.add(result);
        }
        
        // add all files
        for (SearchListener listener : searchListeners) {
            listener.handleSearchResults(this, searchResults);
        }
        
        fireBrowseStatusChanged(BrowseState.LOADED);
        
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        } 
    }
    
    /**Adds friendLibraryListEventListener to the FriendLibraryList*/
    private void installListener() {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(friendLibraryListEventListener);
        hasRegisteredListener = true;
    }
    
    /**Removes friendLibraryListEventListener from the FriendLibraryList.  
     * Removes libraryPropertyChangeLister from the friend library if necessary.*/
    private void removeListener() {
        if(hasRegisteredListener) {
            remoteLibraryManager.getFriendLibraryList().removeListEventListener(friendLibraryListEventListener);
        }
        hasRegisteredListener = false;
        setLibrary(null);
    }
    
    private void setLibrary(FriendLibrary newLibrary){
        FriendLibrary oldLibrary = currentLibrary.getAndSet(newLibrary);
        if(newLibrary == oldLibrary){
            return;
        }
        if(oldLibrary != null){
            oldLibrary.removeListener(eventAdapter);
        }
        if (newLibrary == null) {
            return;
        }

        newLibrary.addListener(eventAdapter);
        if(newLibrary.getState() == RemoteLibraryState.LOADED){
            fireBrowseStatusChanged(BrowseState.UPDATED);
        }
        
    }
    
    private void fireBrowseStatusChanged(BrowseState state, Friend... friends){
        BrowseStatus status = new BrowseStatus(FriendSingleBrowseSearch.this, state, friends);
        for (BrowseStatusListener listener : browseStatusListeners) {
            listener.statusChanged(status);
        } 
    }

    private class FriendLibraryListEventListener implements ListEventListener<FriendLibrary> {
        @Override
        public void listChanged(ListEvent listChanges) {
            while (listChanges.next()) {
                if (listChanges.getType() == ListEvent.INSERT) {
                    FriendLibrary newLibrary = (FriendLibrary) listChanges.getSourceList().get(listChanges.getIndex());
                    if (newLibrary.getFriend().getId().equals(friend.getId())) {//There is a new library for our friend!
                        setLibrary(remoteLibraryManager.getFriendLibrary(friend));
                    }
                } else if (listChanges.getType() == ListEvent.DELETE && remoteLibraryManager.getFriendLibrary(friend) == null){   
                    //our friend has logged off
                    setLibrary(null);
                    fireBrowseStatusChanged(BrowseState.OFFLINE, friend);
                }
            }
        }
    }
    
    private class RemoteLibraryToBrowseEventAdapter implements EventListener<RemoteLibraryEvent> {
        @Override
        public void handleEvent(RemoteLibraryEvent event) {
            switch (event.getType()) {
            case STATE_CHANGED:
                RemoteLibraryState state = event.getState();
                if (state != RemoteLibraryState.LOADING) {
                    // The list has changed - tell the listeners
                    if (state == RemoteLibraryState.LOADED) {
                        fireBrowseStatusChanged(BrowseState.UPDATED);
                    } else {
                        fireBrowseStatusChanged(BrowseState.FAILED, friend);
                    }
                }
                break;
            case RESULTS_ADDED:
            case RESULTS_CLEARED:
            case RESULTS_REMOVED:
                fireBrowseStatusChanged(BrowseState.UPDATED);
                break;
            }
        }
    }

    @Override
    public void repeat() {
        stop();
        hasStopped.set(false);
        start();
    }
}
