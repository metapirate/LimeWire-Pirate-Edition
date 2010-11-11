package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager;
import org.limewire.net.TLSManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.statemachine.IOStateObserver;

import com.google.inject.Provider;
import com.limegroup.gnutella.AssertFailure;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.util.MultiShutdownable;

/**
 * Performs the logic of downloading a file from a single host.
 */
public class DownloadWorker {
    /*
     * A worker follows these steps:
     * 
     * CONNECTING: Establish a TCP connection to the host in the RFD. If unable
     * to connect, exit. If able to connect, continue processing the download.
     * 
     * This step is characterized by following: establishConnection ->
     * [push|direct] -> startDownload
     * 
     * DOWNLOADING: The download enters a state machine, which loops forever
     * until either an error occurs or the download is finished. The flow is
     * similar to:
     * 
     * while(true) { if(can request thex) { request and download thex if(needs
     * to consume body) consume body assign and request if(ready to download) do
     * download else if(queued) wait queue time else exit }
     * 
     * except it is performed asynchronously via a state machine.
     * 
     * The states are entered via httpLoop and progress through calls of
     * incrementState(ConnectionStatus). It moves through the following steps: -
     * requestThexIfNeeded - downloadThexIfNeeded - consumeBodyIfNeeded -
     * assignAndRequest - assignWhite or assignGrey - completeAssignAndRequest -
     * completeAssignWhite or completeAssignGrey - httpRequestFinished -
     * beginDownload, handleQueued, or finishHttpLoop
     * 
     * Each 'if needed' method can return true or false. True means that an
     * operation is being performed and upon success or failure the state
     * machine will continue. Success generally calls incrementState again to
     * move to the next state. Failure generally calls finishHttpLoop to stop
     * the download. False means the operation does not need to be performed and
     * the next state can be immediately processed.
     * 
     * The assignAndRequest step has two parts: a. Grab a part of the file to
     * download. If there is unclaimed area on the file grab that, otherwise try
     * to steal claimed area from another worker b. Send http headers to the
     * uploader on the tcp connection established in step 1. The uploader may or
     * may not be able to upload at this time. If the uploader can't upload,
     * it's important that the leased area be restored to the state they were in
     * before we started trying. However, if the http handshaking was
     * successful, the downloader can keep the part it obtained.
     * 
     * Both assignWhite & assignGrey will schedule the HTTP request (part b
     * above) and continue afterwards by calling completeAssignAndRequest even
     * if the request had an exception. This is done so that any read headers
     * can be parsed and accounted for, such as alternate locations.
     * 
     * PUSH DOWNLOADS NOTE: For push downloads, the acceptDownload(file, Socket,
     * index, clientGUI) method of ManagedDownloader is called from
     * DownloadManager. This method needs to notify the appropriate downloader
     * so that it can use the socket.
     * 
     * When establishConnection() realizes that it needs to do a push, it gives
     * the manager its HTTPConnectObserver (a ConnectObserver) and a mini-RFD.
     * When the manager is notified that a push was accepted (via
     * acceptDownload) with that mini-RFD, it will notify the
     * HTTPConnectObserver using handleConnect(Socket).
     * 
     * Note: The establishConnection method schedules a Runnable to remove the
     * observer in a short amount of time (about 9 seconds). If the observer
     * hasn't already connected, it assumes the push failed and terminates by
     * calling shutdown().
     * 
     * If the push was done by a multicast RFD, a failure to connect will
     * proceed to trying a direct connection. Otherwise (the push was done
     * because no direct connect was possible, or because a direct connect
     * failed), the failure of a push means that the download cannot proceed.
     * 
     * CONNECTION ESTABLISHMENT NOTE: All connection establish, push or direct,
     * is done via callbacks. There is no thread blocking on connection
     * establishment. When a connection either succeeds a ConnectObserver's
     * handleConnect(Socket) is called, which will ultimately attempt to start
     * the download via startDownload. If the connection attempt failed, the
     * ConnectObserver's shutdown method is called and no thread is ever
     * created.
     */
    private static final Log LOG = LogFactory.getLog(DownloadWorker.class);

    // /////////////////////// Policy Controls ///////////////////////////
    /** The smallest interval that can be split for parallel download. */
    private static final int MIN_SPLIT_SIZE = 16 * 1024; // 16 KB

    /**
     * The lowest (cumulative) bandwidth we will accept without stealing the
     * entire grey area from a downloader for a new one.
     */
    private final float minAcceptableSpeed;

    /**
     * The speed of download workers that haven't been started yet or do not
     * have enough measurements.
     */
    private static final int UNKNOWN_SPEED = -1;

    /**
     * The time to wait trying to establish each normal connection, in
     * milliseconds.
     */
    private static int NORMAL_CONNECT_TIME = 10000; // 10 seconds

    /**
     * The time to wait trying to establish each push connection, in
     * milliseconds. This needs to be larger than the normal time.
     */
    private static int PUSH_CONNECT_TIME = 20000; // 20 seconds

    /**
     * The time to wait trying to establish a push connection if only a UDP push
     * has been sent (as is in the case of altlocs).
     */
    private static final int UDP_PUSH_CONNECT_TIME = 6000; // 6 seconds

    /**
     * The number of seconds to wait for hosts that don't have any ranges we
     * would be interested in.
     */
    private static final int NO_RANGES_RETRY_AFTER = 60 * 5; // 5 minutes

    /**
     * The number of seconds to wait for hosts that failed once.
     */
    private static final int FAILED_RETRY_AFTER = 60 * 1; // 1 minute

    /**
     * The number of seconds to wait for a busy host (if it didn't give us a
     * retry after header) if we don't have any active downloaders.
     * <p>
     * Note that there are some acceptable problems with the way this values are
     * used. Namely, if we have sources X & Y and source X is tried first, but
     * is busy, its busy-time will be set to 1 minute. Then source Y is tried
     * and is accepted, source X will still retry after 1 minute. This 'problem'
     * is considered an acceptable issue, given the complexity of implementing a
     * method that will work under the circumstances.
     */
    public static final int RETRY_AFTER_NONE_ACTIVE = 60 * 1; // 1 minute

    /**
     * The minimum number of seconds to wait for a busy host if we do have some
     * active downloaders.
     * <p>
     * Note that there are some acceptable problems with the way this values are
     * used. Namely, if we have sources X & Y and source X is tried first and is
     * accepted. Then source Y is tried and is busy, so its busy-time is set to
     * 10 minutes. Then X disconnects, leaving Y with 9 or so minutes left
     * before being retried, despite no other sources available. This 'problem'
     * is considered an acceptable issue, given the complexity of implementing a
     * method that will work under the circumstances.
     */
    private static final int RETRY_AFTER_SOME_ACTIVE = 60 * 10; // 10 minutes

