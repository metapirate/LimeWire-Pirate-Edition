package org.limewire.core.impl.search.browse;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.FriendPresence;

class AnonymousSingleBrowseSearch extends AbstractBrowseSearch {
   
    private static volatile int successesCount = 0;
    private static volatile int failuresCount = 0;
    
    private final FriendPresence friendPresence;
    private final BrowseFactory browseFactory;

    private Browse browse;

    /**
     * @param friendPresence the person to be browsed - must be anonymous and can not be null; 
     */
    public AnonymousSingleBrowseSearch(BrowseFactory browseFactory, FriendPresence friendPresence) {
        assert(friendPresence != null && friendPresence.getFriend().isAnonymous());
        this.friendPresence = friendPresence;
        this.browseFactory = browseFactory;
    }
   

    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(AnonymousSingleBrowseSearch.this);
        }
        browse = browseFactory.createBrowse(friendPresence);
        browse.start(new BrowseEventForwarder());
    }

    @Override
    public void stop() {
        // let the stopping of the browse trigger the events.
        assert (browse != null);
        browse.stop();
    }


    /**
     * Forwards browse information to searchListeners.
     */
    private class BrowseEventForwarder implements BrowseListener {

        @Override
        public void browseFinished(final boolean success) {
            BrowseStatus status;
            
            if (success) {
                successesCount++;                
                status = new BrowseStatus(AnonymousSingleBrowseSearch.this, BrowseState.LOADED);
            } else {
                failuresCount++;
                status = new BrowseStatus(AnonymousSingleBrowseSearch.this, BrowseState.FAILED, friendPresence.getFriend());
            }
            
            for (SearchListener listener : searchListeners) {
                listener.searchStopped(AnonymousSingleBrowseSearch.this);
            }
           
            for (BrowseStatusListener listener : browseStatusListeners) {
                listener.statusChanged(status);
            }
        }

        @Override
        public void handleBrowseResult(final SearchResult searchResult) {              
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(AnonymousSingleBrowseSearch.this, searchResult);
            }

        }
    }


    @Override
    public void repeat() {
        stop();
        start();
    }

}
