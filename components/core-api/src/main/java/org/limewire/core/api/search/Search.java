package org.limewire.core.api.search;

/**
 * A single search.
 */
public interface Search {
    
    /** Returns the category this search is for. */
    SearchCategory getCategory();
    
    /** Adds a new SearchListener. */
    void addSearchListener(SearchListener searchListener);
    
    /** Removes a SearchListener. */
    void removeSearchListener(SearchListener searchListener);
    
    /** Starts the search. */
    void start();
    
    /** Repeats the search. */
    void repeat();
    
    /** Stops the search. */
    void stop();

}
