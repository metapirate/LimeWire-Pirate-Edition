package org.limewire.ui.swing.search.resultpanel;

import java.io.File;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * Defines an interface for handling download requests.
 */
public interface DownloadHandler {
    
    /**
     * Initiates a download of the specified visual search result.
     */
    void download(VisualSearchResult vsr);
    
    /**
     * Initiates a download of the specified visual search result to the
     * specified save file.
     */
    void download(VisualSearchResult vsr, File saveFile);

}
