package org.limewire.ui.swing.search.resultpanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

class DownloadHandlerImpl implements DownloadHandler {
    
    private final SearchResultsModel searchResultsModel;
    private List<DownloadPreprocessor> downloadPreprocessors = new ArrayList<DownloadPreprocessor>();
    private LibraryMediator libraryMediator;
    private final DownloadMediator downloadMediator;
    
    /**
     * Starts all downloads from searches.  Navigates to Library or Downloads without downloading if the file is in either of those locations.
     */
    public DownloadHandlerImpl(SearchResultsModel searchResultsModel,
            LibraryMediator libraryMediator, DownloadMediator downloadMediator) {
        this.searchResultsModel = searchResultsModel;
        this.libraryMediator = libraryMediator;
        this.downloadMediator = downloadMediator;

        this.downloadPreprocessors.add(new LicenseWarningDownloadPreprocessor());
    }


    @Override
    public void download(final VisualSearchResult vsr) {
        download(vsr, null);
    }

    /**
     * Downloads the file specified in vsr if it is not already downloading or in Library.  Navigates to Downloads or Library if it is already in the locations.
     */
    @Override
    public void download(final VisualSearchResult vsr, File saveFile) {
        if (maybeNavigate(vsr)){
            //do not download if we navigate away
            return;
        }

        // execute the download preprocessors
        for (DownloadPreprocessor preprocessor : downloadPreprocessors) {
            boolean shouldDownload = preprocessor.execute(vsr);
            if (!shouldDownload) {
                // do not download!
                return;
            }
        }

        // Start download.
        searchResultsModel.download(vsr, saveFile);
    }
    
   private boolean maybeNavigate(VisualSearchResult vsr) {
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
        case REMOVED:
            downloadMediator.selectAndScrollTo(vsr.getUrn());
            return true;
        case DOWNLOADED:
        case LIBRARY:
            libraryMediator.selectInLibrary(vsr.getUrn());
            return true;
        default:
            return false;
        }
    }

}
