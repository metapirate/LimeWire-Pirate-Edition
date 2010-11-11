package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface ListViewRowHeightRule {
    enum RowDisplayConfig {
        HeadingOnly(36), HeadingAndSubheading(44), HeadingAndMetadata(44), HeadingSubHeadingAndMetadata(56);
        
        private final int height;
        RowDisplayConfig(int height) {
            this.height = height;
        }
        
        public int getRowHeight() {
            return height;
        }
    }
    
    /** Initializes this rule with a search. */
    void initializeWithSearch(String search);
    
    /**
     * Determines which combination of heading, subheading, and metadata should display
     * in the list view of the search results, given a specific VisualSearchResult.
     */
    RowDisplayResult getDisplayResult(VisualSearchResult vsr);
    
    public static interface RowDisplayResult {
        String getHeading();
        String getSubheading();
        PropertyMatch getMetadata();
        RowDisplayConfig getConfig();
        boolean isSpam();
        boolean isStale(VisualSearchResult vsr);
    }
    
    public static interface PropertyMatch {
        String getKey();
        String getHighlightedValue();
    }
}