    private final DownloadWorkerSupport _manager;

    private final RemoteFileDesc _rfd;

    private final VerifyingFile _commonOutFile;
    private final DownloadStatsTracker statsTracker;

    /**
     * Whether I was interrupted before starting.
     */
    private final AtomicBoolean _interrupted = new AtomicBoolean(false);

    /**
     * The downloader that will do the actual downloading. 
     */
     //TODO: un-volatilize after fixing the assertion failures
     
    private volatile HTTPDownloader _downloader;

    /**
     * Whether I should release the ranges that I have leased for download. 
     */ 
     //TODO: un-volatilize after fixing the assertion failures
     
    private volatile boolean _shouldRelease;

    /**
     * The name this worker has in toString & threads.
     */
    private final String _workerName;

    /** The observer used for direct connection establishment. */
    private DirectConnector _connectObserver;

    /** The current state of the non-blocking download. */
    private final DownloadHttpRequestState _currentState;

    /**
     * Whether or not the worker is involved in a stealing operation (as either
     * a thief or victim).
     */
    private volatile boolean _stealing;
    
    private final HTTPDownloaderFactory httpDownloaderFactory;
    private final ScheduledExecutorService backgroundExecutor;
    private final ScheduledExecutorService nioExecutor;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final SocketsManager socketsManager;
    private final TLSManager TLSManager;

    private final RemoteFileDescContext rfdContext;

    protected DownloadWorker(DownloadWorkerSupport manager, RemoteFileDescContext rfdContext,
                             VerifyingFile vf, HTTPDownloaderFactory httpDownloaderFactory,
                             ScheduledExecutorService backgroundExecutor,
                             ScheduledExecutorService nioExecutor,
                             Provider<PushDownloadManager> pushDownloadManager,
                             SocketsManager socketsManager,
                             DownloadStatsTracker statsTracker, TLSManager TLSManager, BandwidthCollector bandwidthCollector) {
        this.httpDownloaderFactory = httpDownloaderFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.nioExecutor = nioExecutor;
        this.pushDownloadManager = pushDownloadManager;
        this.socketsManager = socketsManager;
        _manager = manager;
        _rfd = rfdContext.getRemoteFileDesc();
        this.rfdContext = rfdContext;
        _commonOutFile = vf;
        this.statsTracker = statsTracker;
        this.TLSManager = TLSManager;
        
        minAcceptableSpeed =  bandwidthCollector.getMaxMeasuredTotalDownloadBandwidth() < 8 ? 0.1f : 0.5f;
        
        _currentState = new DownloadHttpRequestState();

        // if we'll be debugging, we want to distinguish the different workers
        if (LOG.isDebugEnabled()) {
            _workerName = "DownloadWorker for "
                    + _manager.getSaveFile().getName() + " #"
                    + System.identityHashCode(this);
        } else {
            _workerName = "DownloaderWorker";
        }
    }

    /**
     * Starts this DownloadWorker's connection establishment.
     */
    public void start() {
        if (LOG.isDebugEnabled())
            LOG.debug("Starting worker: " + _workerName);
        establishConnection();
    }

    /**
     * Initializes the HTTPDownloader with whatever AltLocs we have discovered
     * so far. These will be cleared out after the first write. From then on,
     * only newly successful RFDS will be sent as Alts.
     */
    private void initializeAlternateLocations() {
        int count = 0;
        for (AlternateLocation current : _manager.getValidAlts()) {
            if (count++ >= 10)
                break;
            _downloader.addSuccessfulAltLoc(current);
        }

        count = 0;
        for (AlternateLocation current : _manager.getInvalidAlts()) {
            if (count++ >= 10)
                break;
            _downloader.addFailedAltLoc(current);
        }
    }

    /**
     * Begins the state machine for processing this download.
     */
    private void httpLoop() {
        LOG.debug("Starting HTTP Loop");
        incrementState(null);
    }

    /**
     * Notification that a state has finished. This kicks off the next stage if
     * necessary.
     */
    public void incrementState(ConnectionStatus status) {
        if (LOG.isTraceEnabled())
            LOG.trace("WORKER: " + this + ", State Changed, Current: "
                    + _currentState + ", status: " + status);

        if (_interrupted.get()) {
            finishHttpLoop();
            return;
        }

        switch (_currentState.getCurrentState()) {
        case DOWNLOADING:
            releaseRanges();
        case QUEUED:
        case BEGIN:
            _currentState.setHttp11(_rfd.isHTTP11());
            _currentState.setState(DownloadHttpRequestState.State.REQUESTING_THEX);
            if (requestTHEXIfNeeded())
                break; // wait for callback

        case REQUESTING_THEX:
            _currentState.setState(DownloadHttpRequestState.State.DOWNLOADING_THEX);
            if (downloadThexIfNeeded())
                break;

        case DOWNLOADING_THEX:
            _currentState.setState(DownloadHttpRequestState.State.CONSUMING_BODY);
            if (consumeBodyIfNeeded())
                break; // wait for callback

        case CONSUMING_BODY:
            _downloader.forgetRanges();
            if (status == null || !status.isQueued()) {
                _currentState.setState(DownloadHttpRequestState.State.REQUESTING_HTTP);
                if (!assignAndRequest()) { // no data
                    finishHttpLoop();
                }
                break; // wait for callback (or exit)
            }

        case REQUESTING_HTTP:
            httpRequestFinished(status);
            break;
        default:
            throw new IllegalStateException("bad state: " + _currentState);
        }
    }

    /**
     * Consumes the body of an HTTP Request if necessary. If consumption is
     * needed, this will return true and schedule a callback to continue.
     * Otherwise it will return false.
     * 
     * @return true if the body is scheduled for consumption, false if
     *         processing should continue.
     */
    private boolean consumeBodyIfNeeded() {
        if (_downloader.isBodyConsumed()) {
            LOG.debug("Not consuming body.");
            return false;
        }

        _downloader.consumeBody(new State() {
            @Override
            protected void handleState(boolean success) {
                if (!success)
                    handleRFDFailure();
            }
        });
        return true;
    }

