package org.limewire.core.impl;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.core.impl.download.DownloadListenerList;
import org.limewire.core.impl.magnet.MagnetLinkImpl;
import org.limewire.core.impl.monitor.IncomingSearchListener;
import org.limewire.core.impl.monitor.IncomingSearchListenerList;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.upload.UploadListener;
import org.limewire.core.impl.upload.UploadListenerList;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * An implementation of the UI callback to handle notifications about 
 * asynchronous backend events.
 */
@Singleton
class GlueActivityCallback implements ActivityCallback, QueryReplyListenerList,
        DownloadListenerList, UploadListenerList, 
        IncomingSearchListenerList, GuiCallbackService {

    private final SortedMap<byte[], List<QueryReplyListener>> queryReplyListeners;

    private final List<DownloadListener> downloadListeners = new CopyOnWriteArrayList<DownloadListener>();
    
    private final List<UploadListener> uploadListeners = new CopyOnWriteArrayList<UploadListener>();

    private final List<IncomingSearchListener> monitorListeners = new CopyOnWriteArrayList<IncomingSearchListener>();

    private final DownloadManager downloadManager;
    
    private GuiCallback guiCallback = null;
    
    @Inject
    public GlueActivityCallback(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        queryReplyListeners = new ConcurrentSkipListMap<byte[], List<QueryReplyListener>>(
                GUID.GUID_BYTE_COMPARATOR);
    }

    @Override
    public void addQueryReplyListener(byte[] guid, QueryReplyListener listener) {
        synchronized (queryReplyListeners) {
            List<QueryReplyListener> listeners = queryReplyListeners.get(guid);
            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<QueryReplyListener>();
                queryReplyListeners.put(guid, listeners);
            }
            listeners.add(listener);
        }
    }

    @Override
    public void removeQueryReplyListener(byte[] guid, QueryReplyListener listener) {
        synchronized (queryReplyListeners) {
            List<QueryReplyListener> listeners = queryReplyListeners.get(guid);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    queryReplyListeners.remove(guid);
                }
            }
        }
    }

    @Override
    public void addDownloadListener(DownloadListener listener) {
        downloadListeners.add(listener);
    }

    @Override
    public void removeDownloadListener(DownloadListener listener) {
        downloadListeners.remove(listener);
    }

    @Override
    public void handleMagnets(MagnetOptions[] magnets) {
        if(guiCallback != null) {
            for(MagnetOptions magnetOption : magnets) {
                guiCallback.handleMagnet(new MagnetLinkImpl(magnetOption));
            }
        }
    }

    @Override
    public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        List<QueryReplyListener> listeners = queryReplyListeners.get(queryReply.getGUID());
        if (listeners != null) {
            for (QueryReplyListener listener : listeners) {
                listener.handleQueryReply(rfd, queryReply, locs);
            }
        }
    }

    // TODO: address and port - are ignored
    @Override
    public void handleQuery(QueryRequest query, String address, int port) {
        for (IncomingSearchListener listener : monitorListeners) {
            listener.handleQueryString(query.getQuery());
        }
    }

    public void handleSharedFileUpdate(File file) {
        //TODO PJV this looks like it is supposed to update the gui with the meta data changes about how many hits this file received in searches and how many times it has been uploaded.
        //Can we get rid of this now and use a property listener when we need this data again? could fireProperty events when incrementHitcount etc. are called
    }

    @Override
    public void handleTorrent(final File torrentFile) {
        if(torrentFile != null && torrentFile.exists() && torrentFile.length() > 0) {
            try {
                downloadManager.downloadTorrent(torrentFile, null, false);
            } catch (DownloadException e) {
                handleDownloadException(new DownloadAction() {
                  @Override
                    public void download(File saveDirectory, boolean overwrite) throws DownloadException {
                          downloadManager.downloadTorrent(torrentFile, saveDirectory, overwrite);
                    }

                @Override
                public void downloadCanceled(DownloadException ignored) {
                    //nothing to do
                }
                
                },e,false);
            }
        }
    }

    @Override
    public void installationCorrupted() {
        MessageService.showError(I18nMarker.marktr("<html><b>Your LimeWire may have been corrupted by a virus or trojan!</b></html>"));
    }

    @Override
    public boolean isQueryAlive(GUID guid) {
        return queryReplyListeners.containsKey(guid.bytes());
    }

    @Override
    public void addUpload(Uploader u) {
        for (UploadListener listener : uploadListeners) {
            listener.uploadAdded(u);
        }
    }
    
    @Override
    public void uploadComplete(Uploader u) {
        for (UploadListener listener : uploadListeners) {
            listener.uploadComplete(u);
        }
    }

    @Override
    public void restoreApplication() {
        if(guiCallback != null) {
            guiCallback.restoreApplication();
        }
    }

    @Override
    public String translate(String s) {
        if(guiCallback != null) {
            return guiCallback.translate(s);
        }
        
        return s;
    }

    @Override
    public void uploadsComplete() {
        for (UploadListener listener : uploadListeners) {
            listener.uploadsCompleted();
        }
    }

    @Override
    public void addDownload(Downloader d) {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadAdded(d);
        }
    }
    
    @Override
    public void downloadsComplete() {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadsCompleted();
        }
    }
    
    @Override
    public void promptAboutUnscannedPreview(Downloader dloader) {
        if(guiCallback == null) {
            dloader.discardUnscannedPreview(true);
        } else {
            // FIXME: replace this with a proper warning message
            String msg = I18nMarker.marktr("The file could not be scanned for viruses. Preview anyway?");
            boolean previewAnyway = guiCallback.promptUserQuestion(msg);
            dloader.discardUnscannedPreview(!previewAnyway);
        }
    }
    
    @Override
    public void removeDownload(Downloader d) {
        for (DownloadListener listener : downloadListeners) {
            listener.downloadRemoved(d);
        }
    }
    
    public void setGuiCallback(GuiCallback guiCallback) {
        this.guiCallback = guiCallback;
    }

    @Override
    public void handleDownloadException(DownloadAction downLoadAction,
            DownloadException e, boolean supportsNewSaveDir) {
        if(guiCallback != null) {
            guiCallback.handleDownloadException(downLoadAction, e, supportsNewSaveDir);
        } else {
            ErrorService.error(e, "Error handling DownloadException. GuiCallBack not yet initialized.");
        }
    }

    @Override
    public void addUploadListener(UploadListener listener) {
        uploadListeners.add(listener);
    }

    @Override
    public void removeUploadListener(UploadListener listener) {
        uploadListeners.remove(listener);
    }

    @Override
    public void addIncomingSearchListener(IncomingSearchListener listener) {
        monitorListeners.add(listener);
    }

    @Override
    public void removeIncomingSearchListener(IncomingSearchListener listener) {
        monitorListeners.remove(listener);
    }

    @Override
    public boolean promptTorrentFilePriorities(Torrent torrent) {
        if(guiCallback != null && torrent.hasMetaData()) {
            return guiCallback.promptTorrentFilePriorities(torrent);
        }
        return true;
    }

    /**
     * Asks the user whether to continue with a torrent download that contains
     * files with banned extensions.
     * @return true if the download should continue.
     */
    @Override
    public boolean promptAboutTorrentWithBannedExtensions(Torrent torrent, Set<String> bannedExtensions) {
        if(guiCallback != null)
            return guiCallback.promptAboutTorrentWithBannedExtensions(torrent, bannedExtensions);
        return true;
    }
    
    /**
     * Asks the user whether to continue with a torrent if scanning the torrent file failed.
     * @return true if the download should continue.
     */
    @Override
    public boolean promptAboutTorrentDownloadWithFailedScan() {
        if (guiCallback != null)
            return guiCallback.promptAboutTorrentDownloadWithFailedScan();
        return true;
    }
}
