package org.limewire.core.api.search;

import java.util.Collection;

/** A listener for a search. */
public interface SearchListener {
    
    /** Notification a new search result is received for the search. */
    void handleSearchResult(Search search, SearchResult searchResult);
    
    /** Adds many search results at once. */
    void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults);
    
    /** Notification the search has started. */
    void searchStarted(Search search);
    
    /** Notification the search has stopped. */
    void searchStopped(Search search);
}
