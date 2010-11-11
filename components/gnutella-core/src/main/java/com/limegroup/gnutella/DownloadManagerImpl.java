package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.collection.MultiIterable;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadException.ErrorCode;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.libtorrent.LibTorrentParams;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.service.MessageService;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.Visitor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.bittorrent.TorrentUploadManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.MagnetDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.ResumeDownloader;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadSerializer;
import com.limegroup.gnutella.library.LibraryStatusEvent;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.mozilla.MozillaDownload;
import com.limegroup.mozilla.MozillaDownloaderImpl;

@EagerSingleton
public class DownloadManagerImpl implements DownloadManager, Service, EventListener<LibraryStatusEvent> {
    
    private static final Log LOG = LogFactory.getLog(DownloadManagerImpl.class);
    
    /** The time in milliseconds between checkpointing downloads.dat.  The more
     * often this is written, the less the lost data during a crash, but the
     * greater the chance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds


    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private final List <CoreDownloader> active=new LinkedList<CoreDownloader>();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates 
     *  LOCKING: obtain this' monitor */
    
    private final List <CoreDownloader> waiting=new LinkedList<CoreDownloader>();
    
    private final MultiIterable<CoreDownloader> activeAndWaiting = 
        new MultiIterable<CoreDownloader>(active,waiting); 
    
    /**
     * Whether or not the GUI has been init'd.
     */
    private volatile boolean downloadsReadFromDisk = false;
    
    /** The number if IN-NETWORK active downloaders.  We don't count these when
     * determining how many downloaders are active.
     */
    private int innetworkCount = 0;

    /**
     * The number of active store downloads. These are counted when determining
     * how many downloaders are active
     */
    private int storeDownloadCount = 0;
    
    private int mozillaDownloadCount = 0;
    
    /**
     * The number of times we've been bandwidth measures
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all downloads.
     * This is only counted while downloads are active.
     */
    private float averageBandwidth = 0;
    
    /** The last measured bandwidth, as counted from measureBandwidth. */
    private volatile float lastMeasuredBandwidth;
    
    /**
     * The runnable that pumps inactive downloads to the correct state.
     */
    private Runnable _waitingPump;
    
    private final EventListenerList<DownloadManagerEvent> listeners =
        new EventListenerList<DownloadManagerEvent>();

    private final Provider<DownloadCallback> downloadCallback;
    private final Provider<MessageRouter> messageRouter;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final CoreDownloaderFactory coreDownloaderFactory;
    private final DownloadSerializer downloadSerializer;
    private final IncompleteFileManager incompleteFileManager;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final CategoryManager categoryManager;
    private final PushEndpointFactory pushEndpointFactory;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<TorrentUploadManager> torrentUploadManager;

