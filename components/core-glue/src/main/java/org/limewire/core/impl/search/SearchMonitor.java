package org.limewire.core.impl.search;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.collection.Periodic;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * A monitor for search results.  SearchMonitor is used to automatically 
 * cancel searches that have not been referenced for a long period of time.
 */
class SearchMonitor {
    /** Timeout period of 5 minutes. */
    private static final long TIMEOUT_MSEC = 5 * 60 * 1000;
    
    private final ScheduledExecutorService backgroundExecutor;
    private final Map<SearchResultList, Periodic> resultMap;
    
    private SearchManager searchManager;
    
    /**
     * Constructs a SearchMonitor.
     */
    @Inject
    public SearchMonitor(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
        this.resultMap = new ConcurrentHashMap<SearchResultList, Periodic>();
    }
    
    /**
     * Sets the search manager.
     */
    public void setSearchManager(SearchManager searchManager) {
        this.searchManager = searchManager;
    }
    
    /**
     * Adds the specified search result list to the set being monitored.
     */
    public void addSearch(SearchResultList resultList) {
        // Create periodic task and add to map of monitored results.
        Periodic periodic = new Periodic(new CancelTask(resultList), backgroundExecutor);
        resultMap.put(resultList, periodic);
        
        // Schedule task to cancel search.
        periodic.rescheduleIfLater(TIMEOUT_MSEC);
    }
    
    /**
     * Removes the specified search result list from the set being monitored.
     */
    public void removeSearch(SearchResultList resultList) {
        Periodic periodic = resultMap.remove(resultList);
        if (periodic != null) {
            periodic.unschedule();
        }
    }
    
    /**
     * Updates the cancellation time of the specified search result list.
     */
    public void updateSearch(SearchResultList resultList) {
        Periodic periodic = resultMap.get(resultList);
        if (periodic != null) {
            periodic.rescheduleIfLater(TIMEOUT_MSEC);
        }
    }
    
    /**
     * Task to cancel a search.  This is executed by the search monitor when
     * it has determined that the search has timed out.
     */
    private class CancelTask implements Runnable {
        private final SearchResultList resultList;
        
        public CancelTask(SearchResultList resultList) {
            this.resultList = resultList;
        }

        @Override
        public void run() {
            // Stop search.
            searchManager.stopSearch(resultList);
            // Remove from search monitor.
            removeSearch(resultList);
        }
    }
}
