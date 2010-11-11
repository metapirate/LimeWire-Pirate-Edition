package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.util.TorrentUtil;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.malware.VirusEngine;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPortImpl;
import org.limewire.libtorrent.LibTorrentParams;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.LibTorrentBTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.LibTorrentBTDownloadMementoImpl;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.malware.VirusScanException;
import com.limegroup.gnutella.malware.VirusScanner;

/**
 * Wraps the Torrent class in the Downloader interface to enable the gui to
 * treat the torrent downloader as a normal downloader.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader implements BTDownloader,
        EventListener<TorrentEvent> {

    private static final Log LOG = LogFactory.getLog(BTDownloaderImpl.class);

    private static final AtomicInteger torrentsStarted = new AtomicInteger();

    private static final AtomicInteger torrentsFinished = new AtomicInteger();

    private final DownloadManager downloadManager;
    
    private volatile Torrent torrent;
    private final BTUploaderFactory btUploaderFactory;
    private final AtomicBoolean finishing = new AtomicBoolean(false);
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Library library;
    private final EventMulticaster<DownloadStateEvent> listeners;
    private final AtomicReference<DownloadState> lastState = new AtomicReference<DownloadState>(
            DownloadState.QUEUED);
    private final FileCollection gnutellaFileCollection;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<TorrentUploadManager> torrentUploadManager;
    private final Provider<DangerousFileChecker> dangerousFileChecker;
    private final Provider<VirusScanner> virusScanner;
    private final Provider<DownloadCallback> downloadCallback;

    /**
     * Whether a preview that could not be scanned for viruses should be
     * deleted.
     */
    private volatile boolean discardUnscannedPreview;

    /**
     * Torrent info hash based URN used as a cache for getSha1Urn().
     */
    private volatile URN urn = null;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            BTUploaderFactory btUploaderFactory, Library library, 
            @Named("fastExecutor") ScheduledExecutorService fastExecutor,
            @GnutellaFiles FileCollection gnutellaFileCollection,
            Provider<TorrentManager> torrentManager,
            Provider<TorrentUploadManager> torrentUploadManager,
            Provider<DangerousFileChecker> dangerousFileChecker,
            Provider<VirusScanner> virusScanner,
            Provider<DownloadCallback> downloadCallback,
            CategoryManager categoryManager) {
        super(saveLocationManager, categoryManager);
        this.downloadManager = downloadManager;
        this.btUploaderFactory = btUploaderFactory;
        this.library = library;
        this.gnutellaFileCollection = gnutellaFileCollection;
        this.listeners = new AsynchronousMulticasterImpl<DownloadStateEvent>(fastExecutor);
        this.torrentManager = torrentManager;
        this.torrentUploadManager = torrentUploadManager;
        this.dangerousFileChecker = dangerousFileChecker;
        this.virusScanner = virusScanner;
        this.downloadCallback = downloadCallback;
        discardUnscannedPreview = true;
    }

    @Override
    public void handleEvent(TorrentEvent event) {
        if (TorrentEventType.COMPLETED == event.getType() && !complete.get()) {
            LOG.debug("Finished");
            finishing.set(true);
            torrentsFinished.incrementAndGet();
            if (isInfectedOrDangerous()) {
                return;
            }
            FileUtils.forceDeleteRecursive(getSaveFile());
            File completeDir = getSaveFile().getParentFile();
            torrent.getLock().lock();
            try {
                torrent.moveTorrent(completeDir);

                File torrentUploadFolder = BittorrentSettings.TORRENT_UPLOADS_FOLDER.get();

                File oldTorrentFile = torrent.getTorrentFile();
                if (oldTorrentFile != null) {
                    File newTorrentFile = new File(torrentUploadFolder, oldTorrentFile.getName());
                    torrent.setTorrentFile(newTorrentFile);
                    FileUtils.copy(oldTorrentFile, newTorrentFile);
                    FileUtils.forceDelete(oldTorrentFile);
                }

                File oldFastResumeFile = torrent.getFastResumeFile();
                if (oldFastResumeFile != null) {
                    File newFastResumeFile = new File(torrentUploadFolder, oldFastResumeFile.getName());
                    torrent.setFastResumeFile(newFastResumeFile);
                    FileUtils.copy(oldFastResumeFile, newFastResumeFile);
                    FileUtils.forceDelete(oldFastResumeFile);
                }
            } finally {
                torrent.getLock().unlock();
            }
            createUploadMemento();
            cleanupPriorityZeroFiles();
            File completeFile = getSaveFile();
            addFileToCollections(completeFile);
            complete.set(true);
            deleteIncompleteFiles();
            if(lastState.get() != DownloadState.SCAN_FAILED) {
                lastState.set(DownloadState.COMPLETE);
                listeners.broadcast(new DownloadStateEvent(this, DownloadState.COMPLETE));
            }
            BTDownloaderImpl.this.downloadManager.remove(BTDownloaderImpl.this, true);
            torrent.removeListener(BTDownloaderImpl.this);
        } else if (TorrentEventType.STOPPED == event.getType()) {
            torrent.removeListener(this);
            // Was the torrent stopped because of a virus or dangerous file?
            if (lastState.get() != DownloadState.DANGEROUS &&
                    lastState.get() != DownloadState.THREAT_FOUND) {
                lastState.set(DownloadState.ABORTED);
                listeners.broadcast(new DownloadStateEvent(this, DownloadState.ABORTED));
            }
            BTDownloaderImpl.this.downloadManager.remove(BTDownloaderImpl.this, true);
            deleteIncompleteFiles();
        } else if (TorrentEventType.FAST_RESUME_FILE_SAVED == event.getType()) {
            // nothing to do now.
        } else if (TorrentEventType.STARTED == event.getType()) {
            torrentsStarted.incrementAndGet();
        } else if (TorrentEventType.META_DATA_RECEIVED == event.getType() && getTorrentFile() == null) {
            // Hack to either cancel the torrent incase of collision or
            //  fix the save path after metadata is received in a torrent 
            //  file-less download
            if (getSaveFile().exists()) {
                stop();
            } else {
                String newName = event.getTorrent().getName();
                setSaveFileInternal(new File(getSaveFile().getParentFile(), newName));
                setDefaultFileName(newName);
            }
        }
        else {
            DownloadState currentState = getState();
            if (lastState.getAndSet(currentState) != currentState) {
                listeners.broadcast(new DownloadStateEvent(this, currentState));
            }
        }
    }

    private void createUploadMemento() {
        try {
            torrentUploadManager.get().writeMemento(torrent);
            torrent.setAutoManaged(true);
        } catch (IOException e) {
            LOG.error("Error saving torrent upload menento for torrent: " + torrent.getName(), e);
            // non-fatal, upload will just not be loaded on application
            // restart
        }
    }

    /**
     * Returns true if there are any infected or dangerous files in this
     * torrent, after stopping the download.
     */
    private boolean isInfectedOrDangerous() {
        if(virusScanner.get().isEnabled()) {
            lastState.set(DownloadState.SCANNING);
            listeners.broadcast(new DownloadStateEvent(this, DownloadState.SCANNING));
            try {
                if(isInfected(getIncompleteFile()))
                    return true;
            } catch(VirusScanException e) {
                LOG.error("Error scanning file", e);
                setAttribute(VirusEngine.DOWNLOAD_FAILURE_HINT, e.getDetail(), false);
                lastState.set(DownloadState.SCAN_FAILED);
                listeners.broadcast(new DownloadStateEvent(this, lastState.get()));
            }
        }
        for(File f : getIncompleteFiles()) {
            if(isDangerous(f))
                return true;
        }
        return false;
    }

    /**
     * Checks whether a file fragment is infected or dangerous. If the virus
     * scan fails, the user will be asked whether to preview the file anyway.
     * @param fragment the file to check
     * @param listener a listener to be informed of virus scan progress
     * @return true if the file cannot be previewed.
     */
    private boolean isInfectedOrDangerous(File fragment, ScanListener listener) {
        if(virusScanner.get().isEnabled()) {
            LOG.debug("Starting preview scan");
            listener.scanStarted();
            try {
                boolean infected = isInfected(fragment);
                listener.scanStopped();
                if(infected)
                    return true;                
            } catch(VirusScanException e) {
                LOG.error("Error scanning file", e);
                listener.scanStopped();
                if(promptAboutUnscannedPreview()) {
                    // The user chose to cancel the preview
                    LOG.debug("User chose to cancel preview");
                    return true;
                }
                LOG.debug("User chose to continue with preview");
            }
        }
        return isDangerous(fragment);
    }

    /**
     * Returns true if the given file is infected, after stopping the download.
     */
    private boolean isInfected(File file) throws VirusScanException {
        if(LOG.isDebugEnabled())
            LOG.debug("Scanning " + file);
        if(virusScanner.get().isInfected(file)) {
            if(LOG.isDebugEnabled())
                LOG.debug(file + " is infected");
            lastState.set(DownloadState.THREAT_FOUND);
            listeners.broadcast(new DownloadStateEvent(this, DownloadState.THREAT_FOUND));
            // This will cause TorrentEvent.STOPPED
            torrent.stop();
            return true;
        }
        return false;
    }

    /**
     * Returns true if the given file is dangerous, after stopping the download.
     */
    private boolean isDangerous(File file) {
        if(LOG.isDebugEnabled())
            LOG.debug("Checking whether " + file + " is dangerous");
        if (dangerousFileChecker.get().isDangerous(file)) {
            if(LOG.isDebugEnabled())
                LOG.debug(file + " is dangerous");
            lastState.set(DownloadState.DANGEROUS);
            listeners.broadcast(new DownloadStateEvent(this, DownloadState.DANGEROUS));
            // This will cause TorrentEvent.STOPPED
            torrent.stop();
            return true;
        }
        return false;
    }

    private boolean promptAboutUnscannedPreview() {
        downloadCallback.get().promptAboutUnscannedPreview(this);
        return discardUnscannedPreview;
    }

    @Override
    public void discardUnscannedPreview(boolean delete) {
        discardUnscannedPreview = delete;
    }

    /**
     * Checks to see if this torrent has any priority zero files and removes
     * them.
     */
    private void cleanupPriorityZeroFiles() {
        LOG.debug("Cleaning up zero priority files");
        boolean hasAnyPriorityZero = false;

        List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
        for (TorrentFileEntry fileEntry : fileEntries) {
            if (fileEntry.getPriority() == 0) {
                hasAnyPriorityZero = true;
                break;
            }
        }

        if (hasAnyPriorityZero) {
            for (TorrentFileEntry fileEntry : fileEntries) {
                if (fileEntry.getPriority() == 0) {
                    File torrentDataFile = torrent.getTorrentDataFile(fileEntry);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Deleting " + torrentDataFile);
                    FileUtils.forceDelete(torrentDataFile);
                }
            }

            FileUtils.deleteEmptyDirectories(getSaveFile());
        }
    }

    /**
     * Adds the torrents files to the gnutella share list if the torrent is not
     * private and sharing is enabled, otehrwise the files are added to the
     * library.
     */
    private void addFileToCollections(File completeFile) {

        if (completeFile.isDirectory()) {
            if(LOG.isDebugEnabled())
                LOG.debug("Adding directory " + completeFile + " to library");
            FileFilter torrentFileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    // library addFile method will filter out any truly
                    // unaddable files.
                    return true;
                }
            };
            library.addFolder(completeFile, torrentFileFilter);
            if (!torrent.isPrivate()
                    && SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
                gnutellaFileCollection.addFolder(completeFile, torrentFileFilter);
            }
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("Adding file " + completeFile + " to library");
            library.add(completeFile);
            if (!torrent.isPrivate()
                    && SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()) {
                gnutellaFileCollection.add(completeFile);
            }
        }
    };

    @Override
    public void init(TorrentParams params) throws IOException {
        LOG.debug("Initializing");
        this.torrent = torrentManager.get().addTorrent(params);
        if(torrent == null) {
            LOG.debug("Error adding torrent to TorrentManager");
            throw new IOException("Error adding torrent to TorrentManager.");
        }
        torrent.addListener(this);

        setDefaultFileName(torrent.getName());
    }
    
    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {
        if (!torrent.isFinished()) {
            LOG.debug("Stopping when unfinished");
            torrent.stop();
            downloadManager.remove(this, true);
        } else {
            LOG.debug("Stopping when finished");
            downloadManager.remove(this, true);
        }
    }

    @Override
    public void pause() {
        LOG.debug("Pausing");
        torrent.pause();
    }

    @Override
    public boolean isPaused() {
        return torrent.isPaused();
    }

    @Override
    public boolean isLaunchable() {
        if (isCompleted()) {
            LOG.debug("Launchable: completed");
            return true;
        }

        TorrentInfo torrentInfo = torrent.getTorrentInfo();
        if (torrentInfo == null || torrentInfo.getTorrentFileEntries().size() > 1) {
            LOG.debug("Not launchable: no torrent info or multiple files");
            return false;
        }

        LOG.debug("Launchable: torrent info and only one file");
        return true;
    }

    @Override
    public boolean resume() {
        LOG.debug("Resuming");
        torrent.resume();
        return true;
    }

    @Override
    public File getFile() {
        if (torrent.isFinished()) {
            LOG.debug("Finished: returning save file");
            return getSaveFile();
        } else {
            LOG.debug("Unfinished: returning incomplete file");
            return getIncompleteFile();
        }
    }

    @Override
    public File getIncompleteFile() {
        return new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), torrent.getName());
    }

    @Override
    public File getDownloadFragment(ScanListener listener) {
        if (isCompleted()) {
            LOG.debug("Returning complete file");
            return getSaveFile();
        }

        // Can't preview a multi-file download
        TorrentInfo torrentInfo = torrent.getTorrentInfo();
        if (torrentInfo == null || torrentInfo.getTorrentFileEntries().size() > 1) {
            LOG.debug("No torrent info or multiple files");
            return null;
        }

        // Return a copy of the completed part of the file
        File copy = new File(getIncompleteFile().getParent(),
                IncompleteFileManager.PREVIEW_PREFIX
                + getIncompleteFile().getName());

        // TODO come up with correct size for preview, look at old code checking
        // last verified offsets etc. old code looks wrong though, since the
        // file downloads randomly, the last verified offset does not tell us
        // much.
        long size = Math.min(getIncompleteFile().length(), 2 * 1024 * 1024);
        if (FileUtils.copy(getIncompleteFile(), size, copy) <= 0) {
            LOG.debug("Failed to create preview copy");
            return null;
        }
        if (isInfectedOrDangerous(copy, listener)) {
            LOG.debug("Not returning infected or dangerous preview copy");
            copy.delete();
            return null;
        }
        LOG.debug("Returning preview copy");
        return copy;
    }
    
    @Override
    public DownloadState getState() {
        switch(lastState.get()) {
        case DANGEROUS:
            return DownloadState.DANGEROUS;
        case THREAT_FOUND:
            return DownloadState.THREAT_FOUND;
        case SCAN_FAILED:
            return DownloadState.SCAN_FAILED;
        case SCANNING:
            return DownloadState.SCANNING;
        }

        TorrentStatus status = torrent.getStatus();
        if (!torrent.isStarted() || status == null) {
            return DownloadState.QUEUED;
        }

        TorrentState state = status.getState();

        // complete must be before aborted in order to not remove the download
        // from the list prematurely when teh seed ratio is reached and the
        // torrent is marked as cancelled.
        if (torrent.isFinished()) {
            return DownloadState.COMPLETE;
        }

        if (torrent.isCancelled()) {
            return DownloadState.ABORTED;
        }

        if (status.isError()) {
            if(LOG.isErrorEnabled())
                LOG.error("Error downloading torrent: " + status.getError());
            // gave up maps to stalled in the core api, which is a recoverable
            // error. All torrent downlaods are recoverable.
            return DownloadState.GAVE_UP;
        }

        if (finishing.get()) {
            return DownloadState.SAVING;
        }

        return convertState(state);
    }

    private DownloadState convertState(TorrentState state) {
        switch (state) {
        case DOWNLOADING:
            if (isPaused()) {
                return DownloadState.PAUSED;
            } else {
                return DownloadState.DOWNLOADING;
            }
        case QUEUED_FOR_CHECKING:
            return DownloadState.RESUMING;
        case CHECKING_FILES:
            return DownloadState.RESUMING;
        case SEEDING:
            return DownloadState.COMPLETE;
        case FINISHED:
            return DownloadState.COMPLETE;
        case ALLOCATING:
            return DownloadState.CONNECTING;
        case DOWNLOADING_METADATA:
            return DownloadState.INITIALIZING;
        default:
            throw new IllegalStateException("Unknown libtorrent state: " + state);
        }
    }

    @Override
    public int getRemainingStateTime() {
        // Unused
        return 0;
    }

    @Override
    public long getContentLength() {
        TorrentStatus status = torrent.getStatus();
        long contentLength = status != null ? status.getTotalWanted() : -1;
        if(LOG.isDebugEnabled())
            LOG.debug("Content length: " + contentLength);
        return contentLength;
    }

    @Override
    public long getAmountRead() {
        TorrentStatus status = torrent.getStatus();
        if (status == null) {
            LOG.debug("No status: cannot get amount read");
            return -1;
        } else {
            long amountRead = status.getTotalWantedDone();
            if(LOG.isDebugEnabled())
                LOG.debug("Amount read: " + amountRead);
            return amountRead;
        }
    }
    
    @Override
    public long getAmountVerified() {
        TorrentStatus status = torrent.getStatus();
        if (status == null) {
            LOG.debug("No status: cannot get amount verified");
            return -1;
        } else {
            long amountVerified = status.getTotalWantedDone();
            if(LOG.isDebugEnabled())
                LOG.debug("Amount verified: " + amountVerified);
            return amountVerified;
        }
    }

    @Override
    public long getAmountLost() {
        TorrentStatus status = torrent.getStatus();
        if (status == null) {
            LOG.debug("No status: cannot get amount lost");
            return -1;
        } else {
            long amountLost = status.getTotalFailedDownload();
            if(LOG.isDebugEnabled())
                LOG.debug("Amount lost: " + amountLost);
            return amountLost;
        }
    }

    @Override
    public List<RemoteFileDesc> getRemoteFileDescs() {
        return Collections.emptyList();
    }

    @Override
    public int getQueuePosition() {
        return 1;
    }

    @Override
    public int getQueuedHostCount() {
        return 0;
    }

    @Override
    public GUID getQueryGUID() {
        // Unused for torrents
        return null;
    }

    @Override
    public boolean isCompleted() {
        return complete.get();
    }

    @Override
    public boolean shouldBeRemoved() {
        switch (getState()) {
        case ABORTED:
        case COMPLETE:
        case DANGEROUS:
        case THREAT_FOUND:
        case SCAN_FAILED:
            LOG.debug("Should be removed");
            return true;
        }
        LOG.debug("Should not be removed");
        return false;
    }

    @Override
    public void measureBandwidth() {
        // Unused, we are using the bandwidth reported by libtorrent
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        float bw = torrent.isPaused() ? 0 : (torrent.getDownloadRate() / 1024);
        if(LOG.isDebugEnabled())
            LOG.debug("Measured bandwidth: " + bw);
        return bw;
    }

    @Override
    public float getAverageBandwidth() {
        // Unused by anything
        float bw = torrent.isPaused() ? 0 : (torrent.getDownloadRate() / 1024);
        if(LOG.isDebugEnabled())
            LOG.debug("Average bandwidth: " + bw);
        return bw;
    }

    @Override
    public boolean isRelocatable() {
        return !isCompleted();
    }

    @Override
    protected File getDefaultSaveFile() {
        File f = new File(SharingSettings.getSaveDirectory(), torrent.getName());
        if(LOG.isDebugEnabled())
            LOG.debug("Default save file: " + f);
        return f;
    }

    @Override
    public URN getSha1Urn() {
        if (urn == null) {
            synchronized (this) {
                if (urn == null) {
                    try {
                        urn = URN.createSha1UrnFromHex(torrent.getSha1());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        LOG.debug(urn);
        return urn;
    }

    @Override
    public int getNumHosts() {
        int hosts = torrent.getNumPeers();
        if(LOG.isDebugEnabled())
            LOG.debug(hosts + " hosts");
        return hosts;
    }

    @Override
    public List<Address> getSourcesAsAddresses() {

        List<TorrentPeer> peers = torrent.getTorrentPeers();
        List<Address> list = new ArrayList<Address>(peers.size());
        
        for (TorrentPeer peer : peers) {
            String ip = peer.getIPAddress();
            if (ip != null) {
                try {
                    list.add(new ConnectableImpl(new IpPortImpl(ip), false));
                    if(LOG.isDebugEnabled())
                        LOG.debug("Peer: " + ip);
                } catch (UnknownHostException e) {
                    // Discard invalid host
                    if(LOG.isDebugEnabled())
                        LOG.debug("Invalid peer: " + ip);
                }
            }
        }

        return list;
    }

    @Override
    public List<SourceInfo> getSourcesDetails() {
        
        List<TorrentPeer> peers = torrent.getTorrentPeers();
        List<SourceInfo> sourceInfoList = new ArrayList<SourceInfo>(peers.size());
        
        for (TorrentPeer peer : peers) {
            sourceInfoList.add(new TorrentSourceInfoAdapter(peer));
        }
        return sourceInfoList;
    }

    @Override
    public DownloadPiecesInfo getPieceInfo() {
        return new BTDownloadPiecesInfo(torrent);
    }
    
    @Override
    public void initialize() {
    }

    @Override
    public void startDownload() {
        LOG.debug("Starting download");
        btUploaderFactory.createBTUploader(torrent);
        torrent.start();
    }

    @Override
    public void handleInactivity() {
        // nothing happens when we're inactive
    }

    @Override
    public boolean shouldBeRestarted() {
        return true;
    }

    @Override
    public boolean isAlive() {
        return false; // doesn't apply to torrents
    }

    @Override
    public boolean isQueuable() {
        return !isPaused();
    }

    @Override
    public synchronized void finish() {
        LOG.debug("Finishing");
        deleteIncompleteFiles();
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }

    @Override
    protected DownloadMemento createMemento() {
        LOG.debug("Creating memento");
        return new LibTorrentBTDownloadMementoImpl();
    }

    @Override
    protected void fillInMemento(DownloadMemento memento) {
        LOG.debug("Filling in memento");
        super.fillInMemento(memento);

        LibTorrentBTDownloadMemento btMemento = (LibTorrentBTDownloadMemento) memento;

        btMemento.setName(torrent.getName());
        btMemento.setSha1Urn(getSha1Urn());
        btMemento.setIncompleteFile(getIncompleteFile());
        btMemento.setTrackers(torrent.getTrackerURIS());
        File fastResumeFile = torrent.getFastResumeFile();
        String fastResumePath = fastResumeFile != null ? fastResumeFile.getAbsolutePath() : null;
        btMemento.setFastResumePath(fastResumePath);
        File torrentFile = torrent.getTorrentFile();
        String torrentPath = torrentFile != null ? torrentFile.getAbsolutePath() : null;
        btMemento.setTorrentPath(torrentPath);

        btMemento.setPrivate(torrent.isPrivate());

    }

    public void initFromCurrentMemento(LibTorrentBTDownloadMemento memento)
            throws InvalidDataException {
        LOG.debug("Initializing from memento");
        urn = memento.getSha1Urn();

        if (urn == null) {
            LOG.debug("Null URN");
            throw new InvalidDataException(
                    "Null SHA1 URN retrieved from LibTorrent torrent momento.");
        }

        if (!urn.isSHA1()) {
            LOG.debug("Non-SHA1 URN");
            throw new InvalidDataException(
                    "Non SHA1 URN retrieved from LibTorrent torrent momento.");
        }

        String fastResumePath = memento.getFastResumePath();
        File fastResumeFile = fastResumePath != null ? new File(fastResumePath) : null;

        String torrentPath = memento.getTorrentPath();
        File torrentFile = torrentPath != null ? new File(torrentPath) : null;

        try {
            TorrentParams params = new LibTorrentParams(SharingSettings.INCOMPLETE_DIRECTORY.get(),
                    memento.getName(), StringUtils.toHexString(urn.getBytes()));
            params.setTrackers(memento.getTrackers());
            params.setFastResumeFile(fastResumeFile);
            params.setTorrentFile(torrentFile);
            params.setTorrentDataFile(memento.getIncompleteFile());
            params.setPrivate(memento.isPrivate());

            init(params);
        } catch (IOException e) {
            LOG.debug("Could not initialize downloader (first try)", e);
            // the .torrent file could be invalid, try to initialize just with
            // the memento contents.
            try {
                TorrentParams params = new LibTorrentParams(
                        SharingSettings.INCOMPLETE_DIRECTORY.get(), memento.getName(), 
                        StringUtils.toHexString(urn.getBytes()));
                params.setTrackers(memento.getTrackers());
                params.setFastResumeFile(fastResumeFile);
                params.setTorrentDataFile(memento.getIncompleteFile());
                params.setPrivate(memento.isPrivate());
                init(params);
            } catch (IOException e1) {
                LOG.debug("Could not initialize downloader (second try)", e1);
                throw new InvalidDataException("Could not initialize the BTDownloader", e1);
            }
        }
    }

    public void initFromOldMemento(BTDownloadMemento memento) throws InvalidDataException {
        LOG.debug("Initializing from old memento");
        BTMetaInfoMemento btmetainfo = memento.getBtMetaInfoMemento();

        URI[] trackers = btmetainfo.getTrackers();

        String name = btmetainfo.getFileSystem().getName();

        byte[] infoHash = btmetainfo.getInfoHash();

        URN sha1;
        try {
            sha1 = URN.createSHA1UrnFromBytes(infoHash);
        } catch (IOException e) {
            LOG.debug("Could not create URN", e);
            throw new InvalidDataException(
                    "Could not initialize the BTDownloader, memento hash was invalid", e);
        }

        boolean isPrivate = btmetainfo.isPrivate();

        File saveFile = memento.getSaveFile();
        File saveDir = saveFile == null ? SharingSettings.getSaveDirectory() : saveFile
                .getParentFile();
        saveDir = saveDir == null ? SharingSettings.getSaveDirectory() : saveDir;
        File oldIncompleteFile = btmetainfo.getFileSystem().getIncompleteFile();
        File newIncompleteFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), name);
        if (newIncompleteFile.exists()) {
            LOG.debug("Incomplete file already exists");
            throw new InvalidDataException(
                    "Cannot init memento for BTDownloader, incomplete file already exists: "
                            + newIncompleteFile);
        }

        FileUtils.forceRename(oldIncompleteFile, newIncompleteFile);
        File torrentDir = oldIncompleteFile.getParentFile();
        if (torrentDir.getName().length() == 32) {
            // looks like the old torrent dir
            FileUtils.forceDeleteRecursive(torrentDir);
        }

        try {
            TorrentParams params = new LibTorrentParams(newIncompleteFile.getParentFile(), name,
                    StringUtils.toHexString(sha1.getBytes()));
            params.setTrackers(Arrays.asList(trackers));
            params.setTorrentDataFile(newIncompleteFile);
            params.setPrivate(isPrivate);
            init(params);
        } catch (IOException e) {
            LOG.debug("Could not initialize downloader", e);
            throw new InvalidDataException("Could not initialize the BTDownloader", e);
        }
    }

    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        if (BTDownloadMemento.class.isInstance(memento)) {
            initFromOldMemento((BTDownloadMemento) memento);
        } else if (LibTorrentBTDownloadMemento.class.isInstance(memento)) {
            initFromCurrentMemento((LibTorrentBTDownloadMemento) memento);
        }
        if (!torrent.isValid()) {
            LOG.debug("Error registering torrent");
            throw new InvalidDataException("Error registering torrent");
        }
    }

    /**
     * Adds basic DownloadStateEvent listener support. Currently only
     * broadcasts, COMPLETED and ABORTED states.
     */
    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public void deleteIncompleteFiles() {
        LOG.debug("Deleting incomplete files");
        if (!complete.get()) {
            LOG.debug("Not complete");
            File incompleteFile = getIncompleteFile();
            if (incompleteFile != null) {
                LOG.debug("Deleting incomplete file");
                FileUtils.forceDeleteRecursive(incompleteFile);
            }
        }
        if(torrent.getTorrentFile() != null && torrent.getTorrentFile().getParentFile().equals(SharingSettings.INCOMPLETE_DIRECTORY.get())) {
            LOG.debug("Deleting torrent file");
            FileUtils.forceDelete(torrent.getTorrentFile());
        }
        if(torrent.getFastResumeFile().getParentFile().equals(SharingSettings.INCOMPLETE_DIRECTORY.get())) {
            LOG.debug("Deleting fast resume file");
            FileUtils.forceDelete(torrent.getFastResumeFile());
        }
    }

    @Override
    public List<File> getCompleteFiles() {
        List<File> completeFiles = TorrentUtil.buildTorrentFiles(torrent, getSaveFile().getParentFile());
        if(LOG.isDebugEnabled()) {
            LOG.debug("Getting complete files:");
            for(File f : completeFiles) {
                LOG.debug(" " + f);
            }
        }
        return completeFiles;
    }

    public List<File> getIncompleteFiles() {
        List<File> incompleteFiles = TorrentUtil.buildTorrentFiles(torrent, getIncompleteFile().getParentFile());
        if(LOG.isDebugEnabled()) {
            LOG.debug("Getting incomplete files:");
            for(File f : incompleteFiles) {
                LOG.debug(" " + f);
            }
        }
        return incompleteFiles;
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... file) {
        if (getSha1Urn().equals(urn)) {
            LOG.debug("Conflicts with URN");
            return true;
        }

        for (File f : file) {
            if (conflictsSaveFile(f)) {
                return true;
            }
        }

        LOG.debug("Does not conflict");
        return false;
    }

    @Override
    public boolean conflictsSaveFile(File complete) {
        boolean conflicts = complete.equals(getSaveFile());
        if(conflicts)
            LOG.debug("Conflicts with save file");
        return conflicts;
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        boolean conflicts = incomplete.equals(getIncompleteFile());
        if(conflicts)
            LOG.debug("Conflicts with incomplete file");
        return conflicts;
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getChunkSize() {
        throw new UnsupportedOperationException("BTDownloaderImpl.getChunkSize() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getAmountPending() {
        throw new UnsupportedOperationException(
                "BTDownloaderImpl.getAmountPending() not implemented");
    }

    @Override
    public File getTorrentFile() {
        File torrentFile = torrent.getTorrentFile();
        if(LOG.isDebugEnabled())
            LOG.debug("Getting torrent file: " + torrentFile);
        return torrentFile;
    }

    public Torrent getTorrent() {
        return torrent;
    }
}
