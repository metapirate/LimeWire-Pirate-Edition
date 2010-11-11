package org.limewire.ui.swing.search.model;

/**
 * This class takes a parameter an array of SimilarResultDetectors. It calls
 * detectSimilarResults on each of the given detectors when detectSimilarResult
 * is called on itself.
 */
public class MultiSimilarResultDetector implements SimilarResultsDetector {

    private final SimilarResultsDetector[] similarResultsDetectors;

    public MultiSimilarResultDetector(SimilarResultsDetector... similarResultsDetectors) {
        this.similarResultsDetectors = similarResultsDetectors;
    }

    @Override
    public void detectSimilarResult(VisualSearchResult result) {
        for (SimilarResultsDetector similarResultsDetector : similarResultsDetectors) {
            similarResultsDetector.detectSimilarResult(result);
        }
    }

    @Override
    public void removeSpamItem(VisualSearchResult result, VisualSearchResult newParent) {
        for (SimilarResultsDetector similarResultsDetector : similarResultsDetectors) {
            similarResultsDetector.removeSpamItem(result, newParent);
        }
    }

    @Override
    public void clear() {
        for (SimilarResultsDetector similarResultsDetector : similarResultsDetectors) {
            similarResultsDetector.clear();
        }
    }
}
