package com.limegroup.gnutella.downloader;

import java.net.Socket;

import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;

/**
 * Default implementation of {@link HTTPDownloaderFactory}.
 * 
 * This will create {@link HTTPDownloader HTTPDownloaders} with all the correctly
 * injected fields.
 */
@Singleton
public class HTTPDownloaderFactoryImpl implements HTTPDownloaderFactory {

    private final NetworkManager networkManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final DownloadManager downloadManager;
    private final Provider<CreationTimeCache> creationTimeCache;
    private final BandwidthManager bandwidthManager;
    private final Provider<PushEndpointCache> pushEndpointCache;
    private final PushEndpointFactory pushEndpointFactory;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final ThexReaderFactory thexReaderFactory;
    private final TcpBandwidthStatistics tcpBandwidthStatistics;
    private final NetworkInstanceUtils networkInstanceUtils;


    @Inject
    public HTTPDownloaderFactoryImpl(NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory, DownloadManager downloadManager,
            Provider<CreationTimeCache> creationTimeCache, BandwidthManager bandwidthManager,
            Provider<PushEndpointCache> pushEndpointCache, PushEndpointFactory pushEndpointFactory,
            RemoteFileDescFactory remoteFileDescFactory, ThexReaderFactory thexReaderFactory,
            TcpBandwidthStatistics tcpBandwidthStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadManager = downloadManager;
        this.creationTimeCache = creationTimeCache;
        this.bandwidthManager = bandwidthManager;
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFactory = pushEndpointFactory;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.thexReaderFactory = thexReaderFactory;
        this.tcpBandwidthStatistics = tcpBandwidthStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.HTTPDownloaderFactory#create(java.net.Socket, com.limegroup.gnutella.RemoteFileDesc, com.limegroup.gnutella.downloader.VerifyingFile, boolean)
     */
    public HTTPDownloader create(Socket socket, RemoteFileDescContext rfd, VerifyingFile incompleteFile,
            boolean inNetwork) {
        return new HTTPDownloader(socket, rfd, incompleteFile, inNetwork, true, networkManager,
                alternateLocationFactory, downloadManager, creationTimeCache.get(),
                bandwidthManager, pushEndpointCache, pushEndpointFactory, remoteFileDescFactory,
                thexReaderFactory, tcpBandwidthStatistics, networkInstanceUtils);
    }


}
