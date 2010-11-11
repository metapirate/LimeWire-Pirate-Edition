package com.limegroup.gnutella.downloader;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.net.SocketsManager;
import org.limewire.net.TLSManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class DownloadWorkerFactoryImpl implements DownloadWorkerFactory {
    
    private final HTTPDownloaderFactory httpDownloaderFactory;
    private final ScheduledExecutorService backgroundExecutor;
    private final ScheduledExecutorService nioExecutor;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final SocketsManager socketsManager;
    private final DownloadStatsTracker statsTracker;
    private final TLSManager TLSManager;
    private final BandwidthCollector bandwidthCollector;

    @Inject
    public DownloadWorkerFactoryImpl(
            HTTPDownloaderFactory httpDownloaderFactory,
            @Named("backgroundExecutor")ScheduledExecutorService backgroundExecutor,
            @Named("nioExecutor")ScheduledExecutorService nioExecutor,
            Provider<PushDownloadManager> pushDownloadManager,
            SocketsManager socketsManager,
            DownloadStatsTracker statsTracker, TLSManager TLSManager, BandwidthCollector bandwidthCollector) {
        this.httpDownloaderFactory = httpDownloaderFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.nioExecutor = nioExecutor;
        this.pushDownloadManager = pushDownloadManager;
        this.socketsManager = socketsManager;
        this.statsTracker = statsTracker;
        this.TLSManager = TLSManager;
        this.bandwidthCollector = bandwidthCollector;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.DownloadWorkerFactory#create(com.limegroup.gnutella.downloader.ManagedDownloader, com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.downloader.VerifyingFile)
     */
    public DownloadWorker create(DownloadWorkerSupport manager,
            RemoteFileDescContext rfdContext, VerifyingFile vf) {
        return new DownloadWorker(manager, rfdContext, vf, httpDownloaderFactory,
                backgroundExecutor, nioExecutor, pushDownloadManager,
                socketsManager, statsTracker, TLSManager, bandwidthCollector);
    }

}