    @Inject
    public DownloadManagerImpl(
            Provider<DownloadCallback> downloadCallback,
            Provider<MessageRouter> messageRouter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<PushDownloadManager> pushDownloadManager,
            CoreDownloaderFactory coreDownloaderFactory,
            DownloadSerializer downloaderSerializer,
            IncompleteFileManager incompleteFileManager,
            RemoteFileDescFactory remoteFileDescFactory,
            PushEndpointFactory pushEndpointFactory,
            Provider<TorrentManager> torrentManager,
            Provider<TorrentUploadManager> torrentUploadManager,
            CategoryManager categoryManager) {
        this.downloadCallback = downloadCallback;
        this.messageRouter = messageRouter;
        this.backgroundExecutor = backgroundExecutor;
        this.pushDownloadManager = pushDownloadManager;
        this.coreDownloaderFactory = coreDownloaderFactory;
        this.downloadSerializer = downloaderSerializer;
        this.incompleteFileManager = incompleteFileManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.pushEndpointFactory = pushEndpointFactory;
        this.torrentManager = torrentManager;
        this.torrentUploadManager = torrentUploadManager;
        this.categoryManager = categoryManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#register(com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry)
     */
    // DO NOT REMOVE!  Guice calls this because of the @Inject
    @Inject
    public void register(PushedSocketHandlerRegistry registry) {
        registry.register(this);
    }
    
    @Inject
    void register(ListenerSupport<LibraryStatusEvent> listeners) {
        listeners.addListener(this);
    }

    //////////////////////// Creation and Saving /////////////////////////
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
        registry.register(new Service() {
            public String getServiceName() {
                return org.limewire.i18n.I18nMarker.marktr("Old Downloads");
            }

            public void initialize() {
            };

            public void start() {
                loadSavedDownloadsAndScheduleWriting();
                //TODO load uploads from somewhere else?
                torrentUploadManager.get().loadSavedUploads();
            }

            public void stop() {
            };
        }).in(ServiceStage.LATE);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#start()
     */
    public void start() {
        scheduleWaitingPump();
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Download Management");
    }
    public void initialize() {
    }
    public void stop() {
        writeSnapshot();
    }
    
    /**
     * Adds a new downloader to this manager.
     * @param downloader the core downloader
     */
    public void addNewDownloader(CoreDownloader downloader) {
        initializeDownload(downloader, false);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#loadSavedDownloadsAndScheduleWriting()
     */
    public void loadSavedDownloadsAndScheduleWriting() {
        loadSavedDownloads();
        scheduleSnapshots();
    }
    
    public void loadSavedDownloads() {
        boolean failedAll = true;
        boolean failedSome = false;
        
        List<DownloadMemento> mementos;
        try {
            mementos = downloadSerializer.readFromDisk();
            if(mementos.isEmpty())
                failedAll = false;
        } catch(IOException ioex) {
            mementos = Collections.emptyList();
        }
        for(DownloadMemento memento : mementos) {
            CoreDownloader coreDownloader = prepareMemento(memento);
            if(coreDownloader != null) {
                failedAll = false;
                addNewDownloader(coreDownloader);
            } else {
                failedSome = true;
            }
        }
        
        loadResumeDownloaders();
        downloadsReadFromDisk = true;
        
        if(failedAll) {
            MessageService.showError(I18nMarker.marktr("Sorry, LimeWire couldn't read your old downloads.  You can restart them by clicking 'Try Again' on the downloads.  When LimeWire finds a source for the file, the download will pick up where it left off."));
        } else if(failedSome) {
            MessageService.showError(I18nMarker.marktr("Sorry, LimeWire couldn't read some of your old downloads.  You can restart them by clicking 'Try Again' on the downloads.  When LimeWire finds a source for the file, the download will pick up where it left off."));
        }
    }
    
    private void loadResumeDownloaders() {
        Collection<File> incompleteFiles = 
            incompleteFileManager.getUnregisteredIncompleteFilesInDirectory(
                    SharingSettings.INCOMPLETE_DIRECTORY.get());
        for(File file : incompleteFiles) {
            try {
                download(file);
            } catch (DownloadException e) {
                LOG.error("SLE loading incomplete file", e);
            } catch (CantResumeException e) {
                LOG.error("CRE loading incomplete file", e);
            }
        }
    }
    
    public CoreDownloader prepareMemento(DownloadMemento memento) {
        try {
            return coreDownloaderFactory.createFromMemento(memento);
        } catch(InvalidDataException ide) {
            LOG.warn("Unable to read download from memento: " + memento, ide);
            return null;
        }
    }
    
    public void scheduleSnapshots() {
        Runnable checkpointer=new Runnable() {
            public void run() {
                if (downloadsInProgress() > 0) { //optimization
                    writeSnapshot();
                }
            }
        };
        backgroundExecutor.scheduleWithFixedDelay(checkpointer, 
                               SNAPSHOT_CHECKPOINT_TIME, 
                               SNAPSHOT_CHECKPOINT_TIME, TimeUnit.MILLISECONDS);   
    }      
    
    public void writeSnapshot() {
        if (!downloadsReadFromDisk) {
            LOG.debug("downloads not loaded yet, not writing snapshot");
            return;
        }
        List<DownloadMemento> mementos;
        synchronized(this) {
            mementos = new ArrayList<DownloadMemento>(active.size() + waiting.size());
            for(CoreDownloader downloader : activeAndWaiting) {
                if(downloader.isMementoSupported()) {
                    mementos.add(downloader.toMemento());
                }
            }
        }
        
        downloadSerializer.writeToDisk(mementos);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#isGUIInitd()
     */
    public boolean isSavedDownloadsLoaded() {
        return downloadsReadFromDisk;
    }
    
    PushDownloadManager getPushManager() {
        return pushDownloadManager.get();
    }

    /**
     * Delegates the incoming socket out to BrowseHostHandler & then attempts to assign it
     * to any ManagedDownloader.
     * 
     * Closes the socket if neither BrowseHostHandler nor any ManagedDownloaders wanted it.
     * 
     */
    private synchronized boolean handleIncomingPush(String file, int index, byte [] clientGUID, Socket socket) {
         boolean handled = false;
         for (CoreDownloader md : activeAndWaiting) {
            if (! (md instanceof ManagedDownloader))
                continue; // pushes apply to gnutella downloads only
            ManagedDownloader mmd = (ManagedDownloader)md;
            if (mmd.acceptDownload(file, socket, index, clientGUID)) {
                return true;
            }
         }                 
         return handled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#acceptPushedSocket(java.lang.String, int, byte[], java.net.Socket)
     */
    public boolean acceptPushedSocket(String file, int index,
            byte[] clientGUID, Socket socket) {
        return handleIncomingPush(file, index, clientGUID, socket);
    }
    
    
    public void scheduleWaitingPump() {
        if(_waitingPump != null)
            return;
            
        _waitingPump = new Runnable() {
            public void run() {
                pumpDownloads();
            }
        };
        backgroundExecutor.scheduleWithFixedDelay(_waitingPump,
                               1000,
                               1000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Pumps through each waiting download, either removing it because it was
     * stopped, or adding it because there's an active slot and it requires
     * attention.
     */
    protected synchronized void pumpDownloads() {
        int index = 1;
        for(Iterator<CoreDownloader> i = waiting.iterator(); i.hasNext(); ) {
            CoreDownloader md = i.next();
            if(md.isAlive()) {
                continue;
            } else if(md.shouldBeRemoved()) {
                i.remove();
                cleanupCompletedDownload(md, false);
            }
            // handle downloads from LWS separately, only allow 3 at a time
            else if( storeDownloadCount < 3 && md.getDownloadType() == DownloaderType.STORE ) {
                i.remove();
                storeDownloadCount++;
                active.add(md);
                md.startDownload();
            } else if(hasFreeSlot() && (md.shouldBeRestarted()) && (md.getDownloadType() != DownloaderType.STORE)) {
                i.remove();
                if(md.getDownloadType() == DownloaderType.INNETWORK)
                    innetworkCount++;
                active.add(md);
                md.startDownload();
            } else {
                if(md.isQueuable())
                    md.setInactivePriority(index++);
                md.handleInactivity();
            }
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#isIncomplete(com.limegroup.gnutella.URN)
     */
    public boolean isIncomplete(URN urn) {
        return incompleteFileManager.getFileForUrn(urn) != null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#isActivelyDownloading(com.limegroup.gnutella.URN)
     */
    public boolean isActivelyDownloading(URN urn) {
        Downloader md = getDownloaderForURN(urn);
        
        if(md == null)
            return false;
            
        switch(md.getState()) {
        case QUEUED:
        case BUSY:
        case ABORTED:
        case GAVE_UP:
        case UNABLE_TO_CONNECT:
        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case REMOTE_QUEUED:
        case WAITING_FOR_USER:
        case DANGEROUS:
        case THREAT_FOUND:
        case SCAN_FAILED:
            return false;
        default:
            return true;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getIncompleteFileManager()
     */
    public IncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }    
 
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#downloadsInProgress()
     */
    public synchronized int downloadsInProgress() {
        return active.size() + waiting.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getNumIndividualDownloaders()
     */
    public synchronized int getNumIndividualDownloaders() {
        int ret = 0;
        for (Iterator<CoreDownloader> iter=active.iterator(); iter.hasNext(); ) {  //active
            Object next = iter.next();
            if (! (next instanceof ManagedDownloader))
                continue; // TODO: count torrents separately
            ManagedDownloader md=(ManagedDownloader)next;
            ret += md.getNumDownloaders();
       }
       return ret;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getNumActiveDownloads()
     */
    public synchronized int getNumActiveDownloads() {
        return active.size() - innetworkCount - storeDownloadCount - mozillaDownloadCount;
    }
   
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getNumWaitingDownloads()
     */
    public synchronized int getNumWaitingDownloads() {
        return waiting.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getDownloaderForURN(com.limegroup.gnutella.URN)
     */
    public synchronized Downloader getDownloaderForURN(URN sha1) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.getSha1Urn() != null && sha1.equals(md.getSha1Urn()))
                return md;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getDownloaderForURNString(java.lang.String)
     */
    public synchronized Downloader getDownloaderForURNString(String urn) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.getSha1Urn() != null && urn.equals(md.getSha1Urn().toString()))
                return md;
        }
        return null;
    }    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getDownloaderForIncompleteFile(java.io.File)
     */
    public synchronized Downloader getDownloaderForIncompleteFile(File file) {
        for (CoreDownloader dl : activeAndWaiting) {
            if (dl.conflictsWithIncompleteFile(file)) {
                return dl;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#isGuidForQueryDownloading(com.limegroup.gnutella.GUID)
     */
    public synchronized boolean isGuidForQueryDownloading(GUID guid) {
        for (CoreDownloader md : activeAndWaiting) {
            GUID dGUID = md.getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        return false;
    }
    
    void clearAllDownloads() {
        List<CoreDownloader> buf;
        synchronized(this) {
            buf = new ArrayList<CoreDownloader>(active.size() + waiting.size());
            buf.addAll(active);
            buf.addAll(waiting);
            active.clear();
            waiting.clear();
        }
        for(CoreDownloader md : buf ) { 
            md.stop();
            fireEvent(md, DownloadManagerEvent.Type.REMOVED);
        }
    }
           
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#download(com.limegroup.gnutella.RemoteFileDesc[], java.util.List, com.limegroup.gnutella.GUID, boolean, java.io.File, java.lang.String)
     */
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            List<? extends RemoteFileDesc> alts, GUID queryGUID, 
                                            boolean overwrite, File saveDir,
                                            String fileName) 
        throws DownloadException {

        String fName = getFileName(files, fileName);
        if (conflicts(files, new File(saveDir,fName))) {
            addRemoteFileDescsToDownloader(files);
            
            throw new DownloadException
            (ErrorCode.FILE_ALREADY_DOWNLOADING,
                    new File(fName != null ? fName : ""));
        }

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();

        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader =
            coreDownloaderFactory.createManagedDownloader(files, 
                queryGUID, saveDir, fileName, overwrite);

        initializeDownload(downloader, true);
        
        //Now that the download is started, add the sources w/o caching
        downloader.addDownload(alts,false);
        
        return downloader;
    }

    /**
     * Adds the provided file descs to the first downloader in the list that
     * matches up with it.
     */
    private void addRemoteFileDescsToDownloader(RemoteFileDesc[] files) {
        List<CoreDownloader> downloaders = new ArrayList<CoreDownloader>(active.size() + waiting.size());
        synchronized (this) { 
            // add to all downloaders, even if they are waiting....
            downloaders.addAll(active);
            downloaders.addAll(waiting);
        }
        
        for(CoreDownloader downloader : downloaders) {
            if(downloader instanceof ManagedDownloader) {
                ManagedDownloader managedDownloader = (ManagedDownloader) downloader;
                if(managedDownloader.addDownload(Arrays.asList(files), true)) {
                    //only add fileDescs to 1 downloader
                    break;
                }
            }
        }
    }
    
    /* (non-Javadoc)
    * @see com.limegroup.gnutella.DownloadManager#download(com.limegroup.gnutella.browser.MagnetOptions, boolean, java.io.File, java.lang.String)
    */
    public synchronized Downloader download(MagnetOptions magnet,
            boolean overwrite,
            File saveDir,
            String fileName)
    throws IllegalArgumentException, DownloadException {
        
        if (!magnet.isGnutellaDownloadable()) 
            throw new IllegalArgumentException("magnet not downloadable");
        
        //remove entry from IFM if the incomplete file was deleted.
        incompleteFileManager.purge();
        
        if (fileName == null) {
            fileName = magnet.getFileNameForSaving();
        }
        if (conflicts(magnet.getSHA1Urn(), 0, new File(saveDir,fileName))) {
            throw new DownloadException
            (ErrorCode.FILE_ALREADY_DOWNLOADING, new File(fileName));
        }

        //Note: If the filename exists, it would be nice to check that we are
        //not already downloading the file by calling conflicts with the
        //filename...the problem is we cannot do this effectively without the
        //size of the file (at least, not without being risky in assuming that
        //two files with the same name are the same file). So for now we will
        //just leave it and download the same file twice.

        //Instantiate downloader, validating incompleteFile first.
        MagnetDownloader downloader = 
            coreDownloaderFactory.createMagnetDownloader( magnet,
                overwrite, saveDir, fileName);
        initializeDownload(downloader, true);
        return downloader;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#download(java.io.File)
     */ 
    public synchronized Downloader download(File incompleteFile)
            throws CantResumeException, DownloadException { 
     
        if (conflictsWithIncompleteFile(incompleteFile)) {
            throw new DownloadException
            (ErrorCode.FILE_ALREADY_DOWNLOADING, incompleteFile);
        }

        if (IncompleteFileManager.isTorrentFile(incompleteFile)) {
            return resumeTorrentDownload(incompleteFile);
        }

        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        //if (! overwrite) {
        //    try {
        //        File downloadDir=SettingsManager.instance().getSaveDirectory();
        //        File completeFile=new File(
        //            downloadDir, 
        //            incompleteFileManager.getCompletedName(incompleteFile));
        //        if (completeFile.exists())
        //            throw new FileExistsException(filename);
        //    } catch (IllegalArgumentException e) {
        //        throw new CantResumeException(incompleteFile.getName());
        //    }
        //}

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge();

        //Instantiate downloader, validating incompleteFile first.
        ResumeDownloader downloader=null;
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
            String name=IncompleteFileManager.getCompletedName(incompleteFile);
            long size= IncompleteFileManager.getCompletedSize(incompleteFile);
            downloader = coreDownloaderFactory.createResumeDownloader(
                                              incompleteFile,
                                              name,
                                              size);
        } catch (IllegalArgumentException e) {
            throw new CantResumeException(e, incompleteFile.getName());
        } catch (IOException ioe) {
            throw new CantResumeException(ioe, incompleteFile.getName());
        }
        
        initializeDownload(downloader, true);
        return downloader;
    }

    private Downloader resumeTorrentDownload(File torrentFile) throws CantResumeException,
            DownloadException {
        if(torrentManager.get().isValid()) {
            return downloadTorrent(torrentFile, null, false);
        } else {
            throw new CantResumeException("Torrent Manager Invalid", torrentFile.getName());
        }
    }
    
    @Override
    public synchronized Downloader downloadTorrent(URI torrentURI, final boolean overwrite)
            throws DownloadException {
        if(!isSavedDownloadsLoaded()) {
            throw new DownloadException(DownloadException.ErrorCode.FILES_STILL_RESUMING, null);
        }
        
        if(!torrentManager.get().isValid()) {
            throw new DownloadException(DownloadException.ErrorCode.NO_TORRENT_MANAGER, null);
        }
        
        final BTTorrentFileDownloader torrentDownloader = coreDownloaderFactory
                .createTorrentFileDownloader(torrentURI, true);
        initializeDownload(torrentDownloader, false);
        return torrentDownloader;
    }

    @Override
    public synchronized Downloader downloadTorrent(File torrentFile, File saveDirectory, boolean overwrite)
            throws DownloadException {
        
        if(!isSavedDownloadsLoaded()) {
            throw new DownloadException(DownloadException.ErrorCode.FILES_STILL_RESUMING, torrentFile);
        }
        
        if(!torrentManager.get().isValid()) {
            throw new DownloadException(DownloadException.ErrorCode.NO_TORRENT_MANAGER, torrentFile);
        }
        
        if (torrentFile.length() > 1024 * 1024 * 5) {
            // torrent files are supposed to be small. If it is large it is
            // probably not a valid torrent file
            throw new DownloadException(
                    DownloadException.ErrorCode.TORRENT_FILE_TOO_LARGE, torrentFile);
        }

        if(saveDirectory == null) {
            saveDirectory = SharingSettings.getSaveDirectory();
        }
        
        TorrentParams params = new LibTorrentParams(SharingSettings.INCOMPLETE_DIRECTORY.get(), torrentFile);
        BTDownloader ret = null;
        try {
            params.fill();
            checkIfAlreadyManagedTorrent(params);
            checkActiveAndWaiting(params, saveDirectory);
            
            ret = coreDownloaderFactory.createBTDownloader(params);
            if(ret == null || ret.getTorrent() == null || !ret.getTorrent().isValid()) {
                throw new DownloadException(DownloadException.ErrorCode.NO_TORRENT_MANAGER, torrentFile);
            }

            // Does the torrent contain any files with banned extensions?
            if(!overwrite) {
                Torrent torrent = ret.getTorrent();
                if(!torrent.hasMetaData() || torrent.getTorrentInfo() == null) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Torrent lacks info: " + torrentFile +
                                ", size: " + torrentFile.length() +
                                ", exists: " + torrentFile.exists());
                } else {
                    Set<String> banned = getBannedAndDisabledExtensions(torrent);
                    if(!banned.isEmpty() && !downloadCallback.get().promptAboutTorrentWithBannedExtensions(torrent, banned))
                        throw new DownloadException(DownloadException.ErrorCode.DOWNLOAD_CANCELLED, torrentFile);
                }
            }

            ret.setSaveFile(saveDirectory, null, overwrite);
            if(!overwrite) {
                File saveFile = ret.getSaveFile();
                if (saveFile.exists()) {
                    throw new DownloadException(ErrorCode.FILE_ALREADY_EXISTS, saveFile);
                }
            }
        } catch (IOException e) {
            LOG.error("Error creating BTDownloader", e);
            if(ret != null) {
                Torrent torrent = ret.getTorrent();
                torrentManager.get().removeTorrent(torrent);
                ret.deleteIncompleteFiles();
            }
            if(e instanceof DownloadException) {
                throw (DownloadException)e;
            } else {
                throw new DownloadException(e, torrentFile);
            }
        }

        Torrent torrent = ret.getTorrent();
        if(BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.getValue() && !downloadCallback.get().promptTorrentFilePriorities(torrent)) {
            torrentManager.get().removeTorrent(torrent);
            ret.deleteIncompleteFiles();
            throw new DownloadException(DownloadException.ErrorCode.DOWNLOAD_CANCELLED, torrentFile);
        }

        initializeDownload(ret, true);
        return ret;
    }

    //TODO: this may overwrite files in the library after metadata is found
    @Override
    public synchronized Downloader downloadTorrent(String name, URN sha1,
            List<URI> trackers) throws DownloadException {
        
        if(LOG.isInfoEnabled()) {
            LOG.info("Downloading torrent:");
            LOG.info(" " + name);
            LOG.info(" " + sha1);
            for(URI tracker : trackers) {
                LOG.info(" " + tracker);
            }
        }
        if(!isSavedDownloadsLoaded()) {
            LOG.info("Saved downloads not loaded");
            throw new DownloadException(DownloadException.ErrorCode.FILES_STILL_RESUMING, null);
        }
        
        if(!torrentManager.get().isValid()) {
            LOG.info("Torrent manager is not valid");
            throw new DownloadException(DownloadException.ErrorCode.NO_TORRENT_MANAGER, null);
        }
        
        TorrentParams params = new LibTorrentParams(SharingSettings.INCOMPLETE_DIRECTORY.get(), 
                name, StringUtils.toHexString(sha1.getBytes()), trackers);
        checkIfAlreadyManagedTorrent(params);
        
        try {
            final BTDownloader torrentDownloader = coreDownloaderFactory.createBTDownloader(params);
            
            initializeDownload(torrentDownloader, false);
            
            return torrentDownloader;
        } catch (IOException e) {
            LOG.error("Error creating BTDownloader", e);
            if(e instanceof DownloadException) {
                throw (DownloadException)e;
            } else {
                throw new DownloadException(e, null);
            }
        }
        
    }
  
    /**
     * Returns a (possibly empty) set of banned or disabled file extensions
     * belonging to files in the given torrent. This method should only be
     * called for torrents with metadata and torrent info.
     * Package access for testing.
     */
    Set<String> getBannedAndDisabledExtensions(Torrent torrent) {
        assert torrent.hasMetaData();
        TorrentInfo info = torrent.getTorrentInfo();
        assert info != null;
        Set<String> extensions = new HashSet<String>();
        for(TorrentFileEntry entry: info.getTorrentFileEntries()) {
            String ext = FileUtils.getFileExtension(entry.getPath());
            if(!ext.isEmpty())
                extensions.add(ext);
        }
        Set<String> banned = new HashSet<String>();
        for(String extWithDot : FilterSettings.BANNED_EXTENSIONS.get()) {
            // Sanity check in case the user did something weird to the setting
            if(extWithDot.length() > 1 && extWithDot.startsWith("."))
                banned.add(extWithDot.substring(1));
        }
        if(!LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            banned.addAll(categoryManager.getExtensionsForCategory(Category.PROGRAM));
        }
        extensions.retainAll(banned);
        return extensions;
    }

    /**
     * Ensures the eventual download location is not already taken by the files
     * of any other download.
     */
    private void checkActiveAndWaiting(TorrentParams params, File saveDirectory) throws DownloadException {
        
        URN urn = null;
        try {
            urn = URN.createSha1UrnFromHex(params.getSha1());
        } catch (IOException e) {
           throw new DownloadException(ErrorCode.FILESYSTEM_ERROR, params.getTorrentFile());
        }
        for (CoreDownloader current : activeAndWaiting) {
            if (urn.equals(current.getSha1Urn())) {
                throw new DownloadException(ErrorCode.FILE_ALREADY_DOWNLOADING, params.getTorrentDataFile());
            }

            File saveFile = new File(saveDirectory, params.getName());
            if (current.conflictsSaveFile(saveFile)) {
                throw new DownloadException(ErrorCode.FILE_IS_ALREADY_DOWNLOADED_TO, saveFile);
            }

            if (current.conflictsSaveFile(params.getTorrentDataFile())) {
                throw new DownloadException(ErrorCode.FILE_ALREADY_DOWNLOADING, params
                        .getTorrentDataFile());
            }
        }
    }

    private void checkIfAlreadyManagedTorrent(TorrentParams params) throws DownloadException {
        Torrent torrent = torrentManager.get().getTorrent(params.getTorrentFile());
        if(torrent == null) {
            torrent = torrentManager.get().getTorrent(params.getSha1());
        }
        
        if(torrent != null) {
            if(!torrent.isFinished()) {
                LOG.info("Already downloading");
                throw new DownloadException(ErrorCode.FILE_ALREADY_DOWNLOADING, params.getTorrentDataFile());
            } else {
                LOG.info("Already uploading");
                throw new DownloadException(ErrorCode.FILE_ALREADY_UPLOADING, params.getTorrentDataFile());
            }
        }
    }

    public synchronized Downloader downloadFromMozilla(MozillaDownload listener) {
        
        CoreDownloader downloader = new MozillaDownloaderImpl(this, categoryManager, listener);
        
        downloader.initialize();
        callback(downloader).addDownload(downloader);
        active.add(downloader);
        mozillaDownloadCount++;
        fireEvent(downloader, DownloadManagerEvent.Type.ADDED);
        return downloader;
    }

    /**
     * Performs common tasks for initializing the download.
     * 1) Initializes the downloader.
     * 2) Adds the download to the waiting list.
     * 3) Notifies the callback about the new downloader.
     * 4) Writes the new snapshot out to disk.
     */
    private synchronized void initializeDownload(final CoreDownloader md, boolean saveState) {
        md.initialize();
        waiting.add(md);
        callback(md).addDownload(md);
        if(saveState) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    writeSnapshot(); // Save state for crash recovery.
                }
            });
        }
        // TODO: do this outside the lock
        fireEvent(md, DownloadManagerEvent.Type.ADDED);
    }
    
    /**
     * Returns the callback that should be used for the given md.
     */
    private DownloadCallback callback(Downloader md) {
        return downloadCallback.get();
    }
        
    /**
     * Returns true if there already exists a download for the same file.
     * <p>
     * Same file means: same urn, or as fallback same filename + same filesize
     */
    private boolean conflicts(RemoteFileDesc[] rfds, File... fileName) {
        URN urn = null;
        for (int i = 0; i < rfds.length && urn == null; i++) {
            urn = rfds[0].getSHA1Urn();
        }
        
        return conflicts(urn, rfds[0].getSize(), fileName);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#conflicts(com.limegroup.gnutella.URN, long, java.io.File)
     */
    public boolean conflicts(URN urn, long fileSize, File... fileName) {
        
        if (urn == null && fileSize == 0) {
            return false;
        }
        
        synchronized (this) {
            for (CoreDownloader md : activeAndWaiting) {
                if (md.conflicts(urn, fileSize, fileName)) 
                    return true;
            }
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#isSaveLocationTaken(java.io.File)
     */
    public synchronized boolean isSaveLocationTaken(File candidateFile) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.conflictsSaveFile(candidateFile))
                return true;
        }
        return false;
    }

    private synchronized boolean conflictsWithIncompleteFile(File incompleteFile) {
        for (CoreDownloader md : activeAndWaiting) {
            if (md.conflictsWithIncompleteFile(incompleteFile))
                return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#handleQueryReply(com.limegroup.gnutella.messages.QueryReply)
     */
    public void handleQueryReply(QueryReply qr, Address address) {
        // first check if the qr is of 'sufficient quality', if not just
        // short-circuit.
        if (qr.calculateQualityOfService() < 1)
            return;

        List<Response> responses;
        try {
            qr.validate();
            responses = qr.getResultsAsList();
        } catch(BadPacketException bpe) {
            return; // bad packet, do nothing.
        }
        
        addDownloadWithResponses(responses, qr, address);
    }

    /**
     * Iterates through all responses seeing if they can be matched
     * up to any existing downloaders, adding them as possible
     * sources if they do.
     * @param address can be null 
     */
    private void addDownloadWithResponses(List<? extends Response> responses, QueryReply queryReply, Address address) {
        if(responses == null)
            throw new NullPointerException("null responses");
        if(queryReply == null)
            throw new NullPointerException("null queryReply");

        // need to synch because active and waiting are not thread safe
        List<CoreDownloader> downloaders = new ArrayList<CoreDownloader>(active.size() + waiting.size());
        synchronized (this) { 
            // add to all downloaders, even if they are waiting....
            downloaders.addAll(active);
            downloaders.addAll(waiting);
        }
        
        // short-circuit.
        if(downloaders.isEmpty())
            return;

        //For each response i, offer it to each downloader j.  Give a response
        // to at most one downloader.
        // TODO: it's possible that downloader x could accept response[i] but
        //that would cause a conflict with downloader y.  Check for this.
        for(Response r : responses) {
            // Don't bother with making XML from the EQHD.
            RemoteFileDesc rfd;
            try {
                rfd = r.toRemoteFileDesc(queryReply, address, remoteFileDescFactory, pushEndpointFactory);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            for(Downloader current : downloaders) {
                if ( !(current instanceof ManagedDownloader))
                    continue; // can't add sources to torrents yet
                ManagedDownloader currD = (ManagedDownloader) current;
                // If we were able to add this specific rfd,
                // add any alternates that this response might have
                // also.
                LOG.debugf("adding rfd {0} to downloader {1}", rfd, currD);
                if (currD.addDownload(rfd, true)) {
                    for(IpPort ipp : r.getLocations()) {
                        // don't cache alts.
                        currD.addDownload(remoteFileDescFactory.createRemoteFileDesc(rfd, ipp), false);
                    }
                    break;
                }
            }
        }
    }

    // //////////// Callback Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held by caller */
    private boolean hasFreeSlot() {
        return active.size() - innetworkCount - storeDownloadCount - mozillaDownloadCount
            < DownloadSettings.MAX_SIM_DOWNLOAD.getValue();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#remove(com.limegroup.gnutella.downloader.CoreDownloader, boolean)
     */
    public synchronized void remove(CoreDownloader downloader, 
                                    boolean completed) {
        if(active.remove(downloader)) {
            DownloaderType type = downloader.getDownloadType();
            // These counters only apply to active downloads
            if(type == DownloaderType.INNETWORK)
                innetworkCount--;
            else if(type == DownloaderType.STORE)
                storeDownloadCount--;
            else if(type == DownloaderType.MOZILLA)
                mozillaDownloadCount--;
        }
        waiting.remove(downloader);
        if(completed)
            cleanupCompletedDownload(downloader, true);
        else
            waiting.add(downloader);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#bumpPriority(com.limegroup.gnutella.Downloader, boolean, int)
     */
    public synchronized void bumpPriority(Downloader downl,
                                          boolean up, int amt) {
        CoreDownloader downloader = (CoreDownloader)downl;
        int idx = waiting.indexOf(downloader);
        if(idx == -1)
            return;

        if(up && idx != 0) {
            waiting.remove(idx);
            if (amt > idx)
                amt = idx;
            if (amt != 0)
                waiting.add(idx - amt, downloader);
            else
                waiting.add(0, downloader);     //move to top of list
        } else if(!up && idx != waiting.size() - 1) {
            waiting.remove(idx);
            if (amt != 0) {
                amt += idx;
                if (amt > waiting.size())
                    amt = waiting.size();
                waiting.add(amt, downloader);
            } else {
                waiting.add(downloader);    //move to bottom of list
            }
        }
    }

    /**
     * Cleans up the given Downloader after completion.
     *
     * If ser is true, also writes a snapshot to the disk.
     */
    private void cleanupCompletedDownload(CoreDownloader dl, boolean ser) {
        dl.finish();
        if (dl.getQueryGUID() != null)
            messageRouter.get().downloadFinished(dl.getQueryGUID());
        callback(dl).removeDownload(dl);
        
        //Save this' state to disk for crash recovery.
        if(ser)
            writeSnapshot();

        // Enable auto shutdown
        if(active.isEmpty() && waiting.isEmpty())
            callback(dl).downloadsComplete();
        
        fireEvent(dl, DownloadManagerEvent.Type.REMOVED);
    }           
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#sendQuery(com.limegroup.gnutella.downloader.ManagedDownloader, com.limegroup.gnutella.messages.QueryRequest)
     */
    public void sendQuery(QueryRequest query) {
        messageRouter.get().sendDynamicQuery(query);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#measureBandwidth()
     */
    public void measureBandwidth() {
        List<CoreDownloader> activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList<CoreDownloader>(active);
        }
        
        float currentTotal = 0f;
        boolean c = false;
        for (BandwidthTracker bt : activeCopy) {
            c = true;
            bt.measureBandwidth();
            currentTotal += bt.getAverageBandwidth();
        }
        
        if ( c ) {
            synchronized(this) {
                averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                    / ++numMeasures;
            }
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getMeasuredBandwidth()
     */
    public float getMeasuredBandwidth() {
        List<CoreDownloader> activeCopy;
        synchronized(this) {
            activeCopy = new ArrayList<CoreDownloader>(active);
        }
        
        float sum=0;
        for (BandwidthTracker bt : activeCopy) {
            float curr = 0;
            try{
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;//insufficient data? assume 0
            }
            sum+=curr;
        }
                
        lastMeasuredBandwidth = sum;
        return sum;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getAverageBandwidth()
     */
    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getLastMeasuredBandwidth()
     */
    public float getLastMeasuredBandwidth() {
        return lastMeasuredBandwidth;
    }
    
    private String getFileName(RemoteFileDesc[] rfds, String fileName) {
        for (int i = 0; i < rfds.length && fileName == null; i++) {
            fileName = rfds[i].getFileName();
        }
        return fileName;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.DownloadManager#getAllDownloaders()
     */
    public final Iterable<CoreDownloader> getAllDownloaders() {
        return activeAndWaiting;
    }
    
    /**
     * Listens for events from FileManager
     */
    public void handleEvent(LibraryStatusEvent evt) {
        switch(evt.getType()){
            case LOAD_FINISHING:
                getIncompleteFileManager().registerAllIncompleteFiles();
                break;
        }
    }
    
    // ---------------------------------------------------------------
    // Implementation of LWSIntegrationServicesDelegate    

    public synchronized void visitDownloads(Visitor<CoreDownloader> visitor) {
        for (CoreDownloader downloader : activeAndWaiting) {
            visitor.visit(downloader);
        }
    }    

    private void fireEvent(CoreDownloader downloader, DownloadManagerEvent.Type type) {
        listeners.broadcast(new DownloadManagerEvent(downloader, type));
    }

    public void addListener(EventListener<DownloadManagerEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<DownloadManagerEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public synchronized boolean contains(Downloader downloader) {
        for(CoreDownloader coreDownloader : activeAndWaiting) {
            if(coreDownloader == downloader) {
                return true;
            }
        }
        return false;
    }
}
