package org.limewire.core.impl.mozilla;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.core.api.mozilla.LimeMozillaDownloadManagerListener;
import org.limewire.core.api.mozilla.LimeMozillaDownloadProgressListener;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Objects;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;
import org.mozilla.xpcom.XPCOMException;

import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.mozilla.MozillaDownload;

/**
 * This class listens to a specific Mozilla download and tracks some statistics
 * for us.
 */
public class LimeMozillaDownloadProgressListenerImpl implements nsIDownloadProgressListener,
        MozillaDownload, LimeMozillaDownloadProgressListener {

    private static final Log LOG = LogFactory.getLog(LimeMozillaDownloadProgressListenerImpl.class);

    private final long downloadId;

    private final AtomicInteger state;

    private final AtomicLong totalProgress;

    private final SimpleBandwidthTracker down;

    private final File incompleteFile;

    private final AtomicLong contentLength;

    private final LimeMozillaDownloadManagerListener manager;

    private final EventListenerList<DownloadStateEvent> listeners;

    private final BlockingQueue<DownloadStateEvent> statusEvents;

    private final ScheduledExecutorService backgroundExecutor;

    private final XPComUtility xpComUtility;
    
    private CoreDownloader downloader;
    
    public LimeMozillaDownloadProgressListenerImpl(LimeMozillaDownloadManagerListener manager,
            ScheduledExecutorService backgroundExecutor, nsIDownload download, XPComUtility xpComUtility) {
        this.manager = Objects.nonNull(manager, "manager");
        this.backgroundExecutor = Objects.nonNull(backgroundExecutor, "backgroundExecutor");
        Objects.nonNull(download, "download");
        this.listeners = new EventListenerList<DownloadStateEvent>();
        this.downloadId = download.getId();
        this.state = new AtomicInteger();
        this.statusEvents = new LinkedBlockingQueue<DownloadStateEvent>();
        this.totalProgress = new AtomicLong();
        this.down = new SimpleBandwidthTracker();
        this.incompleteFile = new File(download.getTargetFile().getPath());
        this.contentLength = new AtomicLong(download.getSize());
        this.xpComUtility = xpComUtility;
    }

    @Override
    public synchronized void onDownloadStateChange(short state, nsIDownload download) {
        if (downloadId == download.getId()) {
            changeState(state);
        }
    }
    
    public void init(CoreDownloader coreDownloader, short state) {
        this.downloader = coreDownloader;
        changeState(state);
    }

    private synchronized void changeState(short state) {
        if (state != this.state.get()) {
            this.state.set(state);
            DownloadState downloadStatus = getDownloadStatus();
            DownloadStateEvent downloadStatusEvent = new DownloadStateEvent(downloader, downloadStatus);
            statusEvents.add(downloadStatusEvent);
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (listeners) {
                        DownloadStateEvent event = statusEvents.poll();
                        if (event != null) {
                            listeners.broadcast(event);
                        }
                    }
                }
            });
        }
    }

    @Override
    public synchronized void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
            long curSelfProgress, long maxSelfProgress, long curTotalProgress,
            long maxTotalProgress, nsIDownload download) {

        if (this.downloadId == download.getId()) {
            // this is my download
            int diff = (int) (curTotalProgress - totalProgress.longValue());
            down.count(diff);
            if (!isPaused()) {
                // this event might come in after pausing, so we don't want to
                // change the state in that event
                short state = download.getState();
                changeState(state);
            }
            totalProgress.set(curTotalProgress);
            contentLength.set(download.getSize());
        }
    }

    @Override
    public synchronized void onSecurityChange(nsIWebProgress webProgress, nsIRequest request,
            long state, nsIDownload download) {
        // don't care about this event.
    }

    @Override
    public synchronized void onStateChange(nsIWebProgress webProgress, nsIRequest request,
            long stateFlags, long status, nsIDownload download) {
        if (this.downloadId == download.getId()) {
            // this is my download
            changeState(download.getState());
            if (this.state.get() == nsIDownloadManager.DOWNLOAD_FINISHED) {
                this.contentLength.set(download.getSize());
                this.totalProgress.set(this.contentLength.get());
            }

        }

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

    public long getDownloadId() {
        return downloadId;
    }

    @Override
    public synchronized float getAverageBandwidth() {
        return down.getAverageBandwidth();
    }

    @Override
    public synchronized float getMeasuredBandwidth() {
        try {
            return down.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }
    }

    @Override
    public synchronized void measureBandwidth() {
        down.measureBandwidth();
    }

    @Override
    public synchronized boolean isCompleted() {
        return state.get() == nsIDownloadManager.DOWNLOAD_FINISHED;
    }

    @Override
    public synchronized boolean isPaused() {
        int myState = state.get();
        boolean paused = myState == nsIDownloadManager.DOWNLOAD_PAUSED;
        return paused;
    }

    @Override
    public synchronized DownloadState getDownloadStatus() {
        switch (state.get()) {
        case nsIDownloadManager.DOWNLOAD_SCANNING:
            return DownloadState.RESUMING;
        case nsIDownloadManager.DOWNLOAD_DOWNLOADING:
            return DownloadState.DOWNLOADING;
        case nsIDownloadManager.DOWNLOAD_FINISHED:
            return DownloadState.COMPLETE;
        case nsIDownloadManager.DOWNLOAD_QUEUED:
            return DownloadState.QUEUED;
        case nsIDownloadManager.DOWNLOAD_PAUSED:
            return DownloadState.PAUSED;
        case nsIDownloadManager.DOWNLOAD_NOTSTARTED:
            return DownloadState.PAUSED;
        case nsIDownloadManager.DOWNLOAD_CANCELED:
            return DownloadState.ABORTED;
        case nsIDownloadManager.DOWNLOAD_BLOCKED_PARENTAL:
            return DownloadState.INVALID;
        case nsIDownloadManager.DOWNLOAD_BLOCKED_POLICY:
            return DownloadState.INVALID;
        case nsIDownloadManager.DOWNLOAD_DIRTY:
            return DownloadState.INVALID;
        case nsIDownloadManager.DOWNLOAD_FAILED:
            return DownloadState.INVALID;
        }

        throw new IllegalStateException("unknown mozilla state");
    }

    @Override
    public File getIncompleteFile() {
        return incompleteFile;
    }

    @Override
    public synchronized boolean isInactive() {
        boolean inactive = state.get() != nsIDownloadManager.DOWNLOAD_DOWNLOADING && state.get() != nsIDownloadManager.DOWNLOAD_SCANNING;
        return inactive;
    }

    @Override
    public synchronized long getAmountDownloaded() {
        return totalProgress.get();
    }

    @Override
    public synchronized long getAmountPending() {
        return getContentLength() - getAmountDownloaded();
    }

    @Override
    public synchronized long getContentLength() {
        return contentLength.get();
    }

    @Override
    public void cancelDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                if (!LimeMozillaDownloadProgressListenerImpl.this.isCancelled()) {
                    try {
                        synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                            getDownloadManager().cancelDownload(downloadId);
                        }
                    } catch (XPCOMException e) {
                       	//TODO catching this exception because user in ui can click multiple times and mozilla code cannot handle that
                       	//should revisit and figure out a better way of ahndling this issue
                        LOG.debug(e.getMessage(), e);
                    }
                }
            }
        });

    }

    @Override
    public void pauseDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                    if (!LimeMozillaDownloadProgressListenerImpl.this.isPaused()) {
                        try {
                            getDownloadManager().pauseDownload(downloadId);
                        } catch (XPCOMException e) {
                          	//TODO catching this exception because user in ui can click multiple times and mozilla code cannot handle that
                        	//should revisit and figure out a better way of ahndling this issue
                            LOG.debug(e.getMessage(), e);
                        }
                        changeState(nsIDownloadManager.DOWNLOAD_PAUSED);
                    }
                }
            }
        });
    }

    @Override
    public void removeDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                    manager.removeListener(LimeMozillaDownloadProgressListenerImpl.this);
                    if (state.get() != nsIDownloadManager.DOWNLOAD_FINISHED) {
                        // overriding state to canceled so ti will be removed
                        // from the download list, gui code checks for aborted
                        // state.
                        changeState(nsIDownloadManager.DOWNLOAD_CANCELED);
                    }
                }
            }
        });
    }

    @Override
    public void resumeDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                    if (LimeMozillaDownloadProgressListenerImpl.this.isPaused()
                            || LimeMozillaDownloadProgressListenerImpl.this.isQueued()) {
                        try {
                            getDownloadManager().resumeDownload(downloadId);
                        } catch (XPCOMException e) {
                        	//TODO catching this exception because user in ui can click multiple times and mozilla code cannot handle that
                        	//should revisit and figure out a better way of ahndling this issue
                            LOG.debug(e.getMessage(), e);
                        }
                    }
                }
            }
        });
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = xpComUtility.getServiceProxy(
                "@mozilla.org/download-manager;1", nsIDownloadManager.class);
        return downloadManager;
    }

    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public synchronized boolean isQueued() {
        boolean queued = state.get() == nsIDownloadManager.DOWNLOAD_QUEUED;
        return queued;
    }

    @Override
    public synchronized boolean isCancelled() {
        DownloadState downloadStatus = getDownloadStatus();
        boolean cancelled = downloadStatus == DownloadState.ABORTED
                || downloadStatus == DownloadState.INVALID;
        return cancelled;
    }

    @Override
    public synchronized void setDiskError() {
        state.set(nsIDownloadManager.DOWNLOAD_DIRTY);
    }

}