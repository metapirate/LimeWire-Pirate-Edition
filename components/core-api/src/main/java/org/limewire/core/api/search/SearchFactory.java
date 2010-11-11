package org.limewire.core.api.search;

/**
 * Creates new SearchRequests.
 */
public interface SearchFactory {
    
    Search createSearch(SearchDetails searchDetails);

}
