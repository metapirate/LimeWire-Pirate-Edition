package org.limewire.core.api.search;

import java.util.Collection;

import org.limewire.core.api.URN;
import org.limewire.io.GUID;
import org.limewire.listener.ListenerSupport;

import ca.odell.glazedlists.EventList;

/**
 * Defines the API for the list of results for a single search.  
 * Implementations of SearchResultList should notify registered listeners when
 * results are added to the list.
 */
public interface SearchResultList extends ListenerSupport<Collection<GroupedSearchResult>> {

    /**
     * Returns the GUID associated with the search.  May be null if the search
     * has not started, or if the search is a browse.
     */
    GUID getGuid();
    
    /**
     * Returns the total number of results found.
     */
    int getResultCount();
    
    /**
     * Returns the search associated with this list.  Never null.
     */
    Search getSearch();
    
    /**
     * Returns the query associated with the search.
     */
    String getSearchQuery();
    
    /**
     * Returns the search result associated with the specified URN.
     */
    GroupedSearchResult getGroupedResult(URN urn);
    
    /**
     * Returns the list of search results sorted and grouped by URN.
     */
    EventList<GroupedSearchResult> getGroupedResults();
    
    /**
     * Clears all results.
     */
    void clear();
    
    /**
     * Disposes of resources and removes listeners.
     */
    void dispose();
}
