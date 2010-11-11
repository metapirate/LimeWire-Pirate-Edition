package org.limewire.ui.swing.search.model;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.DownloadExceptionHandler;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Implements a factory for creating the search results data model.
 */
public class SearchResultsModelFactory {

    private final SearchManager searchManager;
    
    private final SpamManager spamManager;

    private final SimilarResultsDetectorFactory similarResultsDetectorFactory;

    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;

    private final VisualSearchResultFactory vsrFactory;

    /**
     * Constructs a SearchResultsModelFactory with the specified factories,
     * managers, and property values.
     */
    @Inject
    public SearchResultsModelFactory(SearchManager searchManager,
            SimilarResultsDetectorFactory similarResultsDetectorFactory,
            SpamManager spamManager, LibraryManager libraryManager,
            DownloadListManager downloadListManager,
            Provider<PropertiableHeadings> propertiableHeadings,
            Provider<DownloadExceptionHandler> downloadExceptionHandler,
            VisualSearchResultFactory vsrFactory) {
        this.searchManager = searchManager;
        this.similarResultsDetectorFactory = similarResultsDetectorFactory;
        this.spamManager = spamManager;
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.vsrFactory = vsrFactory;
    }

    /**
     * Creates a new instance of SearchResultsModel.
     */
    public SearchResultsModel createSearchResultsModel(SearchInfo searchInfo, Search search) {
        // Create search result model.
        BasicSearchResultsModel searchResultsModel = new BasicSearchResultsModel(
                searchInfo, search, vsrFactory, downloadListManager, 
                downloadExceptionHandler, searchManager);

        // Create detector to find similar results.
        SimilarResultsDetector similarResultsDetector = similarResultsDetectorFactory.newSimilarResultsDetector();

        // Add list listener for results already downloaded or being downloaded. 
        // AlreadyDownloaded listener needs to be added to the list before the
        // grouping listener because the grouping listener uses values set by 
        // the AlreadyDownloaded listener.
        AlreadyDownloadedListEventListener alreadyDownloadedListEventListener = 
                new AlreadyDownloadedListEventListener(libraryManager, downloadListManager);
        searchResultsModel.addResultListener(alreadyDownloadedListEventListener);

        // Add list listener to group similar results.
        if (SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue()) {
            GroupingListEventListener groupingListEventListener = new GroupingListEventListener(similarResultsDetector);
            searchResultsModel.addResultListener(groupingListEventListener);
        }

        // Add list listener to handle spam results.
        SpamListEventListener spamListEventListener = new SpamListEventListener(spamManager, similarResultsDetector);
        searchResultsModel.addResultListener(spamListEventListener);

        return searchResultsModel;
    }

}
