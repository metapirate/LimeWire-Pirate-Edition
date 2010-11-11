package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.inject.LazySingleton;
import org.limewire.io.GUID;

import com.google.inject.Inject;

/**
 * Implementation of SearchManager for the live core.
 */
@LazySingleton
public class CoreSearchManager implements SearchManager {

    private final List<SearchResultList> threadSafeSearchList;
    private final SearchMonitor searchMonitor;
    
    /**
     * Constructs a CoreSearchManager with the specified services.
     */
    @Inject
    public CoreSearchManager(SearchMonitor searchMonitor) {
        this.searchMonitor = searchMonitor;
        this.threadSafeSearchList = new CopyOnWriteArrayList<SearchResultList>();
    }
    
    @Inject
    void register() {
        searchMonitor.setSearchManager(this);
    }
    
    @Override
    public SearchResultList addMonitoredSearch(Search search, SearchDetails searchDetails) {
        // Add search.
        SearchResultList resultList = addSearch(search, searchDetails);

        // Add search result list to monitor.
        searchMonitor.addSearch(resultList);

        return resultList;
    }
    
    @Override
    public SearchResultList addSearch(Search search, SearchDetails searchDetails) {
        // Create result list.
        SearchResultList resultList = new CoreSearchResultList(search, searchDetails);
        
        // Add result list to collection.
        threadSafeSearchList.add(resultList);
        
        return resultList;
    }

    @Override
    public void removeSearch(Search search) {
        // Dispose of result list and remove from collection.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (search.equals(resultList.getSearch())) {
                // Dispose search and remove from management.
                resultList.dispose();
                threadSafeSearchList.remove(resultList);
                // Remove from search monitor.
                searchMonitor.removeSearch(resultList);
                break;
            }
        }
    }

    @Override
    public void stopSearch(SearchResultList resultList) {
        // Stop search and remove from management.
        resultList.getSearch().stop();
        resultList.dispose();
        threadSafeSearchList.remove(resultList);
    }
    
    @Override
    public List<SearchResultList> getActiveSearchLists() {
        List<SearchResultList> list = new ArrayList<SearchResultList>();
        
        // Add active searches to list.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (resultList.getGuid() != null) {
                // Update search monitor.
                searchMonitor.updateSearch(resultList);
                list.add(resultList);
            }
        }
        
        return list;
    }

    @Override
    public SearchResultList getSearchResultList(GUID guid) {
        // Return result list from collection.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (guid.equals(resultList.getGuid())) {
                // Update search monitor.
                searchMonitor.updateSearch(resultList);
                return resultList;
            }
        }
        
        // Return null if search not found.
        return null;
    }

    @Override
    public SearchResultList getSearchResultList(Search search) {
        // Return result list from collection.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (search.equals(resultList.getSearch())) {
                // Update search monitor.
                searchMonitor.updateSearch(resultList);
                return resultList;
            }
        }

        // Return null if search not found.
        return null;
    }
}
