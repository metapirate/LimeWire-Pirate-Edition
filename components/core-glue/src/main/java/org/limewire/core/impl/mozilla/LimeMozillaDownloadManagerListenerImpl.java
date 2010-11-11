package org.limewire.core.impl.mozilla;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.core.api.mozilla.LimeMozillaDownloadProgressListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Objects;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;
import org.mozilla.xpcom.XPCOMException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.downloader.CoreDownloader;

/**
 * Provides a means of tracking what downloads have listeners already, and
 * adding listeners for those downloads which need them.
 */
@Singleton
class LimeMozillaDownloadManagerListenerImpl implements
        org.limewire.core.api.mozilla.LimeMozillaDownloadManagerListener,
        nsIDownloadProgressListener {

    private static final Log LOG = LogFactory.getLog(LimeMozillaDownloadManagerListenerImpl.class);

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private final Map<Long, LimeMozillaDownloadProgressListener> listeners;

    private final DownloadManager downloadManager;

    private final ScheduledExecutorService backgroundExecutor;

    private final XPComUtility xpComUtility;
    
    @Inject
    public LimeMozillaDownloadManagerListenerImpl(
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            DownloadManager downloadManager,
            XPComUtility xpComUtility) {
        this.backgroundExecutor = Objects.nonNull(backgroundExecutor, "backgroundExecutor");
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.xpComUtility = Objects.nonNull(xpComUtility, "xpComUtility");
        this.listeners = new HashMap<Long, LimeMozillaDownloadProgressListener>();
    }

    public synchronized void resumeDownloads() {
        nsIDownloadManager downloadManager = getDownloadManager();
        nsISimpleEnumerator enumerator = downloadManager.getActiveDownloads();
        while (enumerator.hasMoreElements()) {
            nsISupports elem = enumerator.getNext();
            
            nsIDownload download = xpComUtility.proxy(elem, nsIDownload.class);
            long downloadId = download.getId();
            try {
                downloadManager.resumeDownload(downloadId);
            } catch (XPCOMException e) {
                // can see the downloads in the ui and reusme them from there.
                // When first starting we don't know what downloads have already
                // started, so eat this exception here.
            }
        }
    }

    public synchronized void addMissingDownloads() {
        nsIDownloadManager downloadManager = getDownloadManager();
        nsISimpleEnumerator enumerator = downloadManager.getActiveDownloads();
        while (enumerator.hasMoreElements()) {
            nsISupports elem = enumerator.getNext();
            nsIDownload download = xpComUtility.proxy(elem, nsIDownload.class);
            addListener(download, nsIDownloadManager.DOWNLOAD_QUEUED);
        }
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = xpComUtility.getServiceProxy(NS_IDOWNLOADMANAGER_CID,
                nsIDownloadManager.class);
        return downloadManager;
    }

    @Override
    public void onDownloadStateChange(short state, nsIDownload download) {
        addMissingDownloads();
    }

    private synchronized boolean addListener(nsIDownload download, short state) {
        long downloadId = download.getId();
        LimeMozillaDownloadProgressListener listener = listeners.get(downloadId);
        if (listener == null) {
            LimeMozillaDownloadProgressListenerImpl listenerImpl = new LimeMozillaDownloadProgressListenerImpl(
                    this, backgroundExecutor, download, xpComUtility);
            listeners.put(download.getId(), listenerImpl);
            CoreDownloader downloader = (CoreDownloader)downloadManager.downloadFromMozilla(listenerImpl);
            listenerImpl.init(downloader, state);
            getDownloadManager().addListener(listenerImpl);
            return true;
        }
        return false;
    }

    @Override
    public void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
            long curSelfProgress, long maxSelfProgress, long curTotalProgress,
            long maxTotalProgress, nsIDownload download) {
        // nothing to do for this listener
    }

    @Override
    public void onSecurityChange(nsIWebProgress webProgress, nsIRequest request, long state,
            nsIDownload download) {
        // don't care about this event.
    }

    @Override
    public void onStateChange(nsIWebProgress webProgress, nsIRequest request, long stateFlags,
            long status, nsIDownload download) {
        addMissingDownloads();
    }

    @Override
    public void setDocument(nsIDOMDocument document) {
        // no mozilla window to use
    }

    @Override
    public nsIDOMDocument getDocument() {
        // no mozilla window to use
        return null;
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }

    @Override
    public synchronized void removeListener(
            final LimeMozillaDownloadProgressListener limeMozillaDownloadProgressListener) {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                nsIDownloadManager downloadManager = getDownloadManager();
                try {
                    long downloadId = limeMozillaDownloadProgressListener.getDownloadId();
                    downloadManager.removeDownload(downloadId);
                } catch (XPCOMException e) {
                    LOG.debug(e.getMessage(), e);
                }
                listeners.remove(limeMozillaDownloadProgressListener);
            }
        });
    }

}