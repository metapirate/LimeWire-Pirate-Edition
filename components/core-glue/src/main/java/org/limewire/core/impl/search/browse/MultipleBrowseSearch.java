package org.limewire.core.impl.search.browse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Aggregates multiple AnonymousSingleBrowseSearches and FriendSingleBrowseSearches.
 *
 */
class MultipleBrowseSearch extends AbstractBrowseSearch {
    
    private static final Log LOG = LogFactory.getLog(MultipleBrowseSearch.class);
    
    private static final int PARALLEL_BROWSES = 2;

    private final CombinedSearchListener combinedSearchListener = new CombinedSearchListener();
    private final CombinedBrowseStatusListener combinedBrowseStatusListener = new CombinedBrowseStatusListener();
    private final List<BrowseSearch> activeBrowses = new CopyOnWriteArrayList<BrowseSearch>();
    private final List<FriendPresence> presences;
    private final Queue<FriendPresence> pendingPresences;
    private final BrowseSearchFactory browseSearchFactory;

    /**
     * @param presences the people to be browsed. Can not be null.
     */
    public MultipleBrowseSearch(BrowseSearchFactory browseSearchFactory, Collection<FriendPresence> presences) {
        this.browseSearchFactory = browseSearchFactory;
        this.presences = new ArrayList<FriendPresence>(presences);
        this.pendingPresences = new ConcurrentLinkedQueue<FriendPresence>();
    }
 
    @Override
    public void start() {
        pendingPresences.addAll(presences);
        
        // Start PARALLEL_BROWSES browses -- as each one finishes, it will start the next browse.
        // This prevents us from doing more than PARALLEL_BROWSES browses in parallel.
        for(int i = 0; i < PARALLEL_BROWSES && !pendingPresences.isEmpty(); i++) {
            if(!startPendingBrowse()) {
                break;
            }
        }
    }
    
    /** Starts a pending browse.  Returns true if it succesfully started. */
    private boolean startPendingBrowse() {
        FriendPresence host = pendingPresences.poll();
        if(host != null) {
            LOG.debugf("Starting browse for host {0}", host);
            BrowseSearch browse = browseSearchFactory.createBrowseSearch(host);
            browse.addSearchListener(combinedSearchListener);
            browse.addBrowseStatusListener(combinedBrowseStatusListener);
            activeBrowses.add(browse);
            browse.start();
            return true;
        } else {
            LOG.debugf("Attempted to start pending browse, but no hosts left.");
            return false;
        }
    }

    @Override
    public void stop() {
        // order here is very important --
        // we clear pending hosts first, so that
        // stopped browses don't start another pending browse.
        pendingPresences.clear();
        for (BrowseSearch browse: activeBrowses){
            browse.stop();
        }
    }
    

    @Override
    public void repeat() {
        // order here is very important --
        // we stop first and then clear,
        // so that events from the stop
        // get cleared out.
        stop();
        combinedBrowseStatusListener.clear();
        combinedSearchListener.clear();        
        start();
    }
    
    private class CombinedSearchListener implements SearchListener {
        /** Keeps count of how many browses have completed */
        private AtomicInteger stoppedBrowses = new AtomicInteger(0);

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(MultipleBrowseSearch.this, searchResult);
            }
        }
        
        @Override
        public void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResults(MultipleBrowseSearch.this, searchResults);
            }
        }
        
        /** Clears the count of completed browses */
        public void clear() {
            stoppedBrowses.set(0);
        }

        @Override
        public void searchStarted(Search search) {
            for (SearchListener listener : searchListeners) {
                listener.searchStarted(MultipleBrowseSearch.this);
            }
        }

        @Override
        public void searchStopped(Search search) {
            LOG.debugf("Received search stopped event {0}", search);
            if (stoppedBrowses.incrementAndGet() == presences.size()) {
                //all of our browses have completed
                for (SearchListener listener : searchListeners) {
                    listener.searchStopped(MultipleBrowseSearch.this);
                }
            }
        }        
    }
    
    private class CombinedBrowseStatusListener implements BrowseStatusListener {
        /** List of all failed browses (Friends includes anonymous) */
        private List<Friend> failedList = new CopyOnWriteArrayList<Friend>();
        /** The number of BrowseSearches in the LOADED state */
        private AtomicInteger loaded = new AtomicInteger(0);
        /** Whether or not there are updates in any of the browses */
        private AtomicBoolean hasUpdated = new AtomicBoolean(false);

        @Override
        public void statusChanged(BrowseStatus status) {
            LOG.debugf("Received status change event {0}", status);
            switch(status.getState()) {
            case FAILED:
            case OFFLINE:
                //getFailedFriends() will only return 1 person 
                //since status is from a single browse
                failedList.addAll(status.getFailedFriends());
                break;
            case UPDATED:
                hasUpdated.set(true);
                break;
            case LOADED:
                loaded.incrementAndGet();
                break;
            }
            
           BrowseState state = getReleventMultipleBrowseState(status);
            if (state != null) {
                BrowseStatus browseStatus = new BrowseStatus(MultipleBrowseSearch.this, state, failedList.toArray(new Friend[failedList.size()]));
                for (BrowseStatusListener listener : browseStatusListeners) {
                    listener.statusChanged(browseStatus);
                }
            }

            activeBrowses.remove(status.getBrowseSearch());
            startPendingBrowse();
        }        
        
        /**
         * Clears all cached data about the browses
         */
        public void clear(){
            hasUpdated.set(false);
            loaded.set(0);
            failedList.clear();
        }
        
        /**
         * @return The aggregated status of the browses.  For example if the browses are a mix of 
         * FAILED and LOADED, the status will be PARTIAL_FAIL.  If the browses contain, FAILED, 
         * LOADED, and UPDATED, it will be UPDATED_PARTIAL_FAIL.
         */
        private BrowseState getReleventMultipleBrowseState(BrowseStatus status){
            if(loaded.get() == presences.size()){
                return BrowseState.LOADED;
            } else if(failedList.size() == presences.size()){
                return BrowseState.FAILED;
            } else if (failedList.size() > 0) {
                if (loaded.get() > 0) {
                    if (hasUpdated.get()) {
                        return BrowseState.UPDATED_PARTIAL_FAIL;
                    } else {
                        return BrowseState.PARTIAL_FAIL;
                    }
                }
            } else if (hasUpdated.get()){
                return BrowseState.UPDATED;
            }
            
            return BrowseState.LOADING;
        }
    }
    
}
