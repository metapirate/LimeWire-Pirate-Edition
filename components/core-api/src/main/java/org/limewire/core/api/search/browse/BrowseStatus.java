package org.limewire.core.api.search.browse;

import java.util.Arrays;
import java.util.List;

import org.limewire.friend.api.Friend;
import org.limewire.util.StringUtils;

public class BrowseStatus {
    
    public enum BrowseState {
        /** The browse completed successfully. */
        LOADED(true),
        /** The browse failed. */
        FAILED(false),
        /** Some of the browses failed (but some succeeded) -- used in multi-browse searches. */
        PARTIAL_FAIL(true),
        /** Some updates are available, but it is not completed. */
        UPDATED(true),
        /** Not all browses have completed, but none have failed yet. */
        LOADING(true),
        /** Useful in multi-browse: some browses have updates, but others have failed. */
        UPDATED_PARTIAL_FAIL(true),
        /** Useful for friend browses: the friend is currently offline. */
        OFFLINE(false),
        /** Useful for multi-friend browses: some friends are online, but sharing nothing. */
        NO_FRIENDS_SHARING(false) ;
        
        private boolean ok;
        BrowseState(boolean ok){
            this.ok = ok;
        }
        
        /**
         * @return true if any files have loaded or if there is a chance of files loading (including UPDATED), false if
         * the browse has failed or there are no files to load
         */
        public boolean isOK(){
            return ok;
        }
    }    
    
    private final BrowseState state;
    private final List<Friend> failed;
    private final BrowseSearch search;
    
    public BrowseStatus(BrowseSearch search, BrowseState state, Friend... failedFriends){
        this.search = search;
        this.state = state;
        this.failed = Arrays.asList(failedFriends);
    }
    
    public BrowseState getState(){
        return state;
    }
    
    public List<Friend> getFailedFriends(){
        return failed;
    }

    public BrowseSearch getBrowseSearch() {
        return search;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
