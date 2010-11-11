package org.limewire.ui.swing.search.model;

import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;

/**
 * A listener to handle updates to the list of visual search results.  As each
 * VisualSearchResult is received, this listener sets its download state if 
 * the result is already in the library, or currently being downloaded.
 */
class AlreadyDownloadedListEventListener implements VisualSearchResultStatusListener {

    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    /**
     * Constructs an AlreadyDownloadedListEventListener with the specified
     * library manager and download list manager.
     */
    public AlreadyDownloadedListEventListener(LibraryManager libraryManager,
            DownloadListManager downloadListManager) {
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
    }
    
    @Override
    public void resultChanged(VisualSearchResult vsr, String propertyName, Object oldValue, Object newValue) {
    }
    
    @Override
    public void resultsCleared() {
    }
    
    @Override
    public void resultCreated(VisualSearchResult visualSearchResult) {
        // Get list of library files, and list of search results. 
        LibraryFileList libraryFileList = libraryManager.getLibraryManagedList();
        
        //TODO should probably check more than just URN, can check the file save path as well.
        URN urn = visualSearchResult.getUrn();
        if (libraryFileList.contains(urn)) {
            // Set download state when result is already in library.
            visualSearchResult.setDownloadState(BasicDownloadState.LIBRARY);            
        } else {
            // Set download state when result is being downloaded.
            DownloadItem downloadItem = downloadListManager.getDownloadItem(urn);
            if(downloadItem != null) {
                downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(visualSearchResult));
                visualSearchResult.setPreExistingDownload(true);
                BasicDownloadState bstate = BasicDownloadState.fromState(downloadItem.getState());
                if(bstate != null) {
                    visualSearchResult.setDownloadState(bstate);
                }
            }
        }
    }
}
