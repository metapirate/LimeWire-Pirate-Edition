package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class MagnetHandlerImpl implements MagnetHandler {

    private final DownloadListManager downloadListManager;

    private final SearchHandler searchHandler;

    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;
    
    private static volatile int torrentDownloadMagnetCount = 0;
    
    private static volatile int gnutellaDownloadMagnetCount = 0;
    
    private static volatile int searchMagnetCount = 0;
    
    @Inject
    MagnetHandlerImpl(SearchHandler searchHandler,
            DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler) {
        this.downloadListManager = downloadListManager;
        this.searchHandler = searchHandler;
        this.downloadExceptionHandler = downloadExceptionHandler;
    }

    /**
     * Handles the given magnet file by either starting a search or starting to
     * download the file specified in the magnet.
     */
    public void handleMagnet(final MagnetLink magnet) {
        SwingUtils.invokeNowOrLater(new Runnable() {
            @Override
            public void run() {
                if(magnet.isGnutellaDownloadable()) {
                    downloadMagnet(magnet);
                } else if(magnet.isTorrentDownloadable()) {
                    downloadTorrent(magnet);
                } else if(magnet.isKeywordTopicOnly()) {
                    searchHandler.doSearch(DefaultSearchInfo.
                            createKeywordSearch(magnet.getQueryString(),
                                    SearchCategory.ALL));
                    searchMagnetCount++;
                } else {
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(),
                            I18n.tr("The magnet link is invalid."),
                            I18n.tr("Invalid Magnet Link"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

    }

    private void downloadTorrent(MagnetLink magnet) {
        try {
            downloadListManager.addTorrentDownload(magnet.getName(),
                    magnet.getURN(), magnet.getTrackerUrls());
            torrentDownloadMagnetCount++;
        } catch(DownloadException e) {
            downloadExceptionHandler.get().handleDownloadException(
                    new TorrentDownloadAction(magnet), e, true);
        }
    }
    
    private void downloadMagnet(MagnetLink magnet) {
        try {
            downloadListManager.addDownload(magnet, null, false);
            gnutellaDownloadMagnetCount++;
        } catch (DownloadException e) {
            downloadExceptionHandler.get().handleDownloadException(
                    new MagnetDownloadAction(magnet), e, true);
        }
    }
    
    private class TorrentDownloadAction implements DownloadAction {
        private final MagnetLink magnet; 
        
        TorrentDownloadAction(MagnetLink magnet) {
            this.magnet = magnet;
        }
        
        @Override
        public void download(File saveFile, boolean overwrite)
            throws DownloadException {
            
            downloadListManager.addTorrentDownload(magnet.getName(),
                    magnet.getURN(), magnet.getTrackerUrls());
            torrentDownloadMagnetCount++;
        }

        @Override
        public void downloadCanceled(DownloadException ignored) {
        }
    }

    private class MagnetDownloadAction implements DownloadAction {
        private final MagnetLink magnet;
        
        MagnetDownloadAction(MagnetLink magnet) {
            this.magnet = magnet;
        }
        
        @Override
        public void download(File saveFile, boolean overwrite)
            throws DownloadException {
            
            downloadListManager.addDownload(magnet, saveFile, overwrite);
            gnutellaDownloadMagnetCount++;
        }
 
        @Override
        public void downloadCanceled(DownloadException ignored) {}
    }
        
}
