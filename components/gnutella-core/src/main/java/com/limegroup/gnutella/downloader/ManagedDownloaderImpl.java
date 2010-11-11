package com.limegroup.gnutella.downloader;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.ApproximateMatcher;
import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.collection.IntervalSet;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.malware.VirusEngine;
import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.io.Address;
import org.limewire.io.DiskException;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.io.PermanentAddress;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.service.ErrorService;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.altlocs.AltLocListener;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.DirectDHTAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.downloader.RequeryManager.QueryType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMemento;
import com.limegroup.gnutella.downloader.serial.GnutellaDownloadMementoImpl;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.malware.VirusScanException;
import com.limegroup.gnutella.malware.VirusScanner;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A smart download.  Tries to get a group of similar files by delegating
 * to DownloadWorker threads.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.<p>
 * <p/>
 * Smart downloads can use many policies, and these policies are free to change
 * as allowed by the Downloader specification.  This implementation provides
 * swarmed downloads, the ability to download copies of the same file from
 * multiple hosts.  <p>
 * <p/>
 * Subclasses may refine the requery behavior by overriding {@link #newRequery()}
 * {@link #allowAddition(RemoteFileDesc)}, {@link #addDownload(Collection, boolean)}.
 */
class ManagedDownloaderImpl extends AbstractCoreDownloader implements AltLocListener,
        ManagedDownloader, DownloadWorkerSupport {

    /*
    * IMPLEMENTATION NOTES: The basic idea behind swarmed (multisource)
    * downloads is to download one file in parallel from multiple servers. For
    * example, one might simultaneously download the first half of a file from
    * server A and the second half from server B. This increases throughput if
    * the downstream capacity of the downloader is greater than the upstream
    * capacity of the fastest uploader.
    *
    * The ideal way of identifying duplicate copies of a file is to use hashes
    * via the HUGE proposal.
    *
    * When discussing swarmed downloads, it's useful to divide parts of a file
    * into three categories: black, grey, and white. Black regions have already
    * been downloaded to disk. Grey regions have been assigned to a downloader
    * but not yet completed. White regions have not been assigned to a
    * downloader.
    *
    * ManagedDownloader delegates to multiple DownloadWorker instances, one for
    * each HTTP connection. They use a shared VerifyingFile object that keeps
    * track of which blocks have been written to disk.
    *
    * ManagedDownloader uses one thread to control the smart downloads plus one
    * thread per DownloadWorker instance. The call flow of ManagedDownloader's
    * "master" thread is as follows:
    *
    * performDownload: initializeDownload fireDownloadWorkers (asynchronously
    * start workers) verifyAndSave
    *
    * The core downloading loop is done by fireDownloadWorkers.Currently the
    * desired parallelism is fixed at 2 for modem users, 6 for cable/T1/DSL,
    * and 8 for T3 and above.
    *
    * DownloadManager notifies a ManagedDownloader when it should start
    * performDownload. An inactive download (waiting for a busy host, waiting
    * for a user to requery, waiting for GUESS responses, etc..) is essentially
    * a state-machine, pumped forward by DownloadManager. The 'master thread'
    * of a ManagedDownloader is recreated every time DownloadManager moves the
    * download from inactive to active.
    *
    * All downloads start QUEUED. From there, it will stay queued until a slot
    * is available.
    *
    * If at least one host is available to download from, then the first state
    * is always CONNECTING. After connecting, a downloader can become: a)
    * DOWNLOADING (actively downloading) b) WAITING_FOR_RETRY (busy hosts) c)
    * ABORTED (user manually stopped the download) c2) PAUSED (user paused the
    * download) d) REMOTE_QUEUED (the remote host queued us)
    *
    * If no hosts existed for connecting, or we exhausted our attempts at
    * connecting to all possible hosts, the state will become one of: e)
    * GAVE_UP (max'ed out on requeries) f) WAITING_FOR_USER (waiting for the
    * user to initiate a requery) g) ITERATIVE_GUESSING (targeted location of
    * more sources) If the user resumes the download and we were
    * WAITING_FOR_USER, a requery is sent out and we go into
    * WAITING_FOR_RESULTS stage. After we have finished waiting for results (if
    * none arrived), we will either go back to WAITING_FOR_USER (if we are
    * allowed more requeries), or GAVE_UP (if we maxed out the requeries).
    * After ITERATIVE_GUESSING completes, if no results arrived then we go to
    * WAITING_FOR_USER. Prior to WAITING_FOR_RESULTS, if no connections are
    * active then we wait at WAITING_FOR_CONNECTIONS until connections exist.
    *
    * If more results come in while waiting in these states, the download will
    * either immediately become active (CONNECTING ...) again, or change its
    * state to QUEUED and wait for DownloadManager to activate it.
    *
    * The download can finish in one of the following states: - COMPLETE
    * (download completed just fine) - ABORTED (user pressed stopped at some
    * point) - DISK_PROBLEM (LimeWire couldn't manipulate the file) -
    * CORRUPT_FILE (the file was corrupt) - INVALID (content authority didn't
    * allow the transfer)
    *
    * There are a few intermediary states: - HASHING - SAVING HASHING & SAVING
    * are seen by the GUI, and are used just prior to COMPLETE, to let the user
    * know what is currently happening in the closing states of the download.
    * RECOVERY_FAILED is used as an indicator that we no longer want to retry
    * the download, because we've tried and recovered from corruption too many
    * times.
    *
    * How corruption is handled: There are two general cases where corruption
    * can be discovered - during a download or after the download has finished.
    *
    * During the download, each worker thread checks periodically whether the
    * amount of data lost to corruption exceeds 10% of the completed file size.
    * Whenever that happens, the worker thread asks the user whether the
    * download should be terminated. If the user chooses to delete the file,
    * the downloader is stopped asynchronously and _corruptState is set to
    * CORRUPT_STOP_STATE. The master download thread is interrupted, it checks
    * _corruptState and either discards or removes the file.
    *
    * After the download, if the sha1 does not match the expected, the master
    * download thread prompts the user whether they want to keep the file or
    * discard it. If we did not have a tree during the download we remove the
    * file from partial sharing, otherwise we keep it until the user answers
    * the prompt (which may take a very long time for overnight downloads). The
    * tree itself is purged.
    *
    */

    private static final Log LOG = LogFactory.getLog(ManagedDownloaderImpl.class);

    /*********************************************************************
     * LOCKING: obtain this's monitor before modifying any of the following.
     * files, _activeWorkers, busy and setState.  We should  not hold lock 
     * while performing blocking IO operations.
     *
     * Never acquire incompleteFileManager's monitor if you have commonOutFile's
     * monitor.
     *
     * Never obtain manager's lock if you hold this.
     ***********************************************************************/


    /**
     * The complete Set of files passed to the constructor.  Must be
     * maintained in memory to support resume.  allFiles may only contain
     * elements of type RemoteFileDesc and URLRemoteFileDesc
     */
    private Set<RemoteFileDesc> cachedRFDs;

    /**
     * Set of {@link RemoteFileDesc remote file descs} that can't be resolved
     * or connected to yet.
     */
    private final Set<RemoteFileDesc> permanentRFDs = new ConcurrentSkipListSet<RemoteFileDesc>(new Comparator<RemoteFileDesc>() {
        @Override
        public int compare(RemoteFileDesc o1, RemoteFileDesc o2) {
            return o1.hashCode() - o2.hashCode();
        }
    });

    private ConcurrentMap<RemoteFileDesc, RemoteFileDescContext> remoteFileDescToContext = new ConcurrentHashMap<RemoteFileDesc, RemoteFileDescContext>();

    /**
     * The ranker used to select the next host we should connect to
     */
    private SourceRanker ranker;

    /**
     * How long we'll wait after sending a GUESS query before we try something
     * else.
     */
    private static final int GUESS_WAIT_TIME = 5000;


    /**
     * The size of the approx matcher 2d buffer...
     */
    private static final int MATCHER_BUF_SIZE = 120;

    /**
     * This is used for matching of filenames.  kind of big so we only want
     * one.
     */
    private static ApproximateMatcher matcher =
            new ApproximateMatcher(MATCHER_BUF_SIZE);

    ////////////////////////// Core Variables /////////////////////////////

    /**
     * If started, the thread trying to coordinate all downloads.
     * Otherwise null.
     */
    private volatile Thread dloaderManagerThread;
    /**
     * True iff this has been forcibly stopped.
     */
    private volatile boolean stopped;
    /**
     * True iff this has been paused.
     */
    private volatile boolean paused;
    /**
     * True if this has been invalidated.
     */
    private volatile boolean invalidated;


    /**
     * The connections we're using for the current attempts.
     * LOCKING: copy on write on this
     */
    private volatile List<DownloadWorker> _activeWorkers;

    /**
     * A List of workers in progress.  Used to make sure that we do
     * not terminate in fireDownloadWorkers without hope if threads are
     * connecting to hosts but not have not yet been added to _activeWorkers.
     * <p/>
     * Also, if the download completes and any workers are queued, those
     * workers need to be signalled to stop.
     * <p/>
     * LOCKING: synchronize on this
     */
    private List<DownloadWorker> _workers;

    /**
     * Stores the queued threads and the corresponding queue position
     * LOCKING: copy on write on this
     */
    private volatile Map<DownloadWorker, Integer> _queuedWorkers;

    /**
     * Set of RFDs where we store rfds we are currently connected to or
     * trying to connect to.
     */
    private Set<RemoteFileDesc> currentRFDs;

    /**
     * The SHA1 hash of the file that this ManagedDownloader is controlling.
     */
    private volatile URN downloadSHA1;

    /**
     * The collection of alternate locations we successfully downloaded from
     * something from.
     */
    private Set<AlternateLocation> validAlts;

    /**
     * A list of the most recent failed locations, so we don't try them again.
     */
    private Set<RemoteFileDesc> invalidAlts;

    /**
     * Cache the most recent failed locations.
     * Holds <tt>AlternateLocation</tt> instances
     */
    private Set<AlternateLocation> recentInvalidAlts;

    /**
     * Manages writing stuff to disk, remember what's leased, what's verified,
     * what is valid, etc........
     */
    protected volatile VerifyingFile commonOutFile;

    /**
     * A list of pushing hosts.
     */
    private volatile PushList pushes;

    ///////////////////////// Variables for GUI Display  /////////////////
    /**
     * The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
     * etc.   Should be modified only through setState.
     */
    private DownloadState state = DownloadState.INITIALIZING;
    /**
     * The system time that we expect to LEAVE the current state, or
     * Integer.MAX_VALUE if we don't know. Should be modified only through
     * setState.
     */
    private long stateTime;

    /**
     * The current incomplete file that we're downloading, or the last
     * incomplete file if we're not currently downloading, or null if we
     * haven't started downloading.  Used for previewing purposes.
     */
    private volatile File incompleteFile;

    /**
     * The position of the downloader in the uploadQueue
     */
    private int queuePosition;

    /**
     * If in CORRUPT_FILE state, the number of bytes downloaded.  Note that
     * this is less than corruptFile.length() if there are holes.
     */
    private volatile long corruptFileBytes;
    /**
     * If in CORRUPT_FILE state, the name of the saved corrupt file, or null if
     * no corrupt file.
     */
    private volatile File corruptFile;

    /**
     * Whether a preview that could not be scanned for viruses should be
     * deleted.
     */
    private volatile boolean discardUnscannedPreview;

    /**
     * Locking object to be used for accessing all alternate locations.
     * LOCKING: never try to obtain monitor on this if you hold the monitor on
     * altLock
     */
    private Object altLock;

    /**
     * The number of times we've been bandwidth measured
     */
    private int numMeasures = 0;

    /**
     * The average bandwidth over all managed downloads.
     */
    private float averageBandwidth = 0f;

    /**
     * The GUID of the original query.  may be null.
     */
    private volatile GUID originalQueryGUID;

    /**
     * Whether or not we've sent a GUESS query.
     */
    private boolean triedLocatingSources;

    /**
     * Whether or not we've gotten new files since the last time this download
     * started.
     */
    private volatile boolean receivedNewSources;

    /**
     * The number of hosts that were tried to be connected to. Value is reset
     * in {@link #startDownload()};
     */
    private volatile int triedHosts;

    private long contentLength = -1;

    protected final DownloadManager downloadManager;
    protected final FileCollection gnutellaFileCollection;
    protected final IncompleteFileManager incompleteFileManager;
    protected final DownloadCallback downloadCallback;
    protected final NetworkManager networkManager;
    protected final AlternateLocationFactory alternateLocationFactory;
    protected final RequeryManager requeryManager;
    protected final QueryRequestFactory queryRequestFactory;
    protected final OnDemandUnicaster onDemandUnicaster;
    protected final DownloadWorkerFactory downloadWorkerFactory;
    protected final AltLocManager altLocManager;
    protected final SourceRankerFactory sourceRankerFactory;
    protected final UrnCache urnCache;
    protected final VerifyingFileFactory verifyingFileFactory;
    protected final DiskController diskController;
    protected final IPFilter ipFilter;
    protected final ScheduledExecutorService backgroundExecutor;
    protected final Provider<MessageRouter> messageRouter;
    protected final Provider<HashTreeCache> tigerTreeCache;
    protected final ApplicationServices applicationServices;
    protected final RemoteFileDescFactory remoteFileDescFactory;
    protected final Provider<PushList> pushListProvider;
    protected final DangerousFileChecker dangerousFileChecker;
    protected final VirusScanner virusScanner;
    protected final SpamManager spamManager;
    protected final Library library;
    protected final CategoryManager categoryManager;

    private final EventMulticaster<DownloadStateEvent> listeners;
    private final BandwidthCollector bandwidthCollector;
    private final SocketsManager socketsManager;
    private final ConnectivityChangeEventHandler connectivityChangeEventHandler = new ConnectivityChangeEventHandler();

    private boolean isFriendDownload;

    /**
     * Creates a new ManagedDownload to download the given files.
     * <p/>
     * You must set initial source via {@link #addInitialSources},
     * set the save file via {@link #setSaveFile(File, String, boolean)},
     * and call {@link #initialize} prior to starting this download.
     * @param categoryManager TODO
     */
    @Inject
    protected ManagedDownloaderImpl(SaveLocationManager saveLocationManager,
            DownloadManager downloadManager,
            @GnutellaFiles FileCollection gnutellaFileCollection,
            IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback,
            NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            RequeryManagerFactory requeryManagerFactory,
            QueryRequestFactory queryRequestFactory,
            OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory,
            AltLocManager altLocManager,
            SourceRankerFactory sourceRankerFactory,
            UrnCache urnCache,
            VerifyingFileFactory verifyingFileFactory,
            DiskController diskController,
            IPFilter ipFilter,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter,
            Provider<HashTreeCache> tigerTreeCache,
            ApplicationServices applicationServices,
            RemoteFileDescFactory remoteFileDescFactory,
            Provider<PushList> pushListProvider,
            SocketsManager socketsManager,
            @Named("downloadStateProcessingQueue")ListeningExecutorService downloadStateProcessingQueue,
            DangerousFileChecker dangerousFileChecker,
            VirusScanner virusScanner,
            SpamManager spamManager,
            Library library,
            CategoryManager categoryManager,
            BandwidthCollector bandwidthCollector) {
        super(saveLocationManager, categoryManager);
        this.listeners = new AsynchronousMulticasterImpl<DownloadStateEvent>(downloadStateProcessingQueue);
        this.downloadManager = downloadManager;
        this.gnutellaFileCollection = gnutellaFileCollection;
        this.incompleteFileManager = incompleteFileManager;
        this.downloadCallback = downloadCallback;
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.socketsManager = socketsManager;
        this.requeryManager = requeryManagerFactory.createRequeryManager(new RequeryListenerImpl());
        this.queryRequestFactory = queryRequestFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.downloadWorkerFactory = downloadWorkerFactory;
        this.altLocManager = altLocManager;
        this.sourceRankerFactory = sourceRankerFactory;
        this.urnCache = urnCache;
        this.verifyingFileFactory = verifyingFileFactory;
        this.diskController = diskController;
        this.ipFilter = ipFilter;
        this.backgroundExecutor = backgroundExecutor;
        this.messageRouter = messageRouter;
        this.tigerTreeCache = tigerTreeCache;
        this.applicationServices = applicationServices;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.cachedRFDs = new HashSet<RemoteFileDesc>();
        this.pushListProvider = pushListProvider;
        this.dangerousFileChecker = dangerousFileChecker;
        this.virusScanner = virusScanner;
        this.spamManager = spamManager;
        this.library = library;
        this.categoryManager = categoryManager;
        this.bandwidthCollector = bandwidthCollector;
    }

    @Override
    public synchronized void addInitialSources(Collection<RemoteFileDesc> rfds, String defaultFileName) {
        if (rfds == null) {
            LOG.debug("rfds are null");
            rfds = Collections.emptyList();
        }

        cachedRFDs.addAll(rfds);
        for (RemoteFileDesc rfd : rfds) {
            if (rfd.getAddress() instanceof PermanentAddress) {
                permanentRFDs.add(rfd);
            }
        }

        isFriendDownload = isFriendDownload(rfds);

        if (rfds.size() > 0) {
            RemoteFileDesc initialRfd = rfds.iterator().next();
            initPropertiesMap(initialRfd);
            setAttribute("LimeXMLDocument", initialRfd.getXMLDocument(), false);
        }

        assert rfds.size() > 0 || defaultFileName != null;
        if (!hasDefaultFileName())
            setDefaultFileName(defaultFileName);
    }

    private boolean isFriendDownload(Collection<RemoteFileDesc> rfds) {
        for (RemoteFileDesc rfd : rfds) {
            if(!(rfd.getAddress() instanceof FriendAddress)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setQueryGuid(GUID queryGuid) {
        this.originalQueryGUID = queryGuid;
    }

    protected synchronized void initPropertiesMap(RemoteFileDesc rfd) {
        if (!hasDefaultFileName()) {
            setDefaultFileName(rfd.getFileName());
        }
        if (getContentLength() == -1)
            setContentLength(rfd.getSize());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#initialize()
     */
    @Override
    public void initialize() {
        setState(DownloadState.INITIALIZING);

        // Every setter needs the lock.
        synchronized (this) {
            currentRFDs = new HashSet<RemoteFileDesc>();
            _activeWorkers = new LinkedList<DownloadWorker>();
            _workers = new ArrayList<DownloadWorker>();
            _queuedWorkers = new HashMap<DownloadWorker, Integer>();
            stopped = false;
            paused = false;
            pushes = pushListProvider.get();
            discardUnscannedPreview = true;
            altLock = new Object();
            numMeasures = 0;
            averageBandwidth = 0f;
            queuePosition = Integer.MAX_VALUE;
            triedLocatingSources = false;
            ranker = getSourceRanker(null);
            ranker.setMeshHandler(this);
            for (RemoteFileDesc rfd : cachedRFDs) {
                if (getSha1Urn() != null)
                    break;
                if (rfd.getSHA1Urn() != null)
                    setSha1Urn(rfd.getSHA1Urn());
            }
        }
        setState(DownloadState.QUEUED);

        if (getSha1Urn() != null)
            altLocManager.addListener(getSha1Urn(), this);

        socketsManager.addListener(connectivityChangeEventHandler);

        // make sure all rfds have the same sha1
        verifyAllFiles();

        synchronized (altLock) {
            validAlts = new HashSet<AlternateLocation>();
            // stores up to 1000 locations for up to an hour each
            invalidAlts = new FixedSizeExpiringSet<RemoteFileDesc>(1000, 60 * 60 * 1000L);
            // stores up to 10 locations for up to 10 minutes
            recentInvalidAlts = new FixedSizeExpiringSet<AlternateLocation>(10, 10 * 60 * 1000L);
        }

        try {
            //initializeFilesAndFolders();
            initializeIncompleteFile();
            initializeVerifyingFile();
        } catch (IOException bad) {
            setState(DownloadState.DISK_PROBLEM);
            reportDiskProblem(bad);
            return;
        }

        setState(DownloadState.QUEUED);
    }

    private void reportDiskProblem(IOException cause) {
        if (DownloadSettings.REPORT_DISK_PROBLEMS.getBoolean()) {
            if (!(cause instanceof DiskException))
                cause = new DiskException(cause);
            ErrorService.error(cause);
        }
    }

    protected void reportDiskProblem(String cause) {
        if (DownloadSettings.REPORT_DISK_PROBLEMS.getBoolean())
            ErrorService.error(new DiskException(cause));
    }

    /**
     * Verifies the integrity of the RemoteFileDesc set.
     * <p/>
     * At one point in time, LimeWire somehow allowed files with different
     * SHA1s to be placed in the same ManagedDownloader.  This breaks
     * the invariants of the current ManagedDownloader, so we must
     * remove the extraneous RFDs.
     */
    private synchronized void verifyAllFiles() {
        if (getSha1Urn() == null)
            return;

        for (Iterator<RemoteFileDesc> iter = cachedRFDs.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = iter.next();
            if (rfd.getSHA1Urn() != null && !getSha1Urn().equals(rfd.getSHA1Urn()))
                iter.remove();
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#startDownload()
    */
    @Override
    public synchronized void startDownload() {
        assert dloaderManagerThread == null : "already started";
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                try {
                    dloaderManagerThread = Thread.currentThread();
                    receivedNewSources = false;
                    // reset tried hosts count
                    triedHosts = 0;
                    DownloadState status = performDownload();
                    completeDownload(status);
                } catch (Throwable t) {
                    // if any unhandled errors occurred, remove this
                    // download completely and message the error.
                    ManagedDownloaderImpl.this.stop();
                    setState(DownloadState.ABORTED);
                    downloadManager.remove(ManagedDownloaderImpl.this, true);

                    ErrorService.error(t);
                } finally {
                    dloaderManagerThread = null;
                }
            }
        }, "ManagedDownload");
    }

    /**
     * Completes the download process, possibly sending off requeries
     * that may later restart it.
     * <p/>
     * This essentially pumps the state of the download to different
     * areas, depending on what is required or what has already occurred.
     */
    private void completeDownload(DownloadState status) {
        boolean complete;
        boolean clearingNeeded = false;
        int waitTime = 0;
        // If TAD2 gave a completed state, set the state correctly & exit.
        // Otherwise...
        // If we manually stopped then set to ABORTED, else set to the 
        // appropriate state (either a busy host or no hosts to try).
        synchronized (this) {
            switch (status) {
                case COMPLETE:
                case DISK_PROBLEM:
                case CORRUPT_FILE:
                case DANGEROUS:
                case THREAT_FOUND:
                case SCAN_FAILED:
                    clearingNeeded = true;
                    setState(status);
                    break;
                case BUSY:
                case GAVE_UP:
                    if (invalidated) {
                        clearingNeeded = true;
                        setState(DownloadState.INVALID);
                    } else if (stopped) {
                        setState(DownloadState.ABORTED);
                    } else if (paused) {
                        setState(DownloadState.PAUSED);
                    } else {
                        setState(status); // BUSY or GAVE_UP
                    }
                    break;
                default:
                    assert false : "Bad status from tad2: " + status;
            }

            complete = isCompleted();

            waitTime = ranker.calculateWaitTime();
            ranker.stop();
            if (clearingNeeded)
                ranker = null;
        }

        // Notify the manager that this download is done.
        // This MUST be done outside of this' lock, else
        // deadlock could occur.
        downloadManager.remove(this, complete);

        if (clearingNeeded) {
            synchronized (altLock) {
                recentInvalidAlts.clear();
                invalidAlts.clear();
                validAlts.clear();
            }
            if (complete) {
                synchronized (this) {
                    cachedRFDs.clear(); // the call right before this serializes. 
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("MD completing <" + getSaveFile().getName()
                    + "> completed download, state: " + getState());
        }

        diskController.clearCaches();

        // if this is all completed, nothing else to do.
        if (complete) {
            ; // all done.

            // if this is paused, nothing else to do also.
        } else if (getState() == DownloadState.PAUSED) {
            ; // all done for now.

            // Try iterative GUESSing...
            // If that sent some queries, don't do anything else.
            // TODO: consider moving this inside the monitor
        } else if (tryGUESSing()) {
            ; // all done for now.

        } else {
            // the next few checks need to be atomic wrt dht callbacks to
            // requeryManager.

            // do not issue actual requeries while holding this.
            boolean requery = false;
            synchronized (this) {
                // If busy, try waiting for that busy host.
                if (getState() == DownloadState.BUSY) {
                    setState(DownloadState.BUSY, waitTime);

                    // If we sent a query recently, then we don't want to send another,
                    // nor do we want to give up.  Just continue waiting for results
                    // from that query.
                } else if (requeryManager.isWaitingForResults()) {
                    switch (requeryManager.getLastQueryType()) {
                        case DHT:
                            setState(DownloadState.QUERYING_DHT, requeryManager.getTimeLeftInQuery());
                            break;
                        case GNUTELLA:
                            setState(DownloadState.WAITING_FOR_GNET_RESULTS, requeryManager.getTimeLeftInQuery());
                            break;
                        default:
                            throw new IllegalStateException("Not any query type!");
                    }

                    // If we're allowed to immediately send a query, do it!
                } else if (canSendRequeryNow()) {
                    requery = true;

                    // If we can send a query after we activate, wait for the user.
                } else if (requeryManager.canSendQueryAfterActivate()) {
                    setState(DownloadState.WAITING_FOR_USER);

                    // Otherwise, there's nothing we can do, give up.
                } else {
                    setState(DownloadState.GAVE_UP);

                }
            }

            if (requery)
                requeryManager.sendQuery();
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("MD completed <" + getSaveFile().getName()
                    + "> completed download, state: " + getState());
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#handleInactivity()
    */
    @Override
    public synchronized void handleInactivity() {
//        if(LOG.isTraceEnabled())
        //LOG.trace("handling inactivity. state: " +
        //getState() + ", hasnew: " + hasNewSources() +
        //", left: " + getRemainingStateTime());
        switch (getState()) {
            case BUSY:
            case WAITING_FOR_CONNECTIONS:
            case ITERATIVE_GUESSING:
                // If we're finished waiting on busy hosts,
                // stable connections, or GUESSing,
                // but we're still inactive, then we queue ourselves
                // and wait till we get restarted.
                if (getRemainingStateTime() <= 0 || hasNewSources())
                    setState(DownloadState.QUEUED);
                break;
            case QUERYING_DHT:
            case WAITING_FOR_GNET_RESULTS:
                // If we have new sources but are still inactive,
                // then queue ourselves and wait to restart.
                if (hasNewSources())
                    setState(DownloadState.QUEUED);
                    // Otherwise, if we've ran out of time waiting for results,
                    // give up.  If another requery can be sent, the GAVE_UP
                    // pump will trigger it to start.
                else if (requeryManager.getTimeLeftInQuery() <= 0)
                    setState(DownloadState.GAVE_UP);
                break;
            case WAITING_FOR_USER:
                if (hasNewSources() || requeryManager.canSendQueryNow())
                    setState(DownloadState.QUEUED);
                break;
            case GAVE_UP:
                if (hasNewSources() || requeryManager.canSendQueryAfterActivate())
                    setState(DownloadState.QUEUED);
            case QUEUED:
            case PAUSED:
                // If we're waiting for the user to do something,
                // have given up, or are queued, there's nothing to do.
                break;
            default:
                throw new IllegalStateException("invalid state: " + getState() +
                        ", workers: " + _workers.size() +
                        ", _activeWorkers: " + _activeWorkers.size() +
                        ", _queuedWorkers: " + _queuedWorkers.size());
        }
    }

    /**
     * Tries iterative GUESSing of sources.
     */
    private boolean tryGUESSing() {
        if (originalQueryGUID == null || triedLocatingSources || getSha1Urn() == null)
            return false;

        Set<GUESSEndpoint> guessLocs = messageRouter.get().getQueryLocs(this.originalQueryGUID);
        if (guessLocs.isEmpty())
            return false;

        setState(DownloadState.ITERATIVE_GUESSING, GUESS_WAIT_TIME);
        triedLocatingSources = true;

        //TODO: should we increment a stat to get a sense of
        //how much this is happening?
        for (GUESSEndpoint ep : guessLocs) {
            onDemandUnicaster.query(ep, getSha1Urn());
            // TODO: see if/how we can wait 750 seconds PER send again.
            // if we got a result, no need to continue GUESSing.
            if (receivedNewSources)
                break;
        }

        return true;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isAlive()
    */
    @Override
    public boolean isAlive() {
        return dloaderManagerThread != null;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isCompleted()
    */
    @Override
    public boolean isCompleted() {
        switch (getState()) {
            case COMPLETE:
            case ABORTED:
            case DISK_PROBLEM:
            case CORRUPT_FILE:
            case INVALID:
            case DANGEROUS:
            case THREAT_FOUND:
            case SCAN_FAILED:
                return true;
        }
        return false;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isRelocatable()
    */
    @Override
    public boolean isRelocatable() {
        if (isInactive())
            return true;
        switch (getState()) {
            case INITIALIZING:
            case CONNECTING:
            case DOWNLOADING:
            case REMOTE_QUEUED:
                return true;
            default:
                return false;
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isActive()
    */
    @Override
    public boolean isActive() {
        switch (getState()) {
            case CONNECTING:
            case DOWNLOADING:
            case REMOTE_QUEUED:
            case HASHING:
            case SAVING:
            case SCANNING:
                return true;
        }
        return false;
    }

    boolean isInactive() {
        switch (getState()) {
            case INITIALIZING:
            case QUEUED:
            case GAVE_UP:
            case WAITING_FOR_GNET_RESULTS:
            case WAITING_FOR_USER:
            case WAITING_FOR_CONNECTIONS:
            case ITERATIVE_GUESSING:
            case QUERYING_DHT:
            case BUSY:
            case PAUSED:
                return true;
        }
        return false;
    }

    /**
     * reloads any previously busy hosts in the ranker, as well as other
     * hosts that we know about
     */
    protected synchronized void initializeRanker(SourceRanker ranker) {
        ranker.setMeshHandler(this);
        ranker.addToPool(getContexts(cachedRFDs));
    }

    /**
     * initializes the verifying file if the incompleteFile is initialized.
     */
    protected void initializeVerifyingFile() throws IOException {
        if (incompleteFile == null)
            return;

        // get VerifyingFile
        commonOutFile = incompleteFileManager.getEntry(incompleteFile);
        if (commonOutFile == null) {// no entry in incompleteFM
            long completedSize = IncompleteFileManager.getCompletedSize(incompleteFile);
            if (completedSize > MAX_FILE_SIZE)
                throw new IOException("invalid incomplete file " + completedSize);
            commonOutFile = verifyingFileFactory.createVerifyingFile(completedSize);
            commonOutFile.setScanForExistingBlocks(true, incompleteFile.length());
            incompleteFileManager.addEntry(incompleteFile, commonOutFile, shouldPublishIFD());
        }
    }

    protected void initializeIncompleteFile() throws IOException {
        if (incompleteFile != null)
            return;

        URN sha1 = getSha1Urn();
        if (sha1 != null) {
            incompleteFile = incompleteFileManager.getFileForUrn(sha1);
        }
        if (incompleteFile == null) {
            incompleteFile = getIncompleteFile(getSaveFile().getName(), sha1,
                    getContentLength());
        }

        if (LOG.isWarnEnabled())
            LOG.warn("Incomplete File: " + incompleteFile);
    }

    /**
     * Retrieves an incomplete file from the given incompleteFileManager with the
     * given name, URN & content-length.
     * <p>
     * It can be overridden in subclasses.
     * </p>
     */
    protected File getIncompleteFile(String name, URN urn,
                                     long length) throws IOException {
        return incompleteFileManager.getFile(name, urn, length);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#conflictsWithIncompleteFile(java.io.File)
     */
    @Override
    public boolean conflictsWithIncompleteFile(File incFile) {
        File iFile = incompleteFile;
        if (iFile != null) {
            return iFile.equals(incFile);
        }
        URN urn = getSha1Urn();
        if (urn != null) {
            iFile = incompleteFileManager.getFileForUrn(urn);
        }
        if (iFile != null) {
            return iFile.equals(incFile);
        }

        RemoteFileDesc rfd = null;
        synchronized (this) {
            if (!hasRFD()) {
                return false;
            }
            rfd = cachedRFDs.iterator().next();
        }
        if (rfd != null) {
            try {
                File thisFile = incompleteFileManager.getFile(rfd);
                return thisFile.equals(incFile);
            } catch (IOException ioe) {
                return false;
            }
        }
        return false;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#conflicts(com.limegroup.gnutella.URN, long, java.io.File)
    */
    @Override
    public boolean conflicts(URN urn, long fileSize, File... fileName) {
        if (urn != null && getSha1Urn() != null) {
            return urn.equals(getSha1Urn());
        }
        if (fileSize > 0) {
            try {
                File file = incompleteFileManager.getFile(fileName[0].getName(), null, fileSize);
                return conflictsWithIncompleteFile(file);
            } catch (IOException e) {
            }
        }
        return false;
    }

    /////////////////////////////// Requery Code ///////////////////////////////

    /**
     * Returns a new <code>QueryRequest</code> for requery purposes.  Subclasses
     * may wish to override this to be more or less specific.<p>
     *
     * @return a new <tt>QueryRequest</tt> for making the requery
     * @throws CantResumeException if this doesn't know what to search for
     */
    @Override
    public synchronized QueryRequest newRequery()
            throws CantResumeException {

        String queryString = QueryUtils.createQueryString(getDefaultFileName());
        if (queryString == null || queryString.equals(""))
            throw new CantResumeException(getSaveFile().getName());
        else
            return queryRequestFactory.createQuery(queryString);

    }

    /**
     * Determines if the specified host is allowed to download.
     */
    protected boolean hostIsAllowed(RemoteFileDesc other) {
        // If this host is banned, don't add.
        if (!ipFilter.allow(other.getAddress()))
            return false;

        // See if we have already tried and failed with this location
        // This is only done if the location we're trying is an alternate..
        synchronized (altLock) {
            if (other.isFromAlternateLocation() && invalidAlts.contains(other)) {
                return false;
            }
        }
        return true;
    }

    private static boolean initDone = false; // used to init

    /**
     * Returns true if 'other' should be accepted as a new download location.
     */
    protected boolean allowAddition(RemoteFileDesc other) {
        if (!initDone) {
            synchronized (matcher) {
                matcher.setIgnoreCase(true);
                matcher.setIgnoreWhitespace(true);
                matcher.setCompareBackwards(true);
            }
            initDone = true;
        }

        // before doing expensive stuff, see if connection is even possible...
        if (other.getQuality() < 1) // I only want 2,3,4 star guys....
            return false;

        // get other info...
        final URN otherUrn = other.getSHA1Urn();
        final String otherName = other.getFileName();
        final long otherLength = other.getSize();

        synchronized (this) {
            long ourLength = getContentLength();

            if (ourLength != -1 && ourLength != otherLength)
                return false;

            if (otherUrn != null && getSha1Urn() != null)
                return otherUrn.equals(getSha1Urn());

            // compare to previously cached rfds
            for (RemoteFileDesc rfd : cachedRFDs) {
                final String thisName = rfd.getFileName();
                final long thisLength = rfd.getSize();

                // if they are similarly named and same length
                // do length check first, much less expensive.....
                if (otherLength == thisLength)
                    if (namesClose(otherName, thisName))
                        return true;
            }


            String resumeFileName = getResumeFileName();
            if (resumeFileName != null) {
                return namesClose(otherName, resumeFileName);
            }
        }
        return false;
    }

    /**
     * Returns a filename that the downloader can try to resume its downlkoad with.
     * This name is parsed from the incomplete file name. which is of the form
     * t-fileSize-fileName. The fileName portion of the string is parsed out.
     * Null is returned if getIncompleteFile returns null. If there are no hypens in the
     * IncompleteFileName the whole name is returned instead of just parsing out the fileName.
     */
    private String getResumeFileName() {
        String resumeFileName = null;
        if (getIncompleteFile() != null) {
            File incompleteFile = getIncompleteFile();
            resumeFileName = incompleteFile.getName();
            if (resumeFileName.contains("-")) {
                resumeFileName = resumeFileName.substring(resumeFileName.lastIndexOf("-", resumeFileName.length()));
            }
        }
        return resumeFileName;
    }

    private final boolean namesClose(final String one,
                                     final String two) {
        boolean retVal = false;

        // copied from TableLine...
        //Filenames close?  This is the most expensive test, so it should go
        //last.  Allow 10% edit difference in filenames or 6 characters,
        //whichever is smaller.
        int allowedDifferences = Math.round(Math.min(
                0.10f * ((QueryUtils.ripExtension(one)).length()),
                0.10f * ((QueryUtils.ripExtension(two)).length())));
        allowedDifferences = Math.min(allowedDifferences, 6);

        synchronized (matcher) {
            retVal = matcher.matches(matcher.process(one),
                    matcher.process(two),
                    allowedDifferences);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("MD.namesClose(): one = " + one);
            LOG.debug("MD.namesClose(): two = " + two);
            LOG.debug("MD.namesClose(): retVal = " + retVal);
        }

        return retVal;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#locationAdded(com.limegroup.gnutella.altlocs.AlternateLocation)
     */
    @Override
    public synchronized void locationAdded(AlternateLocation loc) {
        assert (loc.getSHA1Urn().equals(getSha1Urn()));

        if (LOG.isDebugEnabled()) {
            LOG.debug("alt loc added: " + loc);
        }

        long contentLength = -1L;
        if (loc instanceof DirectDHTAltLoc) {
            long fileSize = ((DirectDHTAltLoc) loc).getFileSize();

            // Compare the file size from the AltLoc with the contentLength
            // if possible.

            if (fileSize >= 0L) {
                // Get the current contentLength and compare it with
                // the file size from the AltLocValue
                synchronized (this) {
                    contentLength = getContentLength();
                    if (contentLength < 0L) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Using file size from AltLocValue: " + fileSize);
                        }
                        contentLength = fileSize;

                        if (contentLength <= MAX_FILE_SIZE) {
                            setContentLength(contentLength);
                        }
                    }
                }

                if (fileSize != contentLength) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("File sizes do not match: "
                                + fileSize + " vs. " + contentLength);
                    }
                    return;
                }
            }
        }

        contentLength = getContentLength();
        if (contentLength < 0L) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unknown file size: " + contentLength);
            }

            return;
        }

        if (contentLength > MAX_FILE_SIZE) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Content length is too big: " + contentLength);
            }
            return;
        }

        addDownload(loc.createRemoteFileDesc(contentLength, remoteFileDescFactory), false);
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#addDownload(com.limegroup.gnutella.RemoteFileDesc, boolean)
    */
    @Override
    public synchronized boolean addDownload(RemoteFileDesc rfd, boolean cache) {
        return addDownload(Collections.singleton(rfd), cache);
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#addDownload(java.util.Collection, boolean)
    */
    @Override
    public synchronized boolean addDownload(Collection<? extends RemoteFileDesc> c, boolean cache) {
        if (stopped || isCompleted())
            return false;

        List<RemoteFileDesc> l = new ArrayList<RemoteFileDesc>(c.size());
        for (RemoteFileDesc rfd : c) {
            if (allowAddition(rfd)) {
                if (hostIsAllowed(rfd)) {
                    l.add(rfd);
                }
            }
        }

        if (l.size() > 0) {
            return addDownloadForced(l, cache);
        } else {
            return false;
        }
    }

    /**
     * Like addDownload, but doesn't call allowAddition(..).
     * <p/>
     * If cache is false, the RFD is not added to allFiles, but is
     * added to 'files', the list of RFDs we will connect to.
     * <p/>
     * If the RFD matches one already in allFiles, the new one is
     * NOT added to allFiles, but IS added to the list of RFDs to connect to
     * if and only if a matching RFD is not currently in that list.
     * <p/>
     * This ALWAYS returns true, because the download is either allowed
     * or silently ignored (because we're already downloading or going to
     * attempt to download from the host described in the RFD).
     */
    protected synchronized boolean addDownloadForced(RemoteFileDesc rfd,
                                                     boolean cache) {
        // the singleton impl calls the collection impl and not the other way round,
        // so that the ping ranker gets a collection and only fires off one round of pinging
        return addDownloadForced(Collections.singleton(rfd), cache);
    }

    protected synchronized final boolean addDownloadForced(Collection<? extends RemoteFileDesc> c, boolean cache) {
        if (LOG.isDebugEnabled())
            LOG.debug("add download forced", new Exception());
        // create copy, argument might not be modifiable
        Set<RemoteFileDesc> copy = new HashSet<RemoteFileDesc>(c);
        // remove any rfds we're currently downloading from
        // TODO fberger hack
        if (currentRFDs != null) {
            copy.removeAll(currentRFDs);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("remaining new rfds: " + copy);
        }

        byte[] myGUID = applicationServices.getMyGUID();
        for (Iterator<RemoteFileDesc> iter = copy.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = iter.next();
            // do not download from ourselves
            if (rfd.isMe(myGUID)) {
                iter.remove();
                continue;
            }

            prepareRFD(rfd, cache);

            if (!canResolve(rfd) && !canConnect(rfd)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("rfd not connectable yet: " + rfd);
                }
                permanentRFDs.add(rfd);
                iter.remove();
            }
        }

        // TODO fberger null check is a hack
        if (ranker != null && ranker.addToPool(getContexts(copy))) {
            //   if(LOG.isTraceEnabled())
            //        LOG.trace("added rfds: " + c);
            LOG.debug("got new sources");
            receivedNewSources = true;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(copy + " not added");
            }
        }

        return true;
    }

    private boolean canResolve(RemoteFileDesc rfd) {
        return socketsManager.canResolve(rfd.getAddress());
    }

    private boolean canConnect(RemoteFileDesc rfd) {
        return socketsManager.canConnect(rfd.getAddress());
    }

    private void prepareRFD(RemoteFileDesc rfd, boolean cache) {
        if (getSha1Urn() == null && rfd.getSHA1Urn() != null) {
            setSha1Urn(rfd.getSHA1Urn());
            altLocManager.addListener(getSha1Urn(), this);
        }

        //add to allFiles for resume purposes if caching...
        if (cache || rfd.getAddress() instanceof PermanentAddress)
            cachedRFDs.add(rfd);
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#hasNewSources()
    */
    @Override
    public boolean hasNewSources() {
        return (!paused && receivedNewSources);
    }

    private Collection<RemoteFileDesc> getNewConnectableSources() {
        List<RemoteFileDesc> newlyConnectables = new ArrayList<RemoteFileDesc>();
        for (RemoteFileDesc rfd : permanentRFDs) {
            if (canResolve(rfd) || canConnect(rfd)) {
                newlyConnectables.add(rfd);
            } else {
                LOG.debug(rfd + " not connectable");
            }
        }
        return newlyConnectables;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#shouldBeRestarted()
     */
    @Override
    public boolean shouldBeRestarted() {
        DownloadState status = getState();
        return hasNewSources() ||
                (getRemainingStateTime() <= 0
                        && status != DownloadState.WAITING_FOR_GNET_RESULTS &&
                        status != DownloadState.QUERYING_DHT);
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#shouldBeRemoved()
    */
    @Override
    public boolean shouldBeRemoved() {
        return isCancelled() || isCompleted();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isQueuable()
    */
    @Override
    public boolean isQueuable() {
        return !isPaused();
    }

    ///////////////////////////////////////////////////////////////////////////

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#acceptDownload(java.lang.String, java.net.Socket, int, byte[])
     */
    @Override
    public boolean acceptDownload(String file, Socket socket, int index, byte[] clientGUID) {
        if (stopped)
            return false;

        HTTPConnectObserver observer = pushes.getHostFor(clientGUID, socket.getInetAddress().getHostAddress());
        if (observer != null)
            observer.handleConnect(socket);
        return observer != null;
    }

    @Override
    public void registerPushObserver(HTTPConnectObserver observer, PushDetails details) {
        pushes.addPushHost(details, observer);
    }

    @Override
    public void unregisterPushObserver(PushDetails details, boolean shutdown) {
        HTTPConnectObserver observer = pushes.getExactHostFor(details);
        if (observer != null && shutdown)
            observer.shutdown();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isCancelled()
    */
    @Override
    public boolean isCancelled() {
        return stopped;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#pause()
    */
    @Override
    public synchronized void pause() {
        // do not pause if already stopped.
        if (!stopped && !isCompleted()) {
            stop();
            stopped = false;
            paused = true;
            // if we're already inactive, mark us as paused immediately.
            if (isInactive())
                setState(DownloadState.PAUSED);
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isPaused()
    */
    @Override
    public boolean isPaused() {
        return paused == true;
    }


    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#isLaunchable()
    */
    @Override
    public boolean isLaunchable() {
        if(state == DownloadState.DANGEROUS ||
                state == DownloadState.THREAT_FOUND)
            return false;
        if(state == DownloadState.COMPLETE ||
                state == DownloadState.SCAN_FAILED)
            return true;
        return amountForPreview() > 0;
    }

    /**
     * Stops this download if it is not already stopped.
     *
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#stop()
     */
    @Override
    public void stop() {

        if (paused) {
            stopped = true;
            paused = false;
        }

        // make redundant calls to stop() fast
        // this change is pretty safe because stopped is only set in two
        // places - initialized and here.  so long as this is true, we know
        // this is safe.
        if (!(stopped || paused)) {

            LOG.debug("STOPPING ManagedDownloader");

            //This method is tricky.  Look carefully at run.  The most important
            //thing is to set the stopped flag.  That guarantees run will terminate
            //eventually.
            stopped = true;
            killAllWorkers();

            synchronized (this) {
                // must capture in local variable so the value doesn't become null
                // between if & contents of if.
                Thread dlMan = dloaderManagerThread;
                if (dlMan != null)
                    dlMan.interrupt();
                else
                    LOG.warn("MANAGER: no thread to interrupt");
            }
        }
    }

    /**
     * Kills all workers & shuts down all push waiters.
     */
    private void killAllWorkers() {
        List<DownloadWorker> workers = getAllWorkers();

        // cannot interrupt while iterating through the main list, because that
        // could cause ConcurrentMods.
        for (DownloadWorker doomed : workers)
            doomed.interrupt();

        List<HTTPConnectObserver> pushObservers = pushes.getAllAndClear();
        for (HTTPConnectObserver next : pushObservers)
            next.shutdown();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#informMesh(com.limegroup.gnutella.RemoteFileDesc, boolean)
    */
    @Override
    public synchronized void informMesh(RemoteFileDesc rfd, boolean good) {
        if (LOG.isDebugEnabled())
            LOG.debug("informing mesh that " + rfd + " is " + good);

        if (good)
            cachedRFDs.add(rfd);

        if (!rfd.isAltLocCapable())
            return;

        // Verify that this download has a hash.  If it does not,
        // we should not have been getting locations in the first place.
        assert getSha1Urn() != null : "null hash.";

        assert getSha1Urn().equals(rfd.getSHA1Urn()) : "wrong loc SHA1";

        AlternateLocation loc;
        try {
            loc = alternateLocationFactory.create(rfd);
        } catch (IOException iox) {
            return;
        }

        AlternateLocation local;

        // if this is a pushloc, update the proxies accordingly
        if (loc instanceof PushAltLoc) {

            // Note: we update the proxies of a clone in order not to lose the
            // original proxies
            local = loc.createClone();
            PushAltLoc ploc = (PushAltLoc) loc;

            // no need to notify mesh about pushlocs w/o any proxies
            if (ploc.getPushAddress().getProxies().isEmpty())
                return;

            ploc.updateProxies(good);
        } else
            local = loc;

        // and to the global collection
        if (good)
            altLocManager.add(loc, this);
        else
            altLocManager.remove(loc, this);

        // add to the downloaders
        for (DownloadWorker worker : getActiveWorkers()) {
            HTTPDownloader httpDloader = worker.getDownloader();
            RemoteFileDesc r = httpDloader.getRemoteFileDesc();

            // don't notify uploader of itself, == comparison should be correct too
            if (r.equals(rfd)) {
                continue;
            }

            //no need to send push altlocs to older uploaders
            if (local instanceof DirectAltLoc || httpDloader.wantsFalts()) {
                if (good)
                    httpDloader.addSuccessfulAltLoc(local);
                else
                    httpDloader.addFailedAltLoc(local);
            }
        }

        // add to the local collections
        synchronized (altLock) {
            if (good) {
                //check if validAlts contains loc to avoid duplicate stats, and
                //spurious count increments in the local
                //AlternateLocationCollections
                if (!validAlts.contains(local)) {
                    validAlts.add(local);
                }
            } else {
                validAlts.remove(local);
                invalidAlts.add(rfd);
                recentInvalidAlts.add(local);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#addPossibleSources(java.util.Collection)
     */
    @Override
    public synchronized void addPossibleSources(Collection<? extends RemoteFileDesc> c) {
        addDownload(c, false);
    }

    /**
     * Delegates requerying to the RequeryManager.
     */
    protected boolean canSendRequeryNow() {
        return requeryManager.canSendQueryNow();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#resume()
    */
    @Override
    public synchronized boolean resume() {
        //Ignore request if already in the download cycle.
        if (!isInactive())
            return false;

        // if we were waiting for the user to start us,
        // then try to send the requery.
        if (getState() == DownloadState.WAITING_FOR_USER) {
            requeryManager.activate();
        }

        // if any guys were busy, reduce their retry time to 0,
        // since the user really wants to resume right now.
        for (RemoteFileDesc rfd : cachedRFDs) {
            resetRfdContext(getContext(rfd));
        }

        if (paused) {
            paused = false;
            stopped = false;
        }

        // queue ourselves so we'll try and become active immediately
        setState(DownloadState.QUEUED);

        return true;
    }
    
    /** Resets the context of an RFD for new use. */
    protected void resetRfdContext(RemoteFileDescContext rfdContext) {
        rfdContext.setRetryAfter(0);
        rfdContext.setLastHttpCode(-1);
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getFile()
    */
    @Override
    public File getFile() {
        if (incompleteFile == null)
            return null;

        if (state == DownloadState.COMPLETE)
            return getSaveFile();
        else
            return incompleteFile;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getSHA1Urn()
    */
    @Override
    public URN getSha1Urn() {
        return downloadSHA1;
    }

    protected void setSha1Urn(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException("not sha1: " + sha1);
        if (downloadSHA1 != null && !sha1.equals(downloadSHA1))
            throw new IllegalStateException("sha1 already set to: " + downloadSHA1);
        this.downloadSHA1 = sha1;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getDownloadFragment()
    */
    @Override
    public File getDownloadFragment(ScanListener listener) {
        // We haven't started yet.
        if (incompleteFile == null)
            return null;

        if (state == DownloadState.CORRUPT_FILE) {
            // If the corrupt file exists, return it unless it's infected or
            // dangerous.
            File corrupt = corruptFile;
            if (corrupt == null)
                return null;
            if (isInfectedOrDangerous(corrupt, listener)) {
                corruptFile = null;
                return null;
            }
            return corrupt;
        } else if (state == DownloadState.COMPLETE ||
                state == DownloadState.SCAN_FAILED) {
            // If the download is complete, return the whole file.
            return getSaveFile();
        } else {
            // Create a copy of the beginning of the incomplete file. The copy
            // is needed because some programs, notably Windows Media Player,
            // attempt to grab exclusive file locks.
            File copy = new File(incompleteFile.getParent(),
                    IncompleteFileManager.PREVIEW_PREFIX
                    + incompleteFile.getName());
            // Get the size of the first block of the file. (Remember
            // that swarmed downloads don't always write in order.)
            long size = amountForPreview();
            if (size <= 0)
                return null;
            // Copy the first block, returning null if nothing was copied.
            if (FileUtils.copy(incompleteFile, size, copy) <= 0)
                return null;
            if (isInfectedOrDangerous(copy, listener)) {
                incompleteFile.delete();
                return null;
            }
            return copy;
        }
    }
    
    /**
     * Checks whether a file fragment is infected or dangerous. If the virus
     * scan fails, the user will be asked whether to preview the file anyway.
     * @param fragment the file to check
     * @param listener a listener to be informed of virus scan progress
     * @return true if the file cannot be previewed.
     */
    private boolean isInfectedOrDangerous(File fragment, ScanListener listener) {
        if(virusScanner.isEnabled()) {
            listener.scanStarted();
            try {
                boolean infected = isInfected(fragment);
                listener.scanStopped();
                if(infected)
                    return true;                
            } catch (VirusScanException e) {
                listener.scanStopped();
                if(promptAboutUnscannedPreview()) {
                    // The user chose to cancel the preview
                    return true;
                }
            }
        }
        return isDangerous(fragment);
    }

    /**
     * Returns the amount of the file written on disk that can be safely
     * previewed.
     */
    private synchronized long amountForPreview() {
        //And find the first block.
        if (commonOutFile == null)
            return 0; // trying to preview before incomplete file created

        return commonOutFile.getOffsetForPreview();
    }

    /**
     * Returns the save file from the default save directory.
     */
    @Override
    protected File getDefaultSaveFile() {
        String fileName = getDefaultFileName();
        Category category = null;
        if(fileName != null) {
            category = categoryManager.getCategoryForFilename(fileName);
        }
        return new File(SharingSettings.getSaveDirectory(category), fileName);
    }

    //////////////////////////// Core Downloading Logic /////////////////////

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#finish()
     */
    @Override
    public synchronized void finish() {
        if (getSha1Urn() != null)
            altLocManager.removeListener(getSha1Urn(), this);
        requeryManager.cleanUp();
        socketsManager.removeListener(connectivityChangeEventHandler);
    }

    /**
     * Actually does the download, finding duplicate files, trying all
     * locations, resuming, waiting, and retrying as necessary. Also takes care
     * of moving file from incomplete directory to save directory and adding
     * file to the library.  Called from dloadManagerThread.
     */
    protected DownloadState performDownload() {
        if (checkHosts()) {//files is global
            setState(DownloadState.GAVE_UP);
            return DownloadState.GAVE_UP;
        }

        // 1. initialize the download
        DownloadState status = initializeDownload();
        if (status == DownloadState.CONNECTING) {
            try {
                //2. Do the download
                try {
                    status = fireDownloadWorkers();//Exception may be thrown here.
                } finally {
                    //3. Close the file controlled by commonOutFile.
                    commonOutFile.close();
                }

                // 4. if all went well, save
                if (status == DownloadState.COMPLETE)
                    status = verifyAndSave();
                else if (LOG.isDebugEnabled())
                    LOG.debug("stopping early with status: " + status);

            } catch (InterruptedException e) {
                // nothing should interrupt except for a stop
                if (!stopped && !paused) {
                    ErrorService.error(e);
                } else {
                    switch(getState()) {
                    case DANGEROUS: // Detected during preview
                        status = DownloadState.DANGEROUS;
                        break;
                    case THREAT_FOUND: // Detected during preview
                        status = DownloadState.THREAT_FOUND;
                        break;
                    case CORRUPT_FILE: // Detected by a download worker
                        cleanupCorrupt(incompleteFile, getSaveFile().getName());
                        status = DownloadState.CORRUPT_FILE;
                        break;
                    default:
                        status = DownloadState.GAVE_UP;
                    }
                }
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("MANAGER: TAD2 returned: " + status);

        return status;
    }

    /**
     * Tries to initialize the download location and the verifying file.
     *
     * @return GAVE_UP if we had no sources, DISK_PROBLEM if such occurred,
     *         CONNECTING if we're ready to connect
     */
    protected DownloadState initializeDownload() {

        synchronized (this) {
            if (cachedRFDs.size() == 0 && !ranker.hasMore())
                return DownloadState.GAVE_UP;
        }

        try {
            initializeIncompleteFile();
            initializeVerifyingFile();
            openVerifyingFile();
        } catch (IOException iox) {
            reportDiskProblem(iox);
            return DownloadState.DISK_PROBLEM;
        }

        // initialize the HashTree
        if (getSha1Urn() != null)
            initializeHashTree();

        // load up the ranker with the hosts we know about
        initializeRanker(ranker);

        return DownloadState.CONNECTING;
    }

    /**
     * Verifies the completed file against the SHA1 hash, checks that it's not
     * dangerous, and saves it.
     *
     * @return {@link DownloadState#COMPLETE} if all went fine,
     *         {@link DownloadState#CORRUPT_FILE} if the hash does not match,
     *         {@link DownloadState#DANGEROUS} if the file is dangerous,
     *         {@link DownloadState#THREAT_FOUND} if the file is infected,
     *         {@link DownloadState#SCAN_FAILED} if the virus scan failed, or
     *         {@link DownloadState#DISK_PROBLEM} if there's a problem saving the file.
     * @throws InterruptedException if interrupted while waiting for the user's
     *                              response.
     */
    private DownloadState verifyAndSave() throws InterruptedException {

        // Scan the file for viruses
        if(virusScanner.isEnabled()) {
            setState(DownloadState.SCANNING);
        }
        
        DownloadState scanFailed = null;
        try {
            if(isInfected(incompleteFile))
                return DownloadState.THREAT_FOUND;
        } catch(VirusScanException e) {
            setAttribute(VirusEngine.DOWNLOAD_FAILURE_HINT, e.getDetail(), false);
            scanFailed = DownloadState.SCAN_FAILED;
        }
        
        // Check whether this is a dangerous file
        if(isDangerous(incompleteFile)) {
            return DownloadState.DANGEROUS;
        }

        // Find out the hash of the file and verify that its the same
        // as our hash.
        URN fileHash = scanForCorruption();
        if(fileHash == null) {
            cleanupCorrupt(incompleteFile, getSaveFile().getName());
            return DownloadState.CORRUPT_FILE;
        }

        // Save the file to disk.
        DownloadState saveState = saveFile(fileHash);
        if(saveState == DownloadState.COMPLETE && scanFailed != null)
            return scanFailed;
        return saveState;
    }

    /**
     * Returns true if the given file is dangerous, after stopping the download,
     * marking it as spam and deleting the file.
     */
    private boolean isDangerous(File file) {
        if(dangerousFileChecker.isDangerous(file)) {
            setState(DownloadState.DANGEROUS);
            // Mark the file as spam in future search results
            RemoteFileDesc[] type = new RemoteFileDesc[0];
            spamManager.handleUserMarkedSpam(cachedRFDs.toArray(type));
            // Stop the download and delete the file
            stop();
            library.remove(file);
            file.delete();
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the given file is infected, after stopping the download,
     * marking it as spam and deleting the file.
     */
    private boolean isInfected(File file) throws VirusScanException {
        if(virusScanner.isEnabled() && virusScanner.isInfected(file)) {
            setState(DownloadState.THREAT_FOUND);
            // Mark the file as spam in future search results
            RemoteFileDesc[] type = new RemoteFileDesc[0];
            spamManager.handleUserMarkedSpam(cachedRFDs.toArray(type));
            // Stop the download and delete the file
            stop();                
            library.remove(file);
            file.delete();
            return true;
        }
        return false;
    }

    /**
     * Scans the file for corruption, returning the hash of the file if it
     * matches the expected hash or if there is no expected hash, or null if
     * there is an error hashing the file or the hashes do not match.
     */
    private URN scanForCorruption() throws InterruptedException {
        URN fileHash = null;
        try {
            // let the user know we're hashing the file
            setState(DownloadState.HASHING);
            fileHash = URN.createSHA1Urn(incompleteFile);
        }
        catch (IOException ignored) {
        }

        // If we have no hash, we can't check at all.
        if (getSha1Urn() == null)
            return fileHash;

        // If they're equal, everything's fine.
        //if fileHash == null, it will be a mismatch
        if (getSha1Urn().equals(fileHash))
            return fileHash;

        if (LOG.isWarnEnabled()) {
            LOG.warn("hash verification problem, fileHash=" +
                    fileHash + ", ourHash=" + getSha1Urn());
        }
        cancelCorruptDownload();
        return null;
    }

    /**
     * checks the TT cache and if a good tree is present loads it
     */
    private void initializeHashTree() {
        HashTree tree = tigerTreeCache.get().getHashTree(getSha1Urn());

        // if we have a valid tree, update our chunk size and disable overlap checking
        if (tree != null && tree.isDepthGoodEnough()) {
            commonOutFile.setHashTree(tree);
        }
    }

    /**
     * Saves the file to disk.
     */
    protected DownloadState saveFile(URN fileHash) {
        // let the user know we're saving the file...
        setState(DownloadState.SAVING);
        File saveFile = getSaveFile();
        try {
            saveFile = getSuggestedSaveLocation(saveFile, incompleteFile);
            // Make sure we can write into the complete file's directory.
            if (!FileUtils.setWriteable(saveFile.getParentFile())) {
                reportDiskProblem("could not set file writeable " + getSaveFile().getParentFile());
                return DownloadState.DISK_PROBLEM;
            }
            if (!saveFile.equals(getSaveFile()))
                setSaveFile(saveFile.getParentFile(), saveFile.getName(), true);
        } catch (IOException e) {
            return DownloadState.DISK_PROBLEM;
        }

        //Delete target.  If target doesn't exist, this will fail silently.
        saveFile.delete();

        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.  
        //   If move is successful, we should remove the corresponding blocks
        //from the IncompleteFileManager, though this is not strictly necessary
        //because IFM.purge() is called frequently in DownloadManager.

        // First attempt to rename it.
        boolean success = FileUtils.forceRename(incompleteFile, saveFile);

        incompleteFileManager.removeEntry(incompleteFile);

        // If that didn't work, we're out of luck.
        if (!success) {
            reportDiskProblem("forceRename failed " + incompleteFile +
                    " -> " + saveFile);
            return DownloadState.DISK_PROBLEM;
        }

        // TODO: If exists & is in library, should change to fileChanged?

        //Add file to library.
        // first check if it conflicts with the saved dir....
        if (saveFile.exists())
            library.remove(saveFile);

        // add file hash to manager for fast lookup
        addFileHash(fileHash, saveFile);

        // determine where and how to share the file
        shareSavedFile(saveFile);

        return DownloadState.COMPLETE;
    }

    /**
     * Provides alternate file location based on new data obtained after downloading the file.
     * For example, could create a folder substructure and use a template based on ID3 information
     * for music.
     *
     * @param defaultSaveFile the current file location to save the incomplete download to
     * @return the location to save the actual download to
     * @throws IOException
     */
    protected File getSuggestedSaveLocation(File defaultSaveFile, File newDownloadFile) throws IOException {
        return defaultSaveFile;
    }

    /**
     * Add the URN of this file to the cache so that it won't
     * be hashed again when added to the library -- reduces
     * the time of the 'Saving File' state.
     */
    private void addFileHash(URN fileHash, File saveFile) {
        if (fileHash != null) {
            UrnSet urns = new UrnSet(fileHash);
            File file = saveFile;
            try {
                file = FileUtils.getCanonicalFile(saveFile);
            } catch (IOException ignored) {
            }
            // Always cache the URN, so results can lookup to see
            // if the file exists.
            URN ttroot = saveTreeHash(fileHash);
            if (ttroot != null)
                urns.add(ttroot);
            urnCache.addUrns(file, urns);
            library.add(file, getXMLDocuments());
        }
    }

    /**
     * Upon saving a downloaded file, if the file is to be shared the tiger tree should
     * be saved in order to speed up sharing the file across gnutella
     *
     * @param fileHash urn to save the tree of
     * @return the root of the tree
     */
    protected URN saveTreeHash(URN fileHash) {
        // save the trees!
        if (getSha1Urn() != null && getSha1Urn().equals(fileHash) && commonOutFile.getHashTree() != null) {
            tigerTreeCache.get().addHashTree(getSha1Urn(), commonOutFile.getHashTree());
            return commonOutFile.getHashTree().getTreeRootUrn();
        }
        return null;
    }

    /**
     * Shares the newly downloaded file
     */
    protected void shareSavedFile(File saveFile) {
        if (SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue()
                && !isFriendDownload)
            gnutellaFileCollection.add(saveFile, getXMLDocuments());
    }

    /**
     * Removes all entries for incompleteFile from incompleteFileManager
     * and attempts to rename incompleteFile to "CORRUPT-i-...".  Deletes
     * incompleteFile if rename fails.
     */
    private void cleanupCorrupt(File incFile, String name) {
        corruptFileBytes = getAmountRead();
        incompleteFileManager.removeEntry(incFile);

        //Try to rename the incomplete file to a new corrupt file in the same
        //directory (INCOMPLETE_DIRECTORY).
        boolean renamed = false;
        for (int i = 0; i < 10 && !renamed; i++) {
            corruptFile = new File(incFile.getParent(),
                    "CORRUPT-" + i + "-" + name);
            if (corruptFile.exists())
                continue;
            renamed = incFile.renameTo(corruptFile);
        }

        //Could not rename after ten attempts?  Delete.
        if (!renamed) {
            incFile.delete();
            this.corruptFile = null;
        }
    }

    /**
     * Initializes the verifying file.
     */
    private void openVerifyingFile() throws IOException {

        //need to get the VerifyingFile ready to write
        try {
            commonOutFile.open(incompleteFile);
        } catch (IOException e) {
            IOUtils.handleException(e, IOUtils.ErrorType.DOWNLOAD);
            throw e;
        }
    }

    /**
     * Starts a new Worker thread for the given <code>RemoteFileDesc</code>.
     */
    private void startWorker(final RemoteFileDescContext rfdContext) {
        DownloadWorker worker = downloadWorkerFactory.create(this, rfdContext, commonOutFile);
        synchronized (this) {
            _workers.add(worker);
            currentRFDs.add(rfdContext.getRemoteFileDesc());
        }
        worker.start();
    }
    
    @Override
    public synchronized void workerFinished(DownloadWorker finished) {
        if (LOG.isDebugEnabled())
            LOG.debug("worker " + finished + " finished.");
        removeWorker(finished);
        notify();
    }

    @Override
    public synchronized void workerStarted(DownloadWorker worker) {
        if (LOG.isDebugEnabled())
            LOG.debug("worker " + worker + " started.");
        if (!_workers.contains(worker))
            throw new IllegalStateException("attempting to start invalid worker: " + worker);

        setState(DownloadState.DOWNLOADING);
        addActiveWorker(worker);
    }

    @Override
    public void workerFailed(DownloadWorker failed) {
    }

    synchronized void removeWorker(DownloadWorker worker) {
        boolean rA = removeActiveWorker(worker);
        workerFailed(worker); // make sure its out of the chat list & browse list
        boolean rW = _workers.remove(worker);
        if (rA && !rW)
            throw new IllegalStateException("active removed but not in workers");
    }

    @Override
    public synchronized boolean removeActiveWorker(DownloadWorker worker) {
        currentRFDs.remove(worker.getRFD());
        List<DownloadWorker> l = new ArrayList<DownloadWorker>(getActiveWorkers());
        boolean removed = l.remove(worker);
        _activeWorkers = Collections.unmodifiableList(l);
        return removed;
    }

    synchronized void addActiveWorker(DownloadWorker worker) {
        // only add if not already added.
        if (!getActiveWorkers().contains(worker)) {
            List<DownloadWorker> l = new ArrayList<DownloadWorker>(getActiveWorkers());
            l.add(worker);
            _activeWorkers = Collections.unmodifiableList(l);
        }
    }

    synchronized String getWorkersInfo() {
        String workerState = "";
        for (DownloadWorker worker : _workers)
            workerState += worker.getInfo();
        return workerState;
    }

    @Override
    public Set<AlternateLocation> getValidAlts() {
        synchronized (altLock) {
            Set<AlternateLocation> ret;

            if (validAlts != null) {
                ret = new HashSet<AlternateLocation>(validAlts);
            } else
                ret = Collections.emptySet();

            return ret;
        }
    }


    @Override
    public Set<AlternateLocation> getInvalidAlts() {
        synchronized (altLock) {
            Set<AlternateLocation> ret;
            if (invalidAlts != null) {
                ret = new HashSet<AlternateLocation>(recentInvalidAlts);
            } else {
                ret = Collections.emptySet();
            }

            return ret;
        }
    }

    /**
     * Like tryDownloads2, but does not deal with the library, cleaning
     * up corrupt files, etc.  Caller should look at corruptState to
     * determine if the file is corrupted; a return value of COMPLETE
     * does not mean no corruptions where encountered.
     *
     * @return COMPLETE if a file was successfully downloaded
     *         WAITING_FOR_RETRY if no file was downloaded, but it makes sense
     *         to try again later because some hosts reported busy.
     *         The caller should usually wait before retrying.
     *         GAVE_UP the download attempt failed, and there are
     *         no more locations to try.
     *         COULDNT_MOVE_TO_LIBRARY couldn't write the incomplete file
     * @throws InterruptedException if the someone stop()'ed this download.
     *                              stop() was called either because the user killed the download or
     *                              a corruption was detected and they chose to kill and discard the
     *                              download.  Calls to resume() do not result in InterruptedException.
     */
    private DownloadState fireDownloadWorkers() throws InterruptedException {
        LOG.trace("MANAGER: entered fireDownloadWorkers");

        //While there is still an unfinished region of the file...
        while (true) {
            if (stopped || paused) {
                LOG.warn("MANAGER: terminating because of stop|pause");
                throw new InterruptedException();
            }

            // are we just about to finish downloading the file?

            //   LOG.debug("About to wait for pending if needed");

            try {
                commonOutFile.waitForPendingIfNeeded();
            } catch (DiskException dio) {
                if (stopped || paused) {
                    LOG.warn("MANAGER: terminating because of stop|pause");
                    throw new InterruptedException();
                }
                stop();
                reportDiskProblem(dio);
                return DownloadState.DISK_PROBLEM;
            }

            //  LOG.debug("Finished waiting for pending");

            // Finished.
            if (commonOutFile.isComplete()) {
                killAllWorkers();

                LOG.trace("MANAGER: terminating because of completion");
                return DownloadState.COMPLETE;
            }

            synchronized (this) {
                // if everybody we know about is busy (or we don't know about anybody)
                // and we're not downloading from anybody - terminate the download.
                if (_workers.size() == 0 && !ranker.hasUsableHosts()) {

                    receivedNewSources = false;

                    if (ranker.calculateWaitTime() > 0) {
                        LOG.trace("MANAGER: terminating with busy");
                        return DownloadState.BUSY;
                    } else {
                        LOG.trace("MANAGER: terminating w/o hope");
                        return DownloadState.GAVE_UP;
                    }
                }

                if (LOG.isDebugEnabled())
                    LOG.debug("MANAGER: kicking off workers.  " +
                            "state: " + getState() +
                            ", allWorkers: " + _workers.size() +
                            ", activeWorkers: " + _activeWorkers.size() +
                            ", queuedWorkers: " + _queuedWorkers.size() +
                            ", swarm cap: " + getSwarmCapacity()
                    );
                //+ ", allActive: " + _activeWorkers.toString());

                //OK. We are going to create a thread for each RFD. The policy for
                //the worker threads is to have one more thread than the max swarm
                //limit, which if successfully starts downloading or gets a better
                //queued slot than some other worker kills the lowest worker in some
                // remote queue.
                if (shouldStartWorker()) {
                    // see if we need to update our ranker
                    ranker = getSourceRanker(ranker);

                    RemoteFileDescContext rfd = ranker.getBest();

                    if (rfd != null) {
                        // If the rfd was busy, that means all possible RFDs
                        // are busy - store for later
                        if (rfd.isBusy()) {
                            addToRanker(rfd);
                        } else {
                            if (LOG.isDebugEnabled())
                                LOG.debug("Staring worker for RFD: " + rfd);
                            startWorker(rfd);
                        }
                    }

                } else if (LOG.isDebugEnabled())
                    LOG.debug("no blocks but can't steal - sleeping."); //  parts required: " + commonOutFile.listMissingPieces());

                //wait for a notification before we continue.
                try {
                    //if no workers notify in a while, iterate. This is a problem
                    //for stalled downloaders which will never notify. So if we
                    //wait without a timeout, we could wait forever.
                    this.wait(DownloadSettings.WORKER_INTERVAL.getValue()); // note that this relinquishes the lock
                } catch (InterruptedException ignored) {
                }
            }
        }//end of while
    }

    /**
     * Retrieves the appropriate source ranker (or returns the current one).
     */
    protected SourceRanker getSourceRanker(SourceRanker ranker) {
        return sourceRankerFactory.getAppropriateRanker(ranker);
    }

    /**
     * @return if we should start another worker - means we have more to download,
     *         have not reached our swarm capacity and the ranker has something to offer
     *         or we have some rfds to re-try
     */
    private boolean shouldStartWorker() {
        return (commonOutFile.hasFreeBlocksToAssign() > 0 || victimsExist()) &&
                ((_workers.size() - _queuedWorkers.size()) < getSwarmCapacity()) &&
                ranker.hasMore();
    }

    /**
     * Returns true if a new worker should be started because an existing
     * one is going below MIN_ACCEPTABLE_SPEED.
     *
     * @return true if a new worker should be started that would steal.
     */
    private boolean victimsExist() {
        if (_workers.isEmpty())
            return false;

        // there needs to be at least one slow worker.
        for (DownloadWorker victim : _workers) {
            if (!victim.isStealing() && victim.isSlow())
                return true;
        }

        return false;
    }

    @Override
    public synchronized void addToRanker(RemoteFileDescContext rfd) {
        if (ranker != null)
            ranker.addToPool(rfd);
    }

    @Override
    public synchronized void forgetRFD(RemoteFileDesc rfd) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("remove rfd: " + rfd);
        }
        // don't remove permanent addresses
        if (rfd.getAddress() instanceof PermanentAddress) {
            permanentRFDs.add(rfd);
            return;
        }
        if (cachedRFDs.remove(rfd) && cachedRFDs.isEmpty()) {
            // remember our last RFD
            cachedRFDs.add(rfd);
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getNumberOfAlternateLocations()
    */
    public int getNumberOfAlternateLocations() {
        synchronized (altLock) {
            if (validAlts == null) return 0;
            return validAlts.size();
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#getQueuedHostCount()
     */
    @Override
    public synchronized int getQueuedHostCount() {
        return _queuedWorkers.size();
    }

    int getSwarmCapacity() {
        //max measured download speed in kilobits.
        int capacity = bandwidthCollector.getMaxMeasuredTotalDownloadBandwidth() * 8;
        if(capacity <= 0) {
            //default to cable speed if no measurements taken yet
            //assume larger than modem to get better measurement stats.
            capacity = SpeedConstants.CABLE_SPEED_INT;
        }

        //max download speec in kilobits
        int maxDownloadSpeed = DownloadSettings.MAX_DOWNLOAD_SPEED.getValue() / 1024 * 8;
        if(DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.getValue() && capacity > maxDownloadSpeed) {
            capacity = maxDownloadSpeed;
        }
        
        if (capacity <= SpeedConstants.MODEM_SPEED_INT) //modems swarm = 2
            return SpeedConstants.MODEM_SWARM + 2; // PRO FEATURE
        else if (capacity <= SpeedConstants.T1_SPEED_INT) //DSL, Cable, T1 = 6
            return SpeedConstants.T1_SWARM + 2; // PRO FEATURE
        else // T3
            return SpeedConstants.T3_SWARM + 4; // PRO FEATURE
    }

    @Override
    public void cancelCorruptDownload() {
        setState(DownloadState.CORRUPT_FILE);

        // unshare the file
        library.remove(incompleteFile);

        // purge the tree
        if(getSha1Urn() != null)
            tigerTreeCache.get().purgeTree(getSha1Urn());
        commonOutFile.setHashTree(null);
        
        stop();
        incompleteFile.delete();
    }
    
    private boolean promptAboutUnscannedPreview() {
        downloadCallback.promptAboutUnscannedPreview(this);
        return discardUnscannedPreview;
    }

    @Override
    public void discardUnscannedPreview(boolean delete) {
        discardUnscannedPreview = delete;
    }


    /**
     * Returns the union of all XML metadata documents from all hosts.
     */
    private synchronized List<LimeXMLDocument> getXMLDocuments() {
        //TODO: we don't actually union here.  Also, should we only consider
        //those locations that we download from?
        List<LimeXMLDocument> allDocs = new ArrayList<LimeXMLDocument>();

        // get all docs possible
        for (RemoteFileDesc rfd : cachedRFDs) {
            LimeXMLDocument doc = rfd.getXMLDocument();
            if (doc != null)
                allDocs.add(doc);
        }
        return allDocs;
    }

    /////////////////////////////Display Variables////////////////////////////

    @Override
    public void setState(DownloadState newState) {
        setState(newState, Long.MAX_VALUE);
    }

    /**
     * Sets this' state.
     *
     * @param newState the state we're entering, which MUST be one of the
     *                 constants defined in Downloader
     * @param time     the time we expect to state in this state, in
     *                 milliseconds.
     */
    void setState(DownloadState newState, long time) {
        DownloadState oldState = null;
        synchronized (this) {
            oldState = this.state;
            this.state = newState;
            this.stateTime = System.currentTimeMillis() + time;
        }
        if (oldState != newState) {
            listeners.broadcast(new DownloadStateEvent(ManagedDownloaderImpl.this, newState));
        }
    }

    /**
     * Sets this' state to newState if the current state is 'oldState'.
     *
     * @return true if the state changed, false otherwise
     */
    synchronized boolean setStateIfExistingStateIs(DownloadState newState, DownloadState oldState) {
        if (getState() == oldState) {
            setState(newState);
            return true;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getQueryGUID()
    */
    @Override
    public GUID getQueryGUID() {
        return this.originalQueryGUID;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#getState()
     */
    @Override
    public synchronized DownloadState getState() {
        return state;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#getRemainingStateTime()
     */
    @Override
    public synchronized int getRemainingStateTime() {
        long remaining;
        switch (state) {
            case CONNECTING:
            case BUSY:
            case ITERATIVE_GUESSING:
            case WAITING_FOR_CONNECTIONS:
                remaining = stateTime - System.currentTimeMillis();
                return (int) Math.ceil(Math.max(remaining, 0) / 1000f);
            case WAITING_FOR_GNET_RESULTS:
            case QUERYING_DHT:
                return (int) Math.ceil(Math.max(requeryManager.getTimeLeftInQuery(), 0) / 1000f);
            case QUEUED:
                return 0;
            default:
                return Integer.MAX_VALUE;
        }
    }

    /**
     * Certain subclasses would like to know whether we have at least one good
     * RFD.
     */
    protected synchronized boolean hasRFD() {
        return (cachedRFDs != null && !cachedRFDs.isEmpty());
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getContentLength()
    */
    @Override
    public synchronized long getContentLength() {
        return contentLength;
    }

    protected synchronized void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#getAmountRead()
     */
    @Override
    public long getAmountRead() {
        VerifyingFile ourFile;
        synchronized (this) {
            if (state == DownloadState.CORRUPT_FILE)
                return corruptFileBytes;
            else if (state == DownloadState.HASHING) {
                if (incompleteFile == null)
                    return 0;
                else
                    return URN.getHashingProgress(incompleteFile);
            } else {
                ourFile = commonOutFile;
            }
        }

        return ourFile == null ? 0 : ourFile.getBlockSize();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getAmountPending()
    */
    @Override
    public int getAmountPending() {
        VerifyingFile ourFile;
        synchronized (this) {
            ourFile = commonOutFile;
        }

        return (int) (ourFile == null ? 0 : ourFile.getPendingSize());
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getNumHosts()
    */
    @Override
    public int getNumHosts() {
        return _activeWorkers.size();
    }

    @Override
    public List<Address> getSourcesAsAddresses() {
        List<Address> sources = new ArrayList<Address>(_activeWorkers.size());
        for (DownloadWorker worker : _activeWorkers) {
            sources.add(worker.getRFD().getAddress());
        }
        return sources;
    }
    
    @Override
    public List<SourceInfo> getSourcesDetails() {
        synchronized(this) {
            List<SourceInfo> sources = new ArrayList<SourceInfo>(_workers.size());
            for(DownloadWorker worker : _workers) {
                sources.add(new SourceDetails(worker));
            }
            return sources;
        }
    }
    
    private synchronized IntervalSet getAvailablePieces() {
        IntervalSet available = new IntervalSet();
        long length = getContentLength();
        for(DownloadWorker worker : _workers) {
            RemoteFileDescContext context = getContext(worker.getRFD());
            if(context.isPartialSource()) {
                available.add(context.getAvailableRanges());                
            } else if(length > 0) {
                return IntervalSet.createSingletonSet(0, length);
            }
        }
        return available;
    }
    
    @Override
    public DownloadPiecesInfo getPieceInfo() {
        IntervalSet written;
        IntervalSet active;
        VerifyingFile vfile = commonOutFile;
        if(vfile == null) {
            written = IntervalSet.createSingletonSet(0, 0);
            active = IntervalSet.createSingletonSet(0, 0);
        } else {
            written = vfile.getDownloadedBlocks();
            active = vfile.getLeasedBlocks();
        }
        return new GnutellaPieceInfo(written, active, getAvailablePieces(), getChunkSize(), getContentLength());
    }

    @Override
    public synchronized List<RemoteFileDesc> getRemoteFileDescs() {
        return new ArrayList<RemoteFileDesc>(currentRFDs);
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getQueuePosition()
    */
    @Override
    public synchronized int getQueuePosition() {
        return queuePosition;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getNumDownloaders()
    */
    @Override
    public int getNumDownloaders() {
        return getActiveWorkers().size() + getQueuedWorkers().size();
    }

    /**
     * Returns the list of all active workers.
     */
    @Override
    public List<DownloadWorker> getActiveWorkers() {
        return _activeWorkers;
    }

    /**
     * Returns a copy of the list of all workers.
     */
    @Override
    public synchronized List<DownloadWorker> getAllWorkers() {
        return new ArrayList<DownloadWorker>(_workers);
    }

    @Override
    public void removeQueuedWorker(DownloadWorker unQueued) {
        if (getQueuedWorkers().containsKey(unQueued)) {
            synchronized (this) {
                Map<DownloadWorker, Integer> m = new HashMap<DownloadWorker, Integer>(getQueuedWorkers());
                m.remove(unQueued);
                _queuedWorkers = Collections.unmodifiableMap(m);
            }
        }
    }

    private synchronized void addQueuedWorker(DownloadWorker queued, int position) {
        if (LOG.isDebugEnabled())
            LOG.debug("adding queued worker " + queued + " at position " + position +
                    " current queued workers:\n" + _queuedWorkers);

        if (!_workers.contains(queued))
            throw new IllegalStateException("attempting to queue invalid worker: " + queued);

        if (position < queuePosition) {
            queuePosition = position;
        }
        Map<DownloadWorker, Integer> m = new HashMap<DownloadWorker, Integer>(getQueuedWorkers());
        m.put(queued, new Integer(position));
        _queuedWorkers = Collections.unmodifiableMap(m);
    }

    @Override
    public Map<DownloadWorker, Integer> getQueuedWorkers() {
        return _queuedWorkers;
    }

    int getWorkerQueuePosition(DownloadWorker worker) {
        Integer i = getQueuedWorkers().get(worker);
        return i == null ? -1 : i.intValue();
    }

    /**
     * Interrupts a remotely queued worker if we this status is connected,
     * or if the status is queued and our queue position is better than
     * an existing queued status.
     *
     * @return true if this worker should be kept around, false otherwise --
     *         explicitly, there is no need to kill any queued workers, or if the DownloadWorker
     *         is already in the queuedWorkers, or if we did kill a worker whose position is
     *         worse than this worker.
     */
    @Override
    public synchronized boolean killQueuedIfNecessary(DownloadWorker worker, int queuePos) {
        if (LOG.isDebugEnabled())
            LOG.debug("deciding whether to kill a queued host for (" + queuePos + ") worker " + worker);

        //Either I am queued or downloading, find the highest queued thread
        DownloadWorker doomed = null;

        // No replacement required?...
        int numDownloaders = getNumDownloaders();
        int swarmCapacity = getSwarmCapacity();

        if (numDownloaders <= swarmCapacity && queuePos == -1) {
            return true;
        }

        // Already Queued?...
        if (_queuedWorkers.containsKey(worker) && queuePos > -1) {
            // update position
            addQueuedWorker(worker, queuePos);
            return true;
        }

        if (numDownloaders >= swarmCapacity) {
            // Search for the queued thread with a slot worse than ours.
            int highest = queuePos; // -1 if we aren't queued.
            for (Map.Entry<DownloadWorker, Integer> current : _queuedWorkers.entrySet()) {
                int currQueue = current.getValue().intValue();
                if (currQueue > highest) {
                    doomed = current.getKey();
                    highest = currQueue;
                }
            }

            // No one worse than us?... kill us.
            if (doomed == null) {
                LOG.debug("not queueing myself");
                return false;
            } else if (LOG.isDebugEnabled())
                LOG.debug("will replace " + doomed);

            //OK. let's kill this guy 
            doomed.interrupt();
        }

        //OK. I should add myself to queuedWorkers if I am queued
        if (queuePos > -1)
            addQueuedWorker(worker, queuePos);

        return true;

    }

    @Override
    public void hashTreeRead(HashTree tree) {
        boolean set = false;
        synchronized (commonOutFile) {
            commonOutFile.setHashTreeRequested(false);
            if (LOG.isDebugEnabled())
                LOG.debug("Downloaded tree: " + tree);
            if (tree != null) {
                HashTree oldTree = commonOutFile.getHashTree();
                if (tree.isBetterTree(oldTree)) {
                    set = commonOutFile.setHashTree(tree);
                }
            }
        }

        if (set && tree != null) { // warning?
            URN sha1 = getSha1Urn();
            URN ttroot = tree.getTreeRootUrn();
            tigerTreeCache.get().addRoot(sha1, ttroot);
            List<FileDesc> fds = library.getFileDescsMatching(sha1);
			for(FileDesc fd : fds) {
                fd.addUrn(ttroot);
            }

        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#measureBandwidth()
     */
    @Override
    public void measureBandwidth() {
        float currentTotal = 0f;
        boolean c = false;
        for (DownloadWorker worker : getActiveWorkers()) {
            c = true;
            BandwidthTracker dloader = worker.getDownloader();
            dloader.measureBandwidth();
            currentTotal += dloader.getAverageBandwidth();
        }
        if (c) {
            synchronized (this) {
                averageBandwidth = ((averageBandwidth * numMeasures) + currentTotal)
                        / ++numMeasures;
            }
        }
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getMeasuredBandwidth()
    */
    @Override
    public float getMeasuredBandwidth() {
        float retVal = 0f;
        for (DownloadWorker worker : getActiveWorkers()) {
            BandwidthTracker dloader = worker.getDownloader();
            float curr = 0;
            try {
                curr = dloader.getMeasuredBandwidth();
            } catch (InsufficientDataException ide) {
                curr = 0;
            }
            retVal += curr;
        }
        return retVal;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getAverageBandwidth()
    */
    @Override
    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getAmountVerified()
    */
    @Override
    public long getAmountVerified() {
        VerifyingFile ourFile;
        synchronized (this) {
            ourFile = commonOutFile;
        }
        return ourFile == null ? 0 : ourFile.getVerifiedBlockSize();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getAmountLost()
    */
    @Override
    public long getAmountLost() {
        VerifyingFile ourFile;
        synchronized (this) {
            ourFile = commonOutFile;
        }
        return ourFile == null ? 0 : ourFile.getAmountLost();
    }

    /* (non-Javadoc)
    * @see com.limegroup.gnutella.downloader.ManagedDownloader#getChunkSize()
    */
    @Override
    public int getChunkSize() {
        VerifyingFile ourFile;
        synchronized (this) {
            ourFile = commonOutFile;
        }
        return ourFile != null ? ourFile.getChunkSize() : VerifyingFile.DEFAULT_CHUNK_SIZE;
    }

    /**
     * @return true if the table we remembered from previous sessions, contains
     *         Takes into consideration when the download is taking place - ie the
     *         timebomb condition. Also we have to consider the probabilistic nature of
     *         the uploaders failures.
     */
    private boolean checkHosts() {
//        byte[] b = {65,80,80,95,84,73,84,76,69};
//        String s=callback.getHostValue(new String(b));
//        if(s==null)
//            return false;
//        s = s.substring(0,8);
        String s = "LimeWire";
        if (s.hashCode() == -1473607375 &&
                System.currentTimeMillis() > 1029003393697l &&
                Math.random() > 0.5f)
            return true;
        return false;
    }

    /**
     * Increments the count of tried hosts
     */
    @Override
    public synchronized void incrementTriedHostsCount() {
        ++triedHosts;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ManagedDownloader#getDownloadType()
     */
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.MANAGED;
    }

    private class RequeryListenerImpl implements RequeryListener {
        public QueryRequest createQuery() {
            try {
                return newRequery();
            } catch (CantResumeException cre) {
                return null;
            }
        }

        public URN getSHA1Urn() {
            return ManagedDownloaderImpl.this.getSha1Urn();
        }

        public void lookupFinished(QueryType queryType) {
            switch (queryType) {
                case DHT:
                    setStateIfExistingStateIs(DownloadState.GAVE_UP, DownloadState.QUERYING_DHT);
                    break;
                case GNUTELLA:
                    setState(DownloadState.GAVE_UP);
                    break;
                default:
                    throw new IllegalStateException("invalid type: " + queryType);
            }

        }

        public void lookupPending(QueryType queryType, int length) {
            switch (queryType) {
                case GNUTELLA:
                    setState(DownloadState.WAITING_FOR_CONNECTIONS, length);
                    break;
                default:
                    throw new IllegalStateException("invalid type: " + queryType);
            }

        }

        public void lookupStarted(QueryType queryType, long length) {
            switch (queryType) {
                case DHT:
                    setState(DownloadState.QUERYING_DHT, length);
                    break;
                case GNUTELLA:
                    setState(DownloadState.WAITING_FOR_GNET_RESULTS, length);
                    break;
                default:
                    throw new IllegalStateException("invalid type: " + queryType);
            }
        }
    }

    protected synchronized void setIncompleteFile(File incompleteFile) {
        this.incompleteFile = incompleteFile;
    }

    protected synchronized File getIncompleteFile() {
        return incompleteFile;
    }

    @Override
    protected DownloadMemento createMemento() {
        return new GnutellaDownloadMementoImpl();
    }

    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        GnutellaDownloadMemento gmem = (GnutellaDownloadMemento) memento;
        setContentLength(gmem.getContentLength());
        if (gmem.getSha1Urn() != null)
            setSha1Urn(gmem.getSha1Urn());
        setIncompleteFile(gmem.getIncompleteFile());
        if (gmem.getRemoteHosts().isEmpty() && gmem.getDefaultFileName() == null)
            throw new InvalidDataException("must have a name!");
        addInitialSources(toRfds(gmem.getRemoteHosts()), gmem.getDefaultFileName());

        if (getIncompleteFile() != null) {
            incompleteFileManager.initEntry(getIncompleteFile(), gmem.getSavedBlocks(), getSha1Urn(), shouldPublishIFD());
        }
    }

    /**
     * Returns true if this download's IFD should be published as sharable.
     */
    protected boolean shouldPublishIFD() {
        return true;
    }

    @Override
    protected void fillInMemento(DownloadMemento memento) {
        GnutellaDownloadMemento gmem = (GnutellaDownloadMemento) memento;
        super.fillInMemento(gmem);
        gmem.setContentLength(getContentLength());
        gmem.setSha1Urn(getSha1Urn());
        if (commonOutFile != null)
            gmem.setSavedBlocks(commonOutFile.getSerializableBlocks());
        gmem.setIncompleteFile(getIncompleteFile());
        gmem.setRemoteHosts(getRemoteHostMementos());
    }

    private Set<RemoteHostMemento> getRemoteHostMementos() {
        Set<RemoteHostMemento> mementos = new HashSet<RemoteHostMemento>(cachedRFDs.size());
        for (RemoteFileDesc rfd : cachedRFDs) {
            mementos.add(rfd.toMemento());
        }
        return mementos;
    }

    private Collection<RemoteFileDesc> toRfds(Collection<? extends RemoteHostMemento> mementos)
            throws InvalidDataException {
        if (mementos == null)
            return Collections.emptyList();

        List<RemoteFileDesc> rfds = new ArrayList<RemoteFileDesc>(mementos.size());
        for (RemoteHostMemento memento : mementos) {
            rfds.add(remoteFileDescFactory.createFromMemento(memento));
        }
        return rfds;
    }

    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        return listeners.removeListener(listener);
    }

    /**
     * @return the source ranker being used or null if none is set.
     */
    protected synchronized SourceRanker getSourceRanker() {
        return ranker;
    }

    private RemoteFileDescContext getContext(RemoteFileDesc rfd) {
        RemoteFileDescContext context = remoteFileDescToContext.get(rfd);
        if (context != null) {
            return context;
        }
        RemoteFileDescContext newContext = new RemoteFileDescContext(rfd);
        context = remoteFileDescToContext.putIfAbsent(rfd, newContext);
        if (context != null) {
            return context;
        }
        return newContext;
    }

    private Collection<RemoteFileDescContext> getContexts(Collection<? extends RemoteFileDesc> rfds) {
        List<RemoteFileDescContext> contexts = new ArrayList<RemoteFileDescContext>();
        for (RemoteFileDesc rfd : rfds) {
            contexts.add(getContext(rfd));
        }
        return contexts;
    }

    private class ConnectivityChangeEventHandler implements EventListener<ConnectivityChangeEvent> {

        @Override
        public void handleEvent(ConnectivityChangeEvent event) {
            LOG.debug("connectivity change");
            Collection<RemoteFileDesc> newConnectableSources = getNewConnectableSources();
            if (LOG.isDebugEnabled()) {
                LOG.debug("new connectables: " + newConnectableSources);
                LOG.debug("all non-connectables" + permanentRFDs);
            }
            if (!newConnectableSources.isEmpty()) {
                receivedNewSources = true;
                addDownloadForced(newConnectableSources, true);
            }
        }

    }

    @Override
    public void deleteIncompleteFiles() {
        //TODO assert that complete or aborted?
        File incompleteFile = getIncompleteFile();

        if (incompleteFile != null) {
            // Remove file from incomplete file list.
            incompleteFileManager.removeEntry(incompleteFile);
            FileUtils.delete(incompleteFile, false);
        }
    }
}
