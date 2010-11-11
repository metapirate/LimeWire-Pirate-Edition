package org.limewire.core.impl.support;

import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.support.SessionInfo;
import org.limewire.net.SocketsManager;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Inject;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.uploader.UploadSlotManager;

/** An implementation of SessionInfo that gets it's statistics from various LimeWire components. */
class LimeSessionInfo implements SessionInfo {
    
    private final NIODispatcher dispatcher;
    private final DownloadManager downloadManager;
    private final Statistics statistics;
    private final ConnectionManager connectionManager;
    private final CreationTimeCache creationTimeCache;
    private final DiskController diskController;
    private final SocketsManager socketsManager;
    private final ByteBufferCache byteBufferCache;
    private final UDPService udpService;
    private final Acceptor acceptor;
    private final DownloadServices downloadServices;
    private final UploadServices uploadServices;
    private final ConnectionCheckerManager connectionCheckerManager;
    private final NIODispatcher nioDispatcher;
    private final Library library;
    private final FileView gnutellaFileView;
    private final UploadSlotManager uploadSlotManager;
    private final ConnectionServices connectionServices;
    private final LifecycleManager lifecycleManager;
    private final RemoteLibraryManager remoteLibraryManager;
    private final NetworkManager networkManager;

    @Inject
    public LimeSessionInfo(NIODispatcher dispatcher, DownloadManager downloadManager,
                           Statistics statistics, ConnectionManager connectionManager,
                           CreationTimeCache creationTimeCache,
                           DiskController diskController, SocketsManager socketsManager,
                           ByteBufferCache byteBufferCache, UDPService udpService, Acceptor acceptor,
                           DownloadServices downloadServices, UploadServices uploadServices,
                           ConnectionCheckerManager connectionCheckerManager, NIODispatcher nioDispatcher,
                           Library library,
                           UploadSlotManager uploadSlotManager, ConnectionServices connectionServices,
                           LifecycleManager lifecycleManager, RemoteLibraryManager remoteLibraryManager,
                           @GnutellaFiles FileView gnutellaFileView,
                           NetworkManager networkManager) {
        this.dispatcher = dispatcher;
        this.downloadManager = downloadManager;
        this.statistics = statistics;
        this.connectionManager = connectionManager;
        this.creationTimeCache = creationTimeCache;
        this.diskController = diskController;
        this.socketsManager = socketsManager;
        this.byteBufferCache = byteBufferCache;
        this.udpService = udpService;
        this.acceptor = acceptor;
        this.downloadServices = downloadServices;
        this.uploadServices = uploadServices;
        this.connectionCheckerManager = connectionCheckerManager;
        this.nioDispatcher = nioDispatcher;
        this.library = library;
        this.uploadSlotManager = uploadSlotManager;
        this.connectionServices = connectionServices;
        this.lifecycleManager = lifecycleManager;
        this.remoteLibraryManager = remoteLibraryManager;
        this.gnutellaFileView = gnutellaFileView;
        this.networkManager = networkManager;
    }

    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumberOfPendingTimeouts()
     */
    public int getNumberOfPendingTimeouts() {
        return dispatcher.getNumPendingTimeouts();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumWaitingDownloads()
     */
    public int getNumWaitingDownloads() {
        return downloadManager.getNumWaitingDownloads();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumIndividualDownloaders()
     */
    public int getNumIndividualDownloaders() {
        return downloadManager.getNumIndividualDownloaders();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getCurrentUptime()
     */
    public long getCurrentUptime() {
        return statistics.getUptime();
    }
    
    @Override
    public long getStartTime() {
        return statistics.getStartTime();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumUltrapeerToLeafConnections()
     */
    public int getNumUltrapeerToLeafConnections() {
        return connectionManager.getNumInitializedClientConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumLeafToUltrapeerConnections()
     */
    public int getNumLeafToUltrapeerConnections() {
        return connectionManager.getNumClientSupernodeConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumUltrapeerToUltrapeerConnections()
     */
    public int getNumUltrapeerToUltrapeerConnections() {
        return connectionManager.getNumUltrapeerConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumOldConnections()
     */
    public int getNumOldConnections() {
        return connectionManager.getNumOldConnections();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getCreationCacheSize()
     */
    public long getCreationCacheSize() {
        return creationTimeCache.getSize();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getVerifyingFileByteCacheSize()
     */
    public long getDiskControllerByteCacheSize() {
        return diskController.getSizeOfByteCache();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getVerifyingFileVerifyingCacheSize()
     */
    public long getDiskControllerVerifyingCacheSize() {
        return diskController.getSizeOfVerifyingCache();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getVerifyingFileQueueSize()
     */
    public int getDiskControllerQueueSize() {
        return diskController.getNumPendingItems();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getByteBufferCacheSize()
     */
    public long getByteBufferCacheSize() {
        return byteBufferCache.getHeapCacheSize();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SessionInfo#getNumberOfWaitingSockets()
     */
    public int getNumberOfWaitingSockets() {
        return socketsManager.getNumWaitingSockets();
    }

    public boolean isGUESSCapable() {
        return networkManager.isGUESSCapable();
    }

    public boolean canReceiveSolicited() {
        return networkManager.canReceiveSolicited();
    }

    public boolean acceptedIncomingConnection() {
        return acceptor.acceptedIncoming();
    }
    
    public int getPort() {
        return acceptor.getPort(true);
    }

    @Override
    public boolean canDoFWT() {
        return networkManager.canDoFWT();
    }

    @Override
    public int getNumActiveDownloads() {
        return downloadServices.getNumActiveDownloads();
    }

    @Override
    public int getNumActiveUploads() {
        return uploadServices.getNumUploads();
    }

    @Override
    public int getNumConnectionCheckerWorkarounds() {
        return connectionCheckerManager.getNumWorkarounds();
    }

    @Override
    public int getNumQueuedUploads() {
        return uploadServices.getNumQueuedUploads();
    }

    @Override
    public long[] getSelectStats() {
        return nioDispatcher.getSelectStats();
    }

    @Override
    public int getSharedFileListSize() {
        return gnutellaFileView.size();
    }

    @Override
    public int getManagedFileListSize() {
        return library.size();
    }

    @Override
    public int getAllFriendsFileListSize() {
        return remoteLibraryManager.getAllFriendsLibrary().size();
    }

    @Override
    public String getUploadSlotManagerInfo() {
        return uploadSlotManager.toString();
    }

    @Override
    public boolean isConnected() {
        return connectionServices.isConnected();
    }

    @Override
    public boolean isLifecycleLoaded() {
        return lifecycleManager.isLoaded();
    }

    @Override
    public boolean isShieldedLeaf() {
        return connectionServices.isShieldedLeaf();
    }

    @Override
    public boolean isSupernode() {
        return connectionServices.isSupernode();
    }

    @Override
    public boolean isUdpPortStable() {
        return udpService.portStable();
    }

    @Override
    public int lastReportedUdpPort() {
        return udpService.lastReportedPort();
    }

    @Override
    public int receivedIpPong() {
        return udpService.receivedIpPong();
    }
}
