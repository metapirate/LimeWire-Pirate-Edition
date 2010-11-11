package com.limegroup.gnutella.dht.db;

/**
 * Adapter implementation of {@link SearchListener} with no-ops. 
 */
public class SearchListenerAdapter<Result> implements SearchListener<Result> {

    private static final SearchListener NULL_LISTENER = new SearchListenerAdapter();
    
    /**
     * Returns Null listener for type <code>T</code> which doesn't do anything.
     */
    @SuppressWarnings({ "unchecked", "cast" })
    public static final <T> SearchListener<T> nullListener() {
        return (SearchListener<T>)NULL_LISTENER;
    }
    
    public static final <T> SearchListener<T> nonNullListener(SearchListener<T> listener) {
        if (listener != null) {
            return listener;
        } else {
            return nullListener();
        }
    }
    
    public void handleResult(Result result) {
    }
    
    public void searchFailed() {
    }

}
