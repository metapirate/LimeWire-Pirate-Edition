package org.limewire.ui.swing.search.resultpanel;


public interface SearchResultTruncator {
    
    String truncateHeading(String headingText, int visibleWidthPixels, FontWidthResolver resolver);
    
    public static interface FontWidthResolver {
        int getPixelWidth(String text);
    }
}
