package org.limewire.ui.swing.search.resultpanel;

import java.util.List;

import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface SearchResultMenuFactory {
    public SearchResultMenu create(DownloadHandler downloadHandler, List<VisualSearchResult> selectedItems, SearchResultMenu.ViewType type);
}
