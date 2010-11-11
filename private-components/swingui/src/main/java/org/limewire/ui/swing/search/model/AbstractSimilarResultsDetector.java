package org.limewire.ui.swing.search.model;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

public abstract class AbstractSimilarResultsDetector implements SimilarResultsDetector {

    private static final Log LOG = LogFactory.getLog(AbstractSimilarResultsDetector.class);

    /**
     * Finds the parent correlating the two searchResults and then moves these
     * search results under that parent. Then updates the visibility of these
     * items based on their parents visibilities.
     * 
     * Returns the parent chosen for these search results.
     */
    protected VisualSearchResult update(VisualSearchResult o1, VisualSearchResult o2) {

        VisualSearchResult parent = findParent(o1, o2);

        boolean childrenVisible = o1.isChildrenVisible() || o2.isChildrenVisible()
                || parent.isChildrenVisible() || o1.getSimilarityParent() != null
                && o1.getSimilarityParent().isChildrenVisible() || o2.getSimilarityParent() != null
                && o2.getSimilarityParent().isChildrenVisible()
                || parent.getSimilarityParent() != null
                && parent.getSimilarityParent().isChildrenVisible();

        updateParent(o1.getSimilarityParent(), parent);
        updateParent(o1, parent);
        updateParent(o2.getSimilarityParent(), parent);
        updateParent(o2, parent);

        updateVisibility(parent, childrenVisible);

        return parent;
    }

    /**
     * Update visibilities of newly changed parents.
     */
    private void updateVisibility(VisualSearchResult parent, final boolean childrenVisible) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("Setting child visibility for {0} to {1}", parent.getCoreSearchResults().get(0).getUrn(), childrenVisible);
        }
        parent.setVisible(true);
        parent.setChildrenVisible(childrenVisible);
    }

    /**
     * Updates the child to use the given parent. The parent is set, the
     * children are moved, and the visibility is copied. Also the given child is
     * checked to see if it already has a parent, if so its parent is also
     * updated to be a child of the given parent.
     */
    private void updateParent(VisualSearchResult child, VisualSearchResult parent) {
        parent.setSimilarityParent(null);
        if (child != null && child != parent) {
            child.setSimilarityParent(parent);
            parent.addSimilarSearchResult(child);
            moveChildren(child, parent);
        }
    }

    /**
     * Moves the children from the child to the parent.
     */
    private void moveChildren(VisualSearchResult child, VisualSearchResult parent) {
        child.removeSimilarSearchResult(parent);
        for (VisualSearchResult item : child.getSimilarResults()) {
            updateParent(item, parent);
            child.removeSimilarSearchResult(item);
            if(parent != item) {
                parent.addSimilarSearchResult(item);
            }
        }
    }

    /**
     * Returns which item should be the parent between the two similar search
     * results. The item with the most sources should be the parent as it's
     * likely to provide the fastest download.
     */
    private VisualSearchResult findParent(VisualSearchResult o1, VisualSearchResult o2) {

        VisualSearchResult parent1 = o1.getSimilarityParent();
        VisualSearchResult parent2 = o2.getSimilarityParent();
        int o1Count = o1.getSources().size();
        int o2Count = o2.getSources().size();
        int parent1Count = parent1 == null ? 0 : parent1.getSources().size();
        int parent2Count = parent2 == null ? 0 : parent2.getSources().size();

        // Parents should not have parents of their own 
        assert(parent1 == null || parent1.getSimilarityParent() == null);
        assert(parent2 == null || parent2.getSimilarityParent() == null);

        // Find the result with the highest count - if we're in the process of
        // regrouping results, a child may have a higher count than its parent
        if(parent2Count > parent1Count && parent2Count > o2Count &&
                parent2Count > o1Count) {
            return parent2;
        } else if(parent1Count > o2Count && parent1Count > o1Count) {
            return parent1;
        } else if(o2Count > o1Count) {
            return o2;
        } else if(o1Count > o2Count) {
            return o1;
        }        
        // All the results have equal counts; keep the current parent (if any)
        if(parent1 != null) {
            return parent1;
        } else if(parent2 != null) {
            return parent2;
        } else {
            return o1; // Doesn't matter whether we return o1 or o2
        }
    }

}