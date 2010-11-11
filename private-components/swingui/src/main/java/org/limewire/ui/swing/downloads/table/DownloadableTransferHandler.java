package org.limewire.ui.swing.downloads.table;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.dnd.LocalFileTransferable;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.DownloadExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

class DownloadableTransferHandler extends TransferHandler {
    private final DownloadListManager downloadListManager;

    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;

    private final Provider<List<File>> selectedFiles;

    @Inject
    public DownloadableTransferHandler(DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler,
            @FinishedDownloadSelected Provider<List<File>> selectedFiles) {
        this.downloadListManager = downloadListManager;
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.selectedFiles = selectedFiles;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return false;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new LocalFileTransferable(selectedFiles.get().toArray(new File[0]));
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }
        Transferable t = info.getTransferable();
        final List<SearchResultTransferable> searchResultTransferableList;
        searchResultTransferableList = getTransferData(t);

        BackgroundExecutorService.execute(new Runnable() {
            public void run() {

                for (final SearchResultTransferable searchResultTransferable : searchResultTransferableList) {
                    try {
                        downloadListManager.addDownload(searchResultTransferable.getSearch(),
                                searchResultTransferable.getSearchResults());
                    } catch (DownloadException e) {
                        downloadExceptionHandler.get().handleDownloadException(
                                new DownloadAction() {
                                    @Override
                                    public void download(File saveFile, boolean overwrite)
                                            throws DownloadException {
                                        downloadListManager.addDownload(searchResultTransferable
                                                .getSearch(), searchResultTransferable
                                                .getSearchResults(), saveFile, overwrite);
                                    }

                                    @Override
                                    public void downloadCanceled(DownloadException ignored) {
                                        // do nothing
                                    }
                                }, e, true);
                    }
                }
            }
        });

        return true;
    }

    private List<SearchResultTransferable> getTransferData(Transferable t) {
        return null;
    }

    private static class SearchResultTransferable {

        public List<? extends SearchResult> getSearchResults() {
            return null;
        }

        public Search getSearch() {
            return null;
        }

    }

}
