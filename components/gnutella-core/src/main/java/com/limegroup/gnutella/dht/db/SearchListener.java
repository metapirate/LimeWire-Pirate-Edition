package com.limegroup.gnutella.dht.db;

public interface SearchListener<Result> {
    
    /**
     * Called when a result has been found, can be called several times,
     * depending on the kind of search.
     */
    void handleResult(Result result);

    /**
     * Is called when a search has been performed, no result has been returned 
     * or an exception occurred during lookup. 
     */
    void searchFailed();
    
}
