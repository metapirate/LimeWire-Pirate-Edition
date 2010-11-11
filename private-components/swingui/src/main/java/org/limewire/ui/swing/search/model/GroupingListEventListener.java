package org.limewire.ui.swing.search.model;

import static org.limewire.ui.swing.search.model.VisualSearchResult.NEW_SOURCES;

/**
 * A listener to handle updates to the list of visual search results.  This
 * listener applies a SimilarResultsDetector to each VisualSearchResult to
 * allow similar results to be grouped.
 */
class GroupingListEventListener implements VisualSearchResultStatusListener {

    private final SimilarResultsDetector similarResultsDetector;

    /**
     * Constructs a GroupingListEventListener with the specified similar 
     * results detector.
     */
    public GroupingListEventListener(SimilarResultsDetector similarResultsDetector) {
        this.similarResultsDetector = similarResultsDetector;
    }

    @Override
    public void resultCreated(VisualSearchResult vsr) {
        similarResultsDetector.detectSimilarResult(vsr);
    }
    
    @Override
    public void resultChanged(VisualSearchResult vsr, String propertyName, Object oldValue, Object newValue) {
        if (NEW_SOURCES.equals(propertyName)) {
            similarResultsDetector.detectSimilarResult(vsr);
        }
    }
    
    @Override
    public void resultsCleared() {
        similarResultsDetector.clear();
    }
}
