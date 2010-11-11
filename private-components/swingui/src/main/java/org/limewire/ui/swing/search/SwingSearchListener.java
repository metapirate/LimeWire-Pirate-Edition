package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * An implementation of SearchListener to handle search results and update the
 * Swing UI.  Search events are forwarded to the data model and UI using the
 * event dispatch thread.
 */
class SwingSearchListener implements SearchListener {

    private final SearchResultsModel searchResultsModel;
    private final SearchNavItem searchNavItem;
    
    /**
     * Constructs a SwingSearchListener for the specified search model,
     * sponsored results view, and navigation item.
     */
    public SwingSearchListener(SearchResultsModel searchResultsModel,
            SearchNavItem searchNavItem) {
        this.searchResultsModel = searchResultsModel;
        this.searchNavItem = searchNavItem;
        
        // sync UI result count to real count.
        searchResultsModel.getUnfilteredList().addListEventListener(new ListEventListener<VisualSearchResult>() {
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                SwingSearchListener.this.searchNavItem.sourceCountUpdated(
                        SwingSearchListener.this.searchResultsModel.getResultCount());
            }
        });
    }

    @Override
    public void handleSearchResult(Search search, SearchResult searchResult) {
    }
    
    @Override
    public void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults) {
    }

    @Override
    public void searchStarted(Search search) {
    }

    @Override
    public void searchStopped(Search search) {
    }
}