    /**
     * Handles a failure of an RFD.
     */
    private void handleRFDFailure() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("rfd failure", new Exception());
        }
        rfdContext.incrementFailedCount();
        LOG.debug("handling rfd failure for "+_rfd+" with count now "+ rfdContext.getFailedCount());
        // if this RFD had a failure, try it again.
        if (rfdContext.getFailedCount() < 2) {
            LOG.debug("will try again in a minute");
            // set retry after, wait a little before retrying this RFD
            rfdContext.setRetryAfter(FAILED_RETRY_AFTER);
            _manager.addToRanker(rfdContext);
        } else
            // tried the location twice -- it really is bad
            _manager.informMesh(_rfd, false);
    }

    /**
     * Notification that assign&Request has finished. This will: - Finish the
     * download if no file was available. - Loop again if the requested range
     * was unavailable but other data is available. - Queue up if we were
     * instructed to be queued. - Download if we were told to download.
     * <p>
     * In all events, either the download completely finishes or a callback is
     * eventually notified of success or failure & the state continues moving.
     * 
     */
    private void httpRequestFinished(ConnectionStatus status) {
        if (LOG.isDebugEnabled())
            LOG.debug("HTTP req finished, status: " + status);

        _manager.addPossibleSources(_downloader.getLocationsReceived());

        if (status.isNoData() || status.isNoFile()) {
            finishHttpLoop();
        } else {
            if (!status.isConnected())
                releaseRanges();

            // After A&R, we got a non queued response.
            if (!status.isQueued())
                _manager.removeQueuedWorker(this);

            if (status.isPartialData()) {
                _currentState.setState(DownloadHttpRequestState.State.BEGIN);
                incrementState(null);
            } else {
                assert (status.isQueued() || status.isConnected());
                boolean queued = _manager.killQueuedIfNecessary(this, !status
                        .isQueued() ? -1 : status.getQueuePosition());

                if (status.isConnected()) {
                    _currentState.setState(DownloadHttpRequestState.State.DOWNLOADING);
                    beginDownload();
                } else if (!queued) { // If we were told not to queue.
                    finishHttpLoop();
                } else {
                    handleQueued(status);
                }
            }
        }
    }

    /**
     * Begins the process of downloading. When downloading finishes, this will
     * either finish the download (if an error occurred) or move to the next
     * state.
     * <p>
     * A successful download will reset the failed count on the RFD. A
     * DiskException while downloading will notify the manager of a problem.
     */
    private void beginDownload() {
        try {
            _downloader.doDownload(new State() {
                @Override
                protected void handleState(boolean success) {
                    if (success) {
                        rfdContext.resetFailedCount();
                    } else {
                        _manager.workerFailed(DownloadWorker.this);
                    }

                    // if we got too corrupted, cancel the download
                    if (_commonOutFile.isHopeless())
                        _manager.cancelCorruptDownload();

                    long stop = _downloader.getInitialReadingPoint()
                            + _downloader.getAmountRead();
                    if (LOG.isDebugEnabled())
                        LOG.debug("WORKER: terminating from " + _downloader
                                + " at " + stop + " error? " + !success);

                    synchronized (_manager) {
                        if (!success) {
                            _downloader.stop();
                            handleRFDFailure();
                        } else {
                            _manager.informMesh(_rfd, true);
                            if (!_currentState.isHttp11()) // no need to add
                                                            // http11
                                                            // _activeWorkers to
                                                            // files
                                _manager.addToRanker(rfdContext);
                        }
                    }
                }
            });
        } catch (SocketException se) {
            finishHttpLoop();
        }
    }

    /**
     * Determines if we should request a tiger tree from the remote computer.
     * This will return true if a request is going to be performed and false
     * otherwise. If this returns true, a callback will eventually increment the
     * state or finish the download completely.
     * 
     * @return true if the request is scheduled to be sent, false if processing
     *         should continue.
     */
    private boolean requestTHEXIfNeeded() {
        boolean shouldRequest = false;
        synchronized (_commonOutFile) {
            if (!_commonOutFile.isHashTreeRequested()) {
                HashTree ourTree = _commonOutFile.getHashTree();

                // request THEX from the _downloader if (the tree we have
                // isn't good enough or we don't have a tree) and another
                // worker isn't currently requesting one
                shouldRequest = _downloader.hasHashTree()
                        && _manager.getSha1Urn() != null
                        && (ourTree == null || !ourTree.isDepthGoodEnough());

                if (shouldRequest)
                    _commonOutFile.setHashTreeRequested(true);
            }
        }

        if (shouldRequest) {
            _downloader.requestHashTree(_manager.getSha1Urn(), new State() {
                @Override
                protected void handleState(boolean success) {
                }
            });
        }

        return shouldRequest;
    }

    /**
     * Begins a THEX download if it was just requested.
     * <p>
     * If the request failed, this will immediately increment the state so that
     * the body of the response can be consumed. Otherwise it will schedule a
     * download to take place and increment the state when finished.
     * 
     * @return true if the download was scheduled, false if processing should
     *         continue.
     */
    private boolean downloadThexIfNeeded() {
        if (!_downloader.isRequestingThex())
            return false;

        ConnectionStatus status = _downloader.parseThexResponseHeaders();
        if (!status.isConnected()) {
            // retry this RFD without THEX, since that's why it failed.
            rfdContext.setTHEXFailed();
            incrementState(status);
        } else {
            _manager.removeQueuedWorker(this);
            _downloader.downloadThexBody(_manager.getSha1Urn(), new State() {
                @Override
                protected void handleState(boolean success) {
                    HashTree newTree = _downloader.getHashTree();
                    _manager.hashTreeRead(newTree);
                }
            });
        }

        return true;
    }

    /**
     * Release the ranges assigned to our downloader.
     */
    private void releaseRanges() {

        if (!_shouldRelease)
            return;
        _shouldRelease = false;

        // do not release if the file is complete
        if (_commonOutFile.isComplete())
            return;

        HTTPDownloader downloader = _downloader;
        long high, low;
        synchronized (downloader) {

            // If this downloader was a thief and had to skip any ranges, do not
            // release them.
            low = downloader.getInitialReadingPoint()
                    + downloader.getAmountRead();
            low = Math.max(low, downloader.getInitialWritingPoint());
            high = downloader.getInitialReadingPoint()
                    + downloader.getAmountToRead() - 1;
        }

        if ((high - low) >= 0) {// dloader failed to download a part assigned to
                                // it?

            if (LOG.isDebugEnabled())
                LOG.debug("releasing ranges " + Range.createRange(low, high));

            try {
                _commonOutFile.releaseBlock(Range.createRange(low, high));
            } catch (AssertFailure bad) {
                downloader.createAssertionReport(bad);
            }

            downloader.forgetRanges();
        } else
            LOG.debug("nothing to release!");
    }

    /**
     * Schedules a callback for a queued worker.
     * 
     * @return true if we need to tell the manager to churn another connection
     *         and let this one die, false if we are going to try this
     *         connection again.
     */
    private void handleQueued(ConnectionStatus status) {
        // make sure that we're not in _downloaders if we're
        // sleeping/queued. this would ONLY be possible
        // if some uploader was misbehaved and queued
        // us after we successfully managed to download some
        // information. despite the rarity of the situation,
        // we should be prepared.
        _manager.removeActiveWorker(this);

        synchronized (_currentState) {
            if (_interrupted.get()) {
                LOG.debug("Exiting from queueing");
                return;
            }

            LOG.debug("Queueing");
            _currentState.setState(DownloadHttpRequestState.State.QUEUED);
        }

        backgroundExecutor.schedule(new Runnable() {
            public void run() {
                LOG.debug("Queue time up");

                synchronized (_currentState) {
                    if (_interrupted.get()) {
                        LOG.warn("WORKER: interrupted while waiting in queue "
                                + _downloader);
                        return;
                    }
                }

                nioExecutor.execute(
                        new Runnable() {
                            public void run() {
                                incrementState(null);
                            }
                        });
            }
        }, status.getQueuePollTime(), TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to establish a connection to the host in RFD.
     * <p>
     * This will return immediately, scheduling callbacks for the connection
     * events. The appropriate ConnectObserver (Push or Direct) will be notified
     * via handleConnect if successful or shutdown if not. From there, the rest
     * of the download may start.
     */
    private void establishConnection() {
        if (LOG.isTraceEnabled())
            LOG.trace("establishConnection(" + _rfd + ")");

        // this rfd may still be useful remember it
        if (_manager.isCancelled() || _manager.isPaused() || _interrupted.get()) {
            _manager.addToRanker(rfdContext);
            finishWorker();
            return;
        }

        synchronized (_manager) {
            DownloadState state = _manager.getState();
            // If we're just increasing parallelism, stay in DOWNLOADING
            // state. Otherwise the following call is needed to restart
            // the timer.
            if (_manager.getNumDownloaders() == 0
                    && state != DownloadState.COMPLETE
                    && state != DownloadState.ABORTED
                    && state != DownloadState.GAVE_UP
                    && state != DownloadState.UNABLE_TO_CONNECT
                    && state != DownloadState.DISK_PROBLEM
                    && state != DownloadState.CORRUPT_FILE
                    && state != DownloadState.HASHING
                    && state != DownloadState.SAVING
                    && state != DownloadState.SCANNING
                    && state != DownloadState.THREAT_FOUND
                    && state != DownloadState.SCAN_FAILED) {
                if (_interrupted.get())
                    return; // we were signalled to stop.
                _manager.setState(DownloadState.CONNECTING);
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("WORKER: attempting connect to " + _rfd.getAddress());

        // TODO move to DownloadStatsTracker?
        _manager.incrementTriedHostsCount();

        Address address = _rfd.getAddress();
        if (_rfd.isReplyToMulticast()) {
            // Start with a push connect, fallback to a direct connect, and do
            // not forget the RFD upon push failure.
            connectWithPush(new PushConnector(false, true));
        } else if (address instanceof PushEndpoint) {
            // Start with a push connect, do not fallback to a direct connect,
            // and do
            // forgot the RFD upon push failure.
            connectWithPush(new PushConnector(true, false));   
        } else if (address instanceof Connectable) {
            // Start with a direct connect, fallback to a push connect.
            connectDirectly((Connectable)address, new DirectConnector(true));
        } else { 
            socketsManager.connect(address, new SocketsConnectObserver());
        }
    }

    /**
     * Performs actions necessary after the connection process is finished. This
     * will tell the manager this is a bad RFD if no downloader could be
     * created, and stop the downloader if we were interrupted. Returns true if
     * the download should proceed, false otherwise.
     */
    private boolean finishConnect() {
        // if we didn't connect at all, tell the rest about this rfd
        if (_downloader == null) {
            _manager.informMesh(_rfd, false);
            return false;
        } else if (_interrupted.get()) {
            // if the worker got killed, make sure the downloader is stopped.
            _downloader.stop();
            _downloader = null;
            return false;
        }
        return true;
    }

    /**
     * Attempts to asynchronously connect through TCP to the remote end. This
     * will return immediately and the given observer will be notified of
     * success or failure.
     */
    private void connectDirectly(Connectable connectable, DirectConnector observer) {
        if (!_interrupted.get()) {
            ConnectType type = connectable.isTLSCapable()
                    && TLSManager.isOutgoingTLSEnabled() ? ConnectType.TLS
                    : ConnectType.PLAIN;
            if (LOG.isTraceEnabled())
                LOG.trace("WORKER: attempt asynchronous direct connection w/ "
                        + type + " to: " + _rfd);
            _connectObserver = observer;
            try {
                Socket socket = socketsManager.connect(connectable.getInetSocketAddress(),
                        NORMAL_CONNECT_TIME, observer, type);
                if (!observer.isShutdown())
                    observer.setSocket(socket);
            } catch (IOException iox) {
                observer.shutdown();
            }
        } else {
            finishWorker();
        }
    }

    
    /**
     * Attempts to connect by using a push to the remote end. This method will
     * return immediately and the given observer will be notified of success or
     * failure.
     */
    private void connectWithPush(PushConnector observer) {
        if (!_interrupted.get()) {
            if (LOG.isTraceEnabled())
                LOG.trace("WORKER: attempt push connection to: " + _rfd+" proxies ");
            _connectObserver = null;

            // When the push is complete and we have a socket ready to use
            // the acceptor thread is going to notify us using this object
            
            final PushDetails details = new PushDetails(_rfd.getClientGUID(),
                    ((IpPort)_rfd.getAddress()).getAddress());
            observer.setPushDetails(details);
            _manager.registerPushObserver(observer, details);
            pushDownloadManager.get().sendPush(_rfd,
                    observer);
            backgroundExecutor.schedule(new Runnable() {
                public void run() {
                    _manager.unregisterPushObserver(details, true);
                }
            }, _rfd.isFromAlternateLocation() ? UDP_PUSH_CONNECT_TIME
                    : PUSH_CONNECT_TIME, TimeUnit.MILLISECONDS);
        } else {
            finishWorker();
        }
    }

    String getInfo() {
        HTTPDownloader downloader = _downloader;
        if (downloader != null) {
            synchronized (downloader) {
                return this + "hashcode "
                        + System.identityHashCode(downloader)
                        + " will release? " + _shouldRelease + " interrupted? "
                        + _interrupted.get() + " active? " + downloader.isActive()
                        + " victim? " + downloader.isVictim()
                        + " initial reading "
                        + downloader.getInitialReadingPoint()
                        + " initial writing "
                        + downloader.getInitialWritingPoint()
                        + " amount to read " + downloader.getAmountToRead()
                        + " amount read " + downloader.getAmountRead()
                        + " is in stealing " + isStealing() + "\n";
            }
        } else
            return "worker not started";
    }

    /**
     * Assigns a white area or a grey area to a downloader. Sets the state, and
     * checks if this downloader has been interrupted.
     * 
     */
    private boolean assignAndRequest() {
        if (LOG.isTraceEnabled())
            LOG.trace("assignAndRequest for: " + _rfd);

        Range interval = null;
        try {
            synchronized (_commonOutFile) {
                if (_commonOutFile.hasFreeBlocksToAssign() > 0)
                    interval = pickAvailableInterval();
            }
        } catch (NoSuchRangeException nsre) {
            handleNoRanges();
            return false;
        }

        // it is still possible that a worker has died and released their ranges
        // just before we try to steal
        if (interval == null) {
            if (!assignGrey())
                return false;
        } else {
            assignWhite(interval);
        }

        return true;
    }

    /**
     * Completes the assignAndRequest by incrementing the state using the
     * ConnectionStatus that is generated by processing of the response headers.
     * 
     * @param x any IOException encountered while processing
     * @param range the range initially requested
     * @param victim the possibly null victim to steal from.
     */
    private void completeAssignAndRequest(IOException x, Range range,
            DownloadWorker victim) {
        ConnectionStatus status = completeAssignAndRequestImpl(x, range, victim);
        if (victim != null) {
            victim.setStealing(false);
            setStealing(false);
        }
        incrementState(status);
    }

    /**
     * Completes the assign & request process by parsing the response headers
     * and completing either assignWhite or assignGrey.
     * <p>
     * If victim is null, it is assumed that we are completing assignGrey.
     * Otherwise, we are completing assignWhite.
     * 
     * @param x any IOException encountered while processing
     * @param range the range initially requested
     * @param victim the possibly null victim to steal from.
     * @return
     */
    @SuppressWarnings({"ThrowFromFinallyBlock"})
    private ConnectionStatus completeAssignAndRequestImpl(IOException x,
            Range range, DownloadWorker victim) {
        try {
            try {
                _downloader.parseHeaders();
            } finally {
                // The IOX passed in here takes priority over
                // any exception from parsing the headers.
                if (x != null)
                    throw x;
            }

            if (victim == null)
                completeAssignWhite(range);
            else
                completeAssignGrey(victim, range);

        } catch (NoSuchElementException nsex) {
            LOG.debug(_downloader, nsex);

            return handleNoMoreDownloaders();

        } catch (NoSuchRangeException nsrx) {
            LOG.debug(_downloader, nsrx);

            return handleNoRanges();

        } catch (TryAgainLaterException talx) {
            LOG.debug(_downloader, talx);

            return handleTryAgainLater();

        } catch (RangeNotAvailableException rnae) {
            LOG.debug(_downloader, rnae);

            return handleRangeNotAvailable();

        } catch (FileNotFoundException fnfx) {
            LOG.debug(_downloader, fnfx);

            return handleFileNotFound();

        } catch (NotSharingException nsx) {
            LOG.debug(_downloader, nsx);

            return handleNotSharing();

        } catch (QueuedException qx) {
            LOG.debug(_downloader, qx);

            return handleQueued(qx.getQueuePosition(), qx.getMinPollTime());

        } catch (ProblemReadingHeaderException prhe) {
            LOG.debug(_downloader, prhe);

            return handleProblemReadingHeader();

        } catch (UnknownCodeException uce) {
            LOG.debug(_downloader, uce);

            return handleUnknownCode();

        } catch (ContentUrnMismatchException cume) {
            LOG.debug(_downloader, cume);

            return ConnectionStatus.getNoFile();

        } catch (IOException iox) {
            LOG.debug(_downloader, iox);

            return handleIO();

        }

        // did not throw exception? OK. we are downloading
        rfdContext.resetFailedCount();

        synchronized (_manager) {
            if (_manager.isCancelled() || _manager.isPaused() || _interrupted.get()) {
                LOG.trace("Stopped in assignAndRequest");
                _manager.addToRanker(rfdContext);
                return ConnectionStatus.getNoData();
            }

            _manager.workerStarted(this);
        }

        return ConnectionStatus.getConnected();
    }

    /**
     * Schedules a request for the given interval. Upon completion of the
     * request, completeAssignAndRequest will be called with the appropriate
     * parameters.
     */
    private void assignWhite(Range interval) {
        // Intervals from the IntervalSet set are INCLUSIVE on the high end, but
        // intervals passed to HTTPDownloader are EXCLUSIVE. Hence the +1 in the
        // code below. Note connectHTTP can throw several exceptions.
        final long low = interval.getLow();
        final long high = interval.getHigh(); // INCLUSIVE
        _shouldRelease = true;
        _downloader.connectHTTP(low, high + 1, true, _commonOutFile
                .getBlockSize(), new IOStateObserver() {
            public void handleStatesFinished() {
                completeAssignAndRequest(null, Range.createRange(low, high),
                        null);
            }

            public void handleIOException(IOException iox) {
                completeAssignAndRequest(iox, null, null);
            }

            public void shutdown() {
                completeAssignAndRequest(new IOException("shutdown"), null,
                        null);
            }

        });
    }

    /**
     * Completes assigning a white range to a downloader. If the downloader
     * shortened any of the requested ranges, this will release the remaining
     * pieces back to the VerifyingFile.
     */
    private void completeAssignWhite(Range expectedRange) {
        // The _downloader may have told us that we're going to read less data
        // than
        // we expect to read. We must release the not downloading leased
        // intervals
        // We only want to release a range if the reported subrange
        // was different, and was HIGHER than the low point.
        // in case this worker became a victim during the header exchange, we do
        // not
        // clip any ranges.
        HTTPDownloader downloader = _downloader;
        synchronized (downloader) {
            long low = expectedRange.getLow();
            long high = expectedRange.getHigh();
            long newLow = downloader.getInitialReadingPoint();
            long newHigh = (downloader.getAmountToRead() - 1) + newLow; // INCLUSIVE
            if (newHigh - newLow >= 0) {
                if (newLow > low) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("WORKER:"
                                + " Host gave subrange, different low.  Was: "
                                + low + ", is now: " + newLow);

                    _commonOutFile.releaseBlock(Range.createRange(low,
                            newLow - 1));
                }

                if (newHigh < high) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("WORKER:"
                                + " Host gave subrange, different high.  Was: "
                                + high + ", is now: " + newHigh);

                    _commonOutFile.releaseBlock(Range.createRange(newHigh + 1,
                            high));
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("WORKER:" + " assigning white " + newLow + "-"
                            + newHigh + " to " + downloader);
                }
            } else
                LOG.debug("debouched at birth");
        }
    }

    /**
     * Picks an unclaimed interval from the verifying file.
     * 
     * @throws NoSuchRangeException if the remote host is partial and doesn't
     *         have the ranges we need
     */
    private Range pickAvailableInterval() throws NoSuchRangeException {
        Range interval;
        
        // If it's not a partial source, take the first chunk.
        // (If it's HTTP11, take the first chunk up to CHUNK_SIZE)
        if (!rfdContext.isPartialSource()) {          
            if (_currentState.isHttp11()) {
                interval = _commonOutFile.leaseWhite(findChunkSize());
            } else
                interval = _commonOutFile.leaseWhite();
        }

        // If it is a partial source, extract the first needed/available range
        // (If it's HTTP11, take the first chunk up to CHUNK_SIZE)
        else {
            try {
                IntervalSet availableRanges = rfdContext.getAvailableRanges();

                if (_currentState.isHttp11()) {
                    interval = _commonOutFile.leaseWhite(availableRanges,
                            findChunkSize());
                } else
                    interval = _commonOutFile.leaseWhite(availableRanges);

            } catch (NoSuchElementException nsee) {
                // if nothing satisfied this partial source, don't throw NSEE
                // because that means there's nothing left to download.
                // throw NSRE, which means that this particular source is done.
                throw new NoSuchRangeException();
            }
        }

        return interval;
    }

    private long findChunkSize() {
        long chunkSize = _commonOutFile.getChunkSize();
        long free = _commonOutFile.hasFreeBlocksToAssign();

        // if we have less than one free chunk, take half of that
        if (free <= chunkSize && _manager.getActiveWorkers().size() > 1)
            chunkSize = Math.max(MIN_SPLIT_SIZE, free / 2);

        return chunkSize;
    }

    /**
     * Locates an interval from the slowest downloader and schedules a request
     * with it. If the current download has partial ranges, there is no slowest
     * download, or the slowest downloader has no ranges available for stealing,
     * this will return false and processing will immediately continue.
     * Otherwise, this will return true and completeAssignAndRequest will be
     * called when the request completes.
     */
    private boolean assignGrey() {
        // if I'm currently being stolen from, do not try to steal.
        // can happen if my thief is exchanging headers.
        if (isStealing())
            return false;

        // If this _downloader is a partial source, don't attempt to steal...
        // too confusing, too many problems, etc...
        if (rfdContext.isPartialSource()) {
            handleNoRanges();
            return false;
        }

        final DownloadWorker slowest = findSlowestDownloader();

        if (slowest == null) {// Not using this downloader...but RFD maybe
                                // useful
            LOG.debug("didn't find anybody to steal from");
            handleNoMoreDownloaders();
            return false;
        }

        // see what ranges is the victim requesting
        final Range slowestRange = slowest.getDownloadInterval();

        if (slowestRange.getLow() == slowestRange.getHigh()) {
            handleNoMoreDownloaders();
            return false;
        }

        // Note: we are not interested in being queued at this point this
        // line could throw a bunch of exceptions (not queuedException)
        slowest.setStealing(true);
        setStealing(true);
        _downloader.connectHTTP(slowestRange.getLow(), slowestRange.getHigh(),
                false, _commonOutFile.getBlockSize(), new IOStateObserver() {
                    public void handleStatesFinished() {
                        completeAssignAndRequest(null, slowestRange, slowest);
                    }

                    public void handleIOException(IOException iox) {
                        completeAssignAndRequest(iox, null, slowest);
                    }

                    public void shutdown() {
                        completeAssignAndRequest(new IOException("shutdown"),
                                null, slowest);
                    }

                });

        return true;
    }

    /**
     * Completes assigning a grey portion to a downloader. This accounts for
     * changes in the victim's downloaded range while we were requesting.
     */
    private void completeAssignGrey(DownloadWorker victim, Range slowestRange)
            throws IOException {
        Range newSlowestRange;
        long newStart;
        synchronized (victim.getDownloader()) {
            // if the victim died or was stopped while the thief was connecting,
            // we can't steal
            if (!victim.getDownloader().isActive()) {
                LOG.debug("victim is no longer active");
                throw new NoSuchElementException();
            }

            // see how much did the victim download while we were exchanging
            // headers.
            // it is possible that in that time some other worker died and freed
            // his ranges, and
            // the victim has already been assigned some new ranges. If that
            // happened we don't steal.
            newSlowestRange = victim.getDownloadInterval();
            if (newSlowestRange.getHigh() != slowestRange.getHigh()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("victim is now downloading something else "
                            + newSlowestRange + " vs. " + slowestRange);
                throw new NoSuchElementException();
            }

            if (newSlowestRange.getLow() > slowestRange.getLow()
                    && LOG.isDebugEnabled()) {
                LOG.debug("victim managed to download "
                        + (newSlowestRange.getLow() - slowestRange.getLow())
                        + " bytes while stealer was connecting");
            }

            long myLow = _downloader.getInitialReadingPoint();
            long myHigh = _downloader.getAmountToRead() + myLow; // EXCLUSIVE

            // If the stealer isn't going to give us everything we need,
            // there's no point in stealing, so throw an exception and
            // don't steal.
            if (myHigh < slowestRange.getHigh()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("WORKER: not stealing because stealer "
                            + "gave a subrange.  Expected low: "
                            + slowestRange.getLow() + ", high: "
                            + slowestRange.getHigh() + ".  Was low: " + myLow
                            + ", high: " + myHigh);
                }
                throw new IOException();
            }

            newStart = Math.max(newSlowestRange.getLow(), myLow);
            if (LOG.isDebugEnabled()) {
                LOG.debug("WORKER:" + " picking stolen grey " + newStart + "-"
                        + slowestRange.getHigh() + " from [" + victim
                        + "] to [" + this + "]");
            }

            // tell the victim to stop downloading at the point the thief
            // can start downloading
            victim.getDownloader().stopAt(newStart);
        }

        // once we've told the victim where to stop, make our ranges
        // release-able
        _downloader.startAt(newStart);
        _shouldRelease = true;
    }

    Range getDownloadInterval() {
        HTTPDownloader downloader = _downloader;
        synchronized (downloader) {

            long start = Math.max(downloader.getInitialReadingPoint()
                    + downloader.getAmountRead(), downloader
                    .getInitialWritingPoint());

            long stop = downloader.getInitialReadingPoint()
                    + downloader.getAmountToRead();

            return Range.createRange(start, stop);
        }
    }

    /** Sets this worker as being part of or not part of a stealing operation. */
    private void setStealing(boolean stealing) {
        this._stealing = stealing;
    }

    /**
     * Returns true if this worker is currently involved in a stealing
     * operation.
     */
    boolean isStealing() {
        return _stealing;
    }

    /**
     * @return the httpdownloader that is going slowest.
     */
    private DownloadWorker findSlowestDownloader() {
        DownloadWorker slowest = null;
        final float ourSpeed = getOurSpeed();
        float slowestSpeed = ourSpeed;

        Set<DownloadWorker> queuedWorkers = _manager.getQueuedWorkers()
                .keySet();
        for (DownloadWorker worker : _manager.getAllWorkers()) {
            if (worker.isStealing())
                continue;

            if (queuedWorkers.contains(worker))
                continue;

            HTTPDownloader h = worker.getDownloader();

            if (h == null || h == _downloader || h.isVictim())
                continue;

            // if we don't have speed yet, steal from the first slow guy
            if (ourSpeed == UNKNOWN_SPEED) {
                if (worker.isSlow())
                    return worker;
            } else {
                // see if he is the slowest one
                float hisSpeed;
                try {
                    h.getMeasuredBandwidth();
                    hisSpeed = h.getAverageBandwidth();
                } catch (InsufficientDataException ide) {
                    // we assume these guys would go almost as fast as we do, so
                    // we do not steal
                    // from them unless they are the last ones remaining
                    hisSpeed = Math.max(0f, ourSpeed - 0.1f);
                }

                if (hisSpeed < slowestSpeed) {
                    slowestSpeed = hisSpeed;
                    slowest = worker;
                }
            }

        }
        return slowest;
    }

    private float getOurSpeed() {
        if (_downloader == null)
            return UNKNOWN_SPEED;
        try {
            _downloader.getMeasuredBandwidth();
            return _downloader.getAverageBandwidth();
        } catch (InsufficientDataException bad) {
            return UNKNOWN_SPEED;
        }
    }

    /**
     * Returns true if the victim is going below minimum speed.
     * 
     */
    boolean isSlow() {
        float ourSpeed = getOurSpeed();
        return ourSpeed < minAcceptableSpeed && ourSpeed != UNKNOWN_SPEED;
    }

    // //// various handlers for failure states of the assign process /////

    /**
     * No more ranges to download or no more people to steal from - finish
     * download.
     */
    private ConnectionStatus handleNoMoreDownloaders() {
        _manager.addToRanker(rfdContext);
        return ConnectionStatus.getNoData();
    }

    /**
     * The file does not have such ranges.
     */
    private ConnectionStatus handleNoRanges() {
        // forget the ranges we are pretending uploader is busy.
        rfdContext.setAvailableRanges(null);

        // if this RFD did not already give us a retry-after header
        // then set one for it.
        if (!rfdContext.isBusy())
            rfdContext.setRetryAfter(NO_RANGES_RETRY_AFTER);

        rfdContext.resetFailedCount();
        _manager.addToRanker(rfdContext);

        return ConnectionStatus.getNoFile();
    }

    private ConnectionStatus handleTryAgainLater() {
        // if this RFD did not already give us a retry-after header
        // then set one for it.
        if (!rfdContext.isBusy()) {
            rfdContext.setRetryAfter(RETRY_AFTER_NONE_ACTIVE);
        }

        // if we already have downloads going, then raise the
        // retry-after if it was less than the appropriate amount
        if (!_manager.getActiveWorkers().isEmpty()
                && rfdContext.getWaitTime(System.currentTimeMillis()) < RETRY_AFTER_SOME_ACTIVE)
            rfdContext.setRetryAfter(RETRY_AFTER_SOME_ACTIVE);

        _manager.addToRanker(rfdContext);// try this rfd later

        rfdContext.resetFailedCount();
        return ConnectionStatus.getNoFile();
    }

    /**
     * The ranges exist in the file, but the remote host does not have them.
     */
    private ConnectionStatus handleRangeNotAvailable() {
        rfdContext.resetFailedCount();
        _manager.informMesh(_rfd, true);
        // no need to add to files or busy we keep iterating
        return ConnectionStatus.getPartialData();
    }

    private ConnectionStatus handleFileNotFound() {
        _manager.informMesh(_rfd, false);
        return ConnectionStatus.getNoFile();
    }

    private ConnectionStatus handleNotSharing() {
        return handleFileNotFound();
    }

    private ConnectionStatus handleQueued(int position, int pollTime) {
        synchronized (_manager) {
            if (_manager.getActiveWorkers().isEmpty()) {
                if (_manager.isCancelled() || _manager.isPaused()
                        || _interrupted.get())
                    return ConnectionStatus.getNoData(); // we were signalled
                                                            // to stop.
                _manager.setState(DownloadState.REMOTE_QUEUED);
            }
            rfdContext.resetFailedCount();
            return ConnectionStatus.getQueued(position, pollTime);
        }
    }

    private ConnectionStatus handleProblemReadingHeader() {
        return handleFileNotFound();
    }

    private ConnectionStatus handleUnknownCode() {
        return handleFileNotFound();
    }

    private ConnectionStatus handleIO() {
        handleRFDFailure();

        return ConnectionStatus.getNoFile();
    }

    // ////// end handlers of various failure states ///////

    /**
     * Interrupts this downloader.
     */
    void interrupt() {
        if (_interrupted.getAndSet(true))
            return;

        boolean finishLoop;
        synchronized (_currentState) {
            finishLoop = _currentState.getCurrentState() == DownloadHttpRequestState.State.QUEUED;
        }
        if (finishLoop)
            finishHttpLoop();

        if (LOG.isDebugEnabled())
            LOG.debug("Stopping while state is: " + _currentState + ", this: "
                    + toString());

        // If a downloader is set up, we don't need to deal
        // with the connector, since connecting has finished.
        if (_downloader != null) {
            _downloader.stop();
        } else {
            // Ensure that the ConnectObserver is cleaned up.
            DirectConnector observer = _connectObserver;
            if (observer != null) {
                Socket socket = observer.getSocket();
                // Make sure it immediately stops trying to connect.
                if (socket != null)
                    IOUtils.close(socket);
            }
        }
    }

    public RemoteFileDesc getRFD() {
        return _rfd;
    }

    HTTPDownloader getDownloader() {
        return _downloader;
    }

    @Override
    public String toString() {
        return _workerName + "[" + _currentState + "] -> " + _rfd;
    }

    /** Ensures this worker is finished and doesn't start again. */
    private void finishWorker() {
        _interrupted.set(true);
        _manager.workerFinished(this);
    }

    /**
     * Starts a new thread that will perform the download.
     */
    private void startDownload(HTTPDownloader dl) {
        _downloader = dl;

        // If we should continue, then start the download.
        if (finishConnect()) {
            LOG.trace("Starting download");
            initializeAlternateLocations();
            httpLoop();
        } else {
            finishWorker();
        }
    }

    /**
     * Completes the http loop of this downloader, effectively finishing its
     * reign of downloading.
     */
    private void finishHttpLoop() {
        releaseRanges();
        _manager.removeQueuedWorker(this);
        _downloader.stop();
        finishWorker();
    }

    /**
     * A simple IOStateObserver that will increment state upon completion and
     * finish on close/shutdown, but offer the ability for something to be done
     * prior to moving on in each case.
     */
    private abstract class State implements IOStateObserver {
        public final void handleIOException(IOException iox) {
            handleState(false);
            finishHttpLoop();
        }

        public final void handleStatesFinished() {
            handleState(true);
            incrementState(null);
        }

        public final void shutdown() {
            handleState(false);
            finishHttpLoop();
        }

        /** Handles per-state updating. */
        protected abstract void handleState(boolean success);
    }

    /**
     * A ConnectObserver for starting the download via a push connect.
     */
    private class PushConnector extends HTTPConnectObserver implements
            MultiShutdownable {
        private boolean forgetOnFailure;

        private boolean directConnectOnFailure;

        private PushDetails pushDetails;

        /** Additional Shutdownable to notify if we are shutdown. */
        private volatile Shutdownable toCancel;

        /** Determines if this is shutdown yet. */
        private AtomicBoolean shutdown = new AtomicBoolean(false);

        /**
         * Creates a new PushConnector. If forgetOnFailure is true, this will
         * call _manager.forgetRFD(_rfd) if the push fails. If
         * directConnectOnFailure is true, this will attempt a direct connection
         * if the push fails. Upon success, this will always start the download.
         */
        PushConnector(boolean forgetOnFailure, boolean directConnectOnFailure) {
            this.forgetOnFailure = forgetOnFailure;
            this.directConnectOnFailure = directConnectOnFailure;
        }

        /** Associates a new shutdownable that will be notified when this closes. */
        public void addShutdownable(Shutdownable newCancel) {
            toCancel = newCancel;
        }

        /**
         * Notification that the push succeeded. Starts the download if the
         * connection still exists.
         */
        @Override
        public void handleConnect(Socket socket) {
            // LOG.debug(_rfd + " -- Handling connect from PushConnector");
            HTTPDownloader dl = httpDownloaderFactory.create(socket, rfdContext,
                    _commonOutFile, false);
            try {
                dl.initializeTCP();
                statsTracker.successfulPushConnect();
            } catch (IOException iox) {
                failed();
                return;
            }

            startDownload(dl);
        }

        /** Determines if this was shutdown. */
        public boolean isCancelled() {
            return shutdown.get();
        }

        /** Notification that the push failed. */
        public void shutdown() {
            statsTracker.failedPushConnect();
            // if it was already shutdown, don't shutdown again.
            if (shutdown.getAndSet(true))
                return;

            Shutdownable canceller = toCancel;
            if (canceller != null)
                canceller.shutdown();
            failed();
        }

        /** Sets the details that will be used to unregister the push observer. */
        void setPushDetails(PushDetails details) {
            this.pushDetails = details;
        }

        /**
         * Possibly tells the manager to forget this RFD, cleans up various
         * things, and tells the manager to forget this worker.
         */
        private void failed() {
            _manager.unregisterPushObserver(pushDetails, false);

            if (!directConnectOnFailure) {
                if (forgetOnFailure) {
                    _manager.forgetRFD(_rfd);
                }
                finishConnect();
                finishWorker();
            } else {
                assert _rfd.isReplyToMulticast() : "only multicast replies have an address to direct connect to";
                connectDirectly((Connectable)_rfd.getAddress(), new DirectConnector(false));
            }
        }
    }

    /**
     * A ConnectObserver for starting the download via a direct connect.
     */
    private class DirectConnector extends HTTPConnectObserver {
        private boolean pushConnectOnFailure;

        private Socket connectingSocket;

        private boolean shutdown;

        /**
         * Creates a new DirectConnection. If pushConnectOnFailure is true, this
         * will attempt a push connection if the direct connect fails. Upon
         * success, this will always start a new download.
         */
        DirectConnector(boolean pushConnectOnFailure) {
            this.pushConnectOnFailure = pushConnectOnFailure;
        }

        /**
         * Upon successful connect, create the HTTPDownloader with the right
         * socket, and proceed to continue downloading.
         */
        @Override
        public void handleConnect(Socket socket) {
            this.connectingSocket = null;

            HTTPDownloader dl = httpDownloaderFactory.create(socket, rfdContext,
                    _commonOutFile, false);
            try {
                dl.initializeTCP(); // already connected, timeout doesn't
                                    // matter.
                statsTracker.successfulDirectConnect();
            } catch (IOException iox) {
                shutdown(); // if it immediately IOX's, try a push instead.
                return;
            }

            startDownload(dl);
        }

        /**
         * Upon unsuccessful connect, try using a push (if pushConnectOnFailure
         * is true).
         */
        public void shutdown() {
            statsTracker.failedDirectConnect();
            this.shutdown = true;
            this.connectingSocket = null;

            if (pushConnectOnFailure) {
                statsTracker.increment(DownloadStatsTracker.PushReason.DIRECT_FAILED);
                connectWithPush(new PushConnector(false, false));
            } else {
                finishConnect();
                finishWorker();
            }
        }

        void setSocket(Socket socket) {
            this.connectingSocket = socket;
        }

        Socket getSocket() {
            return this.connectingSocket;
        }

        public boolean isShutdown() {
            return shutdown;
        }
    }
    
    private class SocketsConnectObserver implements ConnectObserver {

        @Override
        public void handleConnect(Socket socket) throws IOException {
            LOG.debug("got a socket");
            HTTPDownloader dl = httpDownloaderFactory.create(socket, rfdContext,
                    _commonOutFile, false);
            try {
                dl.initializeTCP(); // already connected, timeout doesn't
                                    // matter.
                statsTracker.successfulDirectConnect();
            } catch (IOException iox) {
                LOG.debug("error initializing tcp", iox);
                shutdown(); // if it immediately IOX's, try a push instead.
                return;
            }
            startDownload(dl);
        }

        @Override
        public void handleIOException(IOException iox) {
            LOG.debug("could not connect", iox);
            finishConnect();
            finishWorker();
        }

        @Override
        public void shutdown() {
            LOG.debug("shut down");
            finishConnect();
            finishWorker();
        }
        
    }
}
