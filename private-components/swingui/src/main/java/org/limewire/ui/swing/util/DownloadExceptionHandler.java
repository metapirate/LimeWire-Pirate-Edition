package org.limewire.ui.swing.util;


import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;

public interface DownloadExceptionHandler {
    /**
     * Handles the supplied DownloadException. Result could be to eat the
     * exception. To try downloading again using the supplied downloadAction, or
     * to popup a dialogue to try and save the download in a new location.
     */
    public void handleDownloadException(final DownloadAction downLoadAction,
            final DownloadException e, final boolean supportNewSaveDir);
}