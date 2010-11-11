package org.limewire.ui.swing.search;

import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SearchResultsModelFactory;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

public class BrowsePanelFactory {

    private SearchResultsModelFactory searchResultsModelFactory;

    private SearchResultsPanelFactory searchResultsPanelFactory;

    @Inject
    public BrowsePanelFactory(SearchResultsModelFactory searchResultsModelFactory,
            SearchResultsPanelFactory searchResultsPanelFactory) {
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.searchResultsPanelFactory = searchResultsPanelFactory;
    }

    public SearchResultsPanel createBrowsePanel(BrowseSearch search, SearchInfo searchInfo) {
        SearchResultsModel searchModel = searchResultsModelFactory.createSearchResultsModel(
                searchInfo, search);
        final SearchResultsPanel searchPanel = searchResultsPanelFactory
                .createSearchResultsPanel(searchModel);

        search.addBrowseStatusListener(new SearchPanelBrowseStatusListener(searchPanel));
        return searchPanel;
    }

    
     /**
     * Sets the BrowseStatus of the SearchResultsPanel when the status changes.
     */
    private static class SearchPanelBrowseStatusListener implements BrowseStatusListener {
        private SearchResultsPanel searchPanel;

        public SearchPanelBrowseStatusListener(SearchResultsPanel searchPanel) {
            this.searchPanel = searchPanel;
        }

        @Override
        public void statusChanged(final BrowseStatus status) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    searchPanel.setBrowseStatus(status);
                }
            });
        }
    }


}
