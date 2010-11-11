package org.limewire.core.api.download;

import java.io.File;
import java.util.List;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;

public interface ResultDownloader {

    /**
     * Adds a download triggered by the given search results. The search results
     * must all be for the same item, otherwise an
     * {@link IllegalArgumentException} may be thrown.
     * 
     * @param search the search that triggered these results. This may be null.
     * @param coreSearchResults the results for the file that should be
     *        downloaded. A list is used to indicate that multiple sources can
     *        be swarmed from at the same time. The list is not intended to
     *        provide different downloads.
     * @param saveFile the location to save this file to.
     * @param overwrite whether or not to automatically overwrite any other files at the saveFileLocation
     */
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults, File saveFile, boolean overwrite)
        throws DownloadException;
    
    /**
     * Adds a download triggered by the given search results. The search results
     * must all be for the same item, otherwise an
     * {@link IllegalArgumentException} may be thrown.
     * 
     * @param search the search that triggered these results. This may be null.
     * @param coreSearchResults the results for the file that should be
     *        downloaded. A list is used to indicate that multiple sources can
     *        be swarmed from at the same time. The list is not intended to
     *        provide different downloads.
     */
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults)
            throws DownloadException;

    
    
}