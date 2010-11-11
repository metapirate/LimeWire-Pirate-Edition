package org.limewire.ui.swing.search.resultpanel;

public interface SearchHeading {

    /**
     * Returns the text of the search, belonging in the heading display field.
     */
    String getText();
    
    /**
     * Returns the text of the search, with any modifications appropriate (for
     * truncation, e.g.) based on the content of the supplied adjoining fragment.
     */
    String getText(String adjoiningFragment);
}
