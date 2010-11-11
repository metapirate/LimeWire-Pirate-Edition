package org.limewire.core.api.download;

import java.io.File;

/**
 * General interface for a download action.
 */
public interface DownloadAction {

    /**
     * Called to start the download.
     * 
     * @param saveFile Location to save the downloaded file.
     * @param overwrite Whether to overwrite or not if the file exists already.
     * @throws DownloadException If the save fails an exception is returned
     *         describing the problem.
     */
    void download(File saveFile, boolean overwrite) throws DownloadException;

    /**
     * Indicates that the download was canceled because of a
     * DownloadException, and that the SaveLocationHandler did not handle it.
     * 
     * @param e The last known DownloadException for this download.
     */
    void downloadCanceled(DownloadException e);
}