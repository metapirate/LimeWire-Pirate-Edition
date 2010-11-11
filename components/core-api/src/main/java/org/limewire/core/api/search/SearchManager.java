package org.limewire.core.api.search;

import java.util.List;

import org.limewire.io.GUID;

/**
 * Defines the API for the search manager.
 */
public interface SearchManager {

    /**
     * Adds the specified search to the manager and returns its result list.
     * A monitored search will be automatically cancelled if no attempt is 
     * made to reference its result list for a period of time.
     * 
     * <p>This method should be called before starting the search; otherwise, the
     * result list may not contain results generated before the list was
     * created.</p>
     */
    SearchResultList addMonitoredSearch(Search search, SearchDetails searchDetails);
    
    /**
     * Adds the specified search to the manager and returns its result list.
     * The search will remain alive until it is explicitly stopped.
     * 
     * <p>This method should be called before starting the search; otherwise, the
     * result list may not contain results generated before the list was
     * created.</p>
     */
    SearchResultList addSearch(Search search, SearchDetails searchDetails);
    
    /**
     * Removes the specified search from the manager.  This method should be
     * called after stopping a search to dispose of resources.
     */
    void removeSearch(Search search);
    
    /**
     * Stops the search associated with the specified result list, and removes
     * the search from the manager.
     */
    void stopSearch(SearchResultList resultList);
    
    /**
     * Returns a list of active searches.  An active search has been assigned
     * a GUID, which includes network searches started but excludes unstarted
     * searches and browse searches.
     */
    List<SearchResultList> getActiveSearchLists();
    
    /**
     * Returns the result list for the specified GUID.  The method returns null
     * if there is no list associated with the GUID.
     */
    SearchResultList getSearchResultList(GUID guid);
    
    /**
     * Returns the result list for the specified search.  The method returns 
     * null if there is no list associated with the search.
     */
    SearchResultList getSearchResultList(Search search);
}
