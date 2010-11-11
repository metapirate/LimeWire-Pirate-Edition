package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.model.SearchResultsModel;

public interface BrowseFailedMessagePanelFactory {
    public BrowseFailedMessagePanel create(SearchResultsModel searchResultsModel);
}
