package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

import org.limewire.core.api.download.DownloadState;

/**
 * A listener to forward DownloadItem property change events to its associated 
 * visual search result.
 */
class DownloadItemPropertyListener implements PropertyChangeListener {
    
    /** Reference to visual search result.  We use a WeakReference to allow
     *  the result to be cleared if the search is closed. 
     */
    private final WeakReference<VisualSearchResult> vsrReference;

    /**
     * Constructs a DownloadItemPropertyListener for the specified visual
     * search result.
     */
    public DownloadItemPropertyListener(VisualSearchResult vsr) {
        this.vsrReference = new WeakReference<VisualSearchResult>(vsr);
    }

    /**
     * Handles a property change event to update the visual search result. 
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            DownloadState state = (DownloadState) evt.getNewValue();
            BasicDownloadState bstate = BasicDownloadState.fromState(state);
            if(bstate != null) {
                setDownloadState(bstate);
            }
        }
    }

    /**
     * Sets the download state in the visual search result.
     */
    private void setDownloadState(BasicDownloadState downloadState) {
        VisualSearchResult vsr = vsrReference.get();
        if (vsr != null) {
            vsr.setDownloadState(downloadState);
        }
    }
}
