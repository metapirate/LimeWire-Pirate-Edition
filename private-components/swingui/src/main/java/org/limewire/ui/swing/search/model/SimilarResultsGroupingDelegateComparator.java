package org.limewire.ui.swing.search.model;

import java.util.Comparator;

public class SimilarResultsGroupingDelegateComparator extends SimilarResultsGroupingComparator {
    private final Comparator<VisualSearchResult>[] comparators;

    public SimilarResultsGroupingDelegateComparator(Comparator<VisualSearchResult>... comparators) {
        this.comparators = comparators;
    }

    @Override
    protected int doCompare(VisualSearchResult o1, VisualSearchResult o2) {
        int compare = 0;
        for (Comparator<VisualSearchResult> comparator : comparators) {
            compare = comparator.compare(o1, o2);
            if (compare != 0) {
                break;
            }
        }
        return compare;
    }
}
