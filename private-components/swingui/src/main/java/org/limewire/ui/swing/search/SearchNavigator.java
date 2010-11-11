package org.limewire.ui.swing.search;

import javax.swing.JComponent;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.ui.swing.search.model.SearchResultsModel;

/** A central hub for showing a new search in the UI. */
public interface SearchNavigator {
    
    /** Adds a new search whose results will show in the given panel. */
    SearchNavItem addSearch(String title, JComponent searchPanel, Search search, SearchResultsModel model);
    
    /** Adds a new browse search whose results will show in the given panel. */
    SearchNavItem addSearch(String title, JComponent searchPanel, BrowseSearch search, SearchResultsModel model);

    /** Opens a new advanced search display. */
    void openAdvancedSearch();

}
