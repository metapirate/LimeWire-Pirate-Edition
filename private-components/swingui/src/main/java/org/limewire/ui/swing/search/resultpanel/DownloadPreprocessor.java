package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * Do things prior to downloading search result.  Can indicate whether or not download proceeds.
 */
interface DownloadPreprocessor {

    /**
     * Does processing prior to actually performing the download
     *
     * @param vsr VisualSearchResult to process
     * @return true if download should continue, false if not
     */
    public boolean execute(VisualSearchResult vsr);
}
