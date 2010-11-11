package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.mainframe.MainPanel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The primary implementation of SearchHandler used to start a new search.
 * SearchHandlerImpl actually uses secondary search handlers to process search
 * requests based on the format of the search query.
 */
@Singleton
class SearchHandlerImpl implements SearchHandler {
    
    private final SearchHandler textSearch;
    private final MainPanel mainPanel;

    /**
     * Constructs a SearchHandlerImpl with the specified secondary search 
     * handlers for p2p and text searches, and main window panel.
     */
    @Inject
    public SearchHandlerImpl(
                        @Named("text") SearchHandler textSearch,
                        MainPanel mainPanel) {
        this.textSearch = textSearch;
        this.mainPanel = mainPanel;
    }

    /**
     * Performs a search operation using the specified SearchInfo object.  The
     * task is forwarded to a secondary search handler based on the format of 
     * the search query.  Returns true if the search request is accepted.
     */
    @Override
    public boolean doSearch(SearchInfo info) {
        if(info.getSearchCategory() == SearchCategory.PROGRAM && !LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            mainPanel.showTemporaryPanel(new ProgramsNotAllowedPanel());
            return false;
        } else {                    
//            String q = info.getSearchQuery();
//            if(q != null && q.toLowerCase(Locale.US).startsWith("p2p://")) {
//                return p2pLinkSearch.doSearch(info);
//            } else {
                return textSearch.doSearch(info);
//            }
        }
    }

}
