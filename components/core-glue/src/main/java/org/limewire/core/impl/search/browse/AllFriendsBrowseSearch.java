package org.limewire.core.impl.search.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.limewire.core.api.library.RemoteLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.RemoteLibraryEvent.Type;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.listener.EventListener;

class AllFriendsBrowseSearch extends AbstractBrowseSearch {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final ExecutorService backgroundExecutor;
    private final AllFriendsListEventListener listEventListener = new AllFriendsListEventListener();

    public AllFriendsBrowseSearch(RemoteLibraryManager remoteLibraryManager, ExecutorService backgroundExecutor) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.backgroundExecutor = backgroundExecutor;
    }


    @Override
    public void start() {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (SearchListener listener : searchListeners) {
                    listener.searchStarted(AllFriendsBrowseSearch.this);
                }
                
                installListener();
                loadSnapshot();
            }
        });
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
        }    
        
        removeListener();
    }


    private void loadSnapshot() {
        RemoteLibrary allFriendsLibrary = remoteLibraryManager.getAllFriendsLibrary();
        List<SearchResult> remoteFileItems = new ArrayList<SearchResult>(allFriendsLibrary.size());
        for (SearchResult searchResult : allFriendsLibrary) {
            remoteFileItems.add(searchResult);
        }
        
        //add all files
        for (SearchListener listener : searchListeners) {
            listener.handleSearchResults(this, remoteFileItems);
        }

        
        //browse is finished
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
        }
        
        BrowseStatus status = (remoteFileItems.size() > 0) ? new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.LOADED) :
            new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.NO_FRIENDS_SHARING);
        for (BrowseStatusListener listener : browseStatusListeners){
            listener.statusChanged(status);
        }

    }
    
    private void installListener(){
        remoteLibraryManager.getAllFriendsLibrary().addListener(listEventListener);
    }
    
    private void removeListener(){
        remoteLibraryManager.getAllFriendsLibrary().removeListener(listEventListener);        
    }
    
    private class AllFriendsListEventListener implements EventListener<RemoteLibraryEvent> {
        @Override
        public void handleEvent(RemoteLibraryEvent event) {
            if (event.getType() != Type.STATE_CHANGED) {
                BrowseStatus status = new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.UPDATED);
                for (BrowseStatusListener listener : browseStatusListeners){
                    listener.statusChanged(status);
                }
            }
        }        
    }

    @Override
    public void repeat() {
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                loadSnapshot();
            }
        });
    }

}
