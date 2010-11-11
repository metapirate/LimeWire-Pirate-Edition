package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchDetails;

/** Contains information about how the search is being performed. */
public interface SearchInfo extends SearchDetails {

    /** What the title of the search is. */
    String getTitle();
    
}
