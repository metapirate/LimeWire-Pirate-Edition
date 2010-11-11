package org.limewire.core.impl.download;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;

public interface DownloadItemFactory {
    /**
     * @return null if it can't craete a download item for this type of search
     * and search results.
     */
    DownloadItem create(Search search, List<? extends SearchResult> searchResults,
            File saveFile, boolean overwrite) throws DownloadException;
}
