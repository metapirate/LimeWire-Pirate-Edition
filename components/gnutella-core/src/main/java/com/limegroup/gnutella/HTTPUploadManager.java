package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.collection.Buffer;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.core.settings.UploadSettings;
import org.limewire.http.HttpAcceptorListener;
import org.limewire.http.auth.ServerAuthState;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.FileLocker;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.http.HttpContextParams;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.uploader.FileRequestHandler;
import com.limegroup.gnutella.uploader.HTTPUploadSession;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HTTPUploader;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadType;
import com.limegroup.gnutella.uploader.authentication.GnutellaBrowseFileViewProvider;
import com.limegroup.gnutella.uploader.authentication.GnutellaUploadFileViewProvider;

/**
 * Manages {@link HTTPUploader} objects that are created by
 * {@link HttpRequestHandler}s through the {@link HTTPUploadSessionManager}
 * interface. Since HTTP 1.1 allows multiple requests for a single connection an
 * {@link HTTPUploadSession} is created for each connection. It keeps track of
 * queuing (which is per connection) and bandwidth and has a reference to the
 * {@link HTTPUploader} that represents the current request.
 * <p>
 * The state of <code>HTTPUploader</code> follows this pattern:
 * 
 * <pre>
 *                             |-&gt;---- THEX_REQUEST -------&gt;--|
 *                             |-&gt;---- UNAVAILABLE_RANGE --&gt;--|
 *                             |-&gt;---- PUSH_PROXY ---------&gt;--|
 *                            /--&gt;---- FILE NOT FOUND -----&gt;--|
 *                           /---&gt;---- MALFORMED REQUEST --&gt;--|
 *                          /----&gt;---- BROWSE HOST --------&gt;--|
 *                         /-----&gt;---- UPDATE FILE --------&gt;--|
 *                        /------&gt;---- QUEUED -------------&gt;--|
 *                       /-------&gt;---- LIMIT REACHED ------&gt;--|
 *                      /--------&gt;---- UPLOADING ----------&gt;--|
 * --&gt;--CONNECTING--&gt;--/                                      |
 *        |                                                  \|/
 *        |                                                   |
 *       /|\                                                  |---&gt;INTERRUPTED
 *        |--------&lt;---COMPLETE-&lt;------&lt;-------&lt;-------&lt;------/      (done)
 *                        |
 *                        |
 *                      (done)
 * </pre>
 * 
 * COMPLETE uploaders may be using HTTP/1.1, in which case the HTTPUploader
 * recycles back to CONNECTING upon receiving the next GET/HEAD request and
 * repeats.
 * <p>
 * INTERRUPTED HTTPUploaders are never reused. However, it is possible that the
 * socket may be reused. This case is only possible when a requester is queued
 * for one file and sends a subsequent request for another file. The first
 * <code>HTTPUploader</code> is set as interrupted and a second one is created
 * for the new file, using the same connection as the first one.
 * <p>
 * To initialize the upload manager {@link #start(HTTPAcceptor)} needs to be
 * invoked which registers handlers with an {@link HTTPAcceptor}.
 * 
 * @see com.limegroup.gnutella.uploader.HTTPUploader
 * @see com.limegroup.gnutella.HTTPAcceptor
 */
@EagerSingleton
public class HTTPUploadManager implements FileLocker, BandwidthTracker,
        UploadManager, HTTPUploadSessionManager, Service {

    /** The key used to store the {@link HTTPUploadSession} object. */
    private final static String SESSION_KEY = "org.limewire.session";

    private static final Log LOG = LogFactory.getLog(HTTPUploadManager.class);

    /**
     * This is a <code>List</code> of all of the current <code>Uploader</code>
     * instances (all of the uploads in progress that are not queued).
     */
    private List<HTTPUploader> activeUploadList = new LinkedList<HTTPUploader>();

    /** A manager for the available upload slots */
    private final UploadSlotManager slotManager;

    /** Set to true when an upload has been successfully completed. */
    private volatile boolean hadSuccesfulUpload = false;

    /** Number of force-shared active uploads. */
    private int forcedUploads;

    private final HttpRequestHandler freeLoaderRequestHandler;

    private final ResponseListener responseListener = new ResponseListener();

    /**
     * Number of active uploads that are not accounted in the slot manager but
     * whose bandwidth is counted. (i.e. Multicast)
     */
    private final Set<HTTPUploader> localUploads = new CopyOnWriteArraySet<HTTPUploader>();

    /**
     * LOCKING: obtain this' monitor before modifying any of the data structures
     */

    /**
     * The number of uploads considered when calculating capacity, if possible.
     * BearShare uses 10. Settings it too low causes you to be fooled be a
     * streak of slow downloaders. Setting it too high causes you to be fooled
     * by a number of quick downloads before your slots become filled.
     */
    private static final int MAX_SPEED_SAMPLE_SIZE = 5;

    /**
     * The min number of uploads considered to give out your speed. Same
     * criteria needed as for MAX_SPEED_SAMPLE_SIZE.
     */
    private static final int MIN_SPEED_SAMPLE_SIZE = 5;

    /** The minimum number of bytes transferred by an uploadeder to count. */
    private static final int MIN_SAMPLE_BYTES = 200000; // 200KB

    public static final int TRANSFER_SOCKET_TIMEOUT = 2 * 60 * 1000;

    /** The average speed in kiloBITs/second of the last few uploads. */
    private Buffer<Integer> speeds = new Buffer<Integer>(MAX_SPEED_SAMPLE_SIZE);

    /**
     * The highestSpeed of the last few downloads, or -1 if not enough downloads
     * have been down for an accurate sample. INVARIANT: highestSpeed>=0 ==>
     * highestSpeed==max({i | i in speeds}) INVARIANT: speeds.size()<MIN_SPEED_SAMPLE_SIZE
     * <==> highestSpeed==-1
     */
    private volatile int highestSpeed = -1;

    /**
     * The number of measureBandwidth's we've had
     */
    private int numMeasures = 0;

    /**
     * The current average bandwidth.
     * 
     * This is only counted while uploads are active.
     */
    private float averageBandwidth = 0f;

    /** The last value that getMeasuredBandwidth created. */
    private volatile float lastMeasuredBandwidth;

    /**
     * Remembers uploaders to disadvantage uploaders that hammer us for download
     * slots. Stores up to 250 entries Maps IP String to RequestCache
     */
    private final Map<String, RequestCache> REQUESTS = new FixedsizeForgetfulHashMap<String, RequestCache>(
            250);

    private final Provider<ActivityCallback> activityCallback;

    private volatile boolean started;
    
    private final HttpRequestHandlerFactory httpRequestHandlerFactory;

    private final Provider<HTTPAcceptor> httpAcceptor;
    
    private final Provider<GnutellaUploadFileViewProvider> gnutellaUploadFileListProvider;

    private final Provider<GnutellaBrowseFileViewProvider> gnutellaBrowseFileListProvider;
    
    private final Library library;
    
    @Inject
    public HTTPUploadManager(UploadSlotManager slotManager,
            HttpRequestHandlerFactory httpRequestHandlerFactory,
            Provider<HTTPAcceptor> httpAcceptor,
            Provider<ActivityCallback> activityCallback,
            Provider<GnutellaUploadFileViewProvider> gnutellaFileListProvider,
            Provider<GnutellaBrowseFileViewProvider> gnutellaBrowseFileListProvider,
            Library library) {
        this.gnutellaUploadFileListProvider = gnutellaFileListProvider;
        this.gnutellaBrowseFileListProvider = gnutellaBrowseFileListProvider;
        this.slotManager = Objects.nonNull(slotManager, "slotManager");
        this.httpRequestHandlerFactory = httpRequestHandlerFactory;
        this.freeLoaderRequestHandler = httpRequestHandlerFactory.createFreeLoaderRequestHandler();
        this.httpAcceptor = Objects.nonNull(httpAcceptor, "httpAcceptor");
        this.library = Objects.nonNull(library, "library");
        this.activityCallback = Objects.nonNull(activityCallback, "activityCallback");
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Upload Management");
    }
    public void initialize() {
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    /**
     * Registers the upload manager at <code>acceptor</code>.
     * 
     * @throws IllegalStateException if uploadmanager was already started
     * @see #stop(HTTPAcceptor)
     */
    public void start() {
        if (started) {
            throw new IllegalStateException();
        }
        
        FileUtils.addFileLocker(this);

        httpAcceptor.get().addAcceptorListener(responseListener);

        // browse
        httpAcceptor.get().registerHandler("/", httpRequestHandlerFactory.createBrowseRequestHandler(gnutellaBrowseFileListProvider.get(), false));

        // push-proxy requests
        NHttpRequestHandler pushProxyHandler = httpRequestHandlerFactory.createPushProxyRequestHandler();
        httpAcceptor.get().registerHandler("/gnutella/push-proxy", pushProxyHandler);
        httpAcceptor.get().registerHandler("/gnet/push-proxy", pushProxyHandler);

        // uploads
        FileRequestHandler fileRequestHandler = httpRequestHandlerFactory.createFileRequestHandler(gnutellaUploadFileListProvider.get(), false);
        httpAcceptor.get().registerHandler("/uri-res/*", fileRequestHandler);
        
        started = true;
    }

    /**
     * Unregisters the upload manager at <code>acceptor</code>.
     * 
     * @see #start(HTTPAcceptor)
     */
    public void stop() {
        if (!started) {
            throw new IllegalStateException();
        }

        httpAcceptor.get().unregisterHandler("/");
        httpAcceptor.get().unregisterHandler("/update.xml");
        httpAcceptor.get().unregisterHandler("/gnutella/push-proxy");
        httpAcceptor.get().unregisterHandler("/gnet/push-proxy");
        httpAcceptor.get().unregisterHandler("/get*");
        httpAcceptor.get().unregisterHandler("/uri-res/*");
        
        httpAcceptor.get().removeAcceptorListener(responseListener);
        
        FileUtils.removeFileLocker(this);
        
        started = false;
    }
    
    public void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException,
            IOException {
        assert started;
        uploader.setState(UploadStatus.FREELOADER);
        freeLoaderRequestHandler.handle(request, response, context);
    }

    /**
     * Determines whether or not this uploader should bypass queuing, (meaning
     * that it will always work immediately, and will not use up slots for other
     * uploaders).
     */
    private boolean shouldBypassQueue(HttpRequest request, HTTPUploader uploader) {
        assert uploader.getState() == UploadStatus.CONNECTING;
        return "HEAD".equals(request.getRequestLine().getMethod())
            || uploader.isForcedShare();
    }

    /**
     * Cleans up a finished uploader. This does the following:
     * <ol>
     * <li>Reports the speed at which this upload occured.
     * <li>Removes the uploader from the active upload list
     * <li>Increments the completed uploads in the FileDesc
     * <li>Removes the uploader from the GUI
     * </ol>
     */
    public void cleanupFinishedUploader(HTTPUploader uploader) {
        assert started;
        
        if (LOG.isTraceEnabled())
            LOG.trace("Cleaning uploader " + uploader);

        UploadStatus state = uploader.getState();
        UploadStatus lastState = uploader.getLastTransferState();
        // assertAsFinished(state);

        long finishTime = System.currentTimeMillis();
        synchronized (this) {
            // Report how quickly we uploaded the data.
            if (uploader.getStartTime() > 0) {
                reportUploadSpeed(finishTime - uploader.getStartTime(),
                        uploader.getTotalAmountUploaded());
            }
            removeFromList(uploader);
            localUploads.remove(uploader);
        }

        if (uploader.getUploadType() != null
                && !uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null
                    && state == UploadStatus.COMPLETE
                    && (lastState == UploadStatus.UPLOADING || lastState == UploadStatus.THEX_REQUEST)) {
                fd.incrementCompletedUploads();
                activityCallback.get().handleSharedFileUpdate(fd.getFile());
            }
        }

        activityCallback.get().uploadComplete(uploader);
    }

    public synchronized void addAcceptedUploader(HTTPUploader uploader, HttpContext context) {
        assert started;
        
        if (uploader.isForcedShare()) 
            forcedUploads++;
        else if (HttpContextParams.isLocal(context))
            localUploads.add(uploader);
        activeUploadList.add(uploader);
        uploader.setStartTime(System.currentTimeMillis());
    }

    public void sendResponse(HTTPUploader uploader, HttpResponse response) {
        assert started;
        
        uploader.setLastResponse(response);

        if (uploader.isVisible()) {
            return;
        }

        // We are going to notify the gui about the new upload, and let
        // it decide what to do with it - will act depending on it's
        // state
        activityCallback.get().addUpload(uploader);
        uploader.setVisible(true);

        if (!uploader.getUploadType().isInternal()) {
            FileDesc fd = uploader.getFileDesc();
            if (fd != null) {
                fd.incrementAttemptedUploads();
                activityCallback.get().handleSharedFileUpdate(fd.getFile());
            }
        }
    }

    public synchronized boolean isServiceable() {
        if (!started) {
            return false;
        }
        
        return slotManager.hasHTTPSlot(uploadsInProgress()
                + getNumQueuedUploads());
    }

    public synchronized boolean mayBeServiceable() {
        if (!started) {
            return false;
        }
        
        return isServiceable();
    }

    public synchronized int uploadsInProgress() {
        return activeUploadList.size() - forcedUploads;
    }

    public synchronized int getNumQueuedUploads() {
        return slotManager.getNumQueued();
    }

    public boolean hadSuccesfulUpload() {
        return hadSuccesfulUpload;
    }

    public synchronized boolean isConnectedTo(InetAddress addr) {
        if (slotManager.getNumUsersForHost(addr.getHostAddress()) > 0)
            return true;

        for (HTTPUploader uploader : activeUploadList) {
            InetAddress host = uploader.getConnectedHost();
            if (host != null && host.equals(addr))
                return true;
        }
        return false;
    }

    public boolean releaseLock(File file) {
        assert started;
        
        FileDesc fd = library.getFileDesc(file);
        if (fd != null)
            return killUploadsForFileDesc(fd);
        else
            return false;
    }

    public synchronized boolean killUploadsForFileDesc(FileDesc fd) {
        boolean ret = false;
        for (HTTPUploader uploader : activeUploadList) {
            FileDesc upFD = uploader.getFileDesc();
            if (upFD != null && upFD.equals(fd)) {
                ret = true;
                uploader.stop();
            }
        }
        return ret;
    }

    /**
     * Checks whether the given upload may proceed based on number of slots,
     * position in upload queue, etc. Updates the upload queue as necessary.
     * 
     * @return ACCEPTED if the download may proceed, QUEUED if this is in the
     *         upload queue, REJECTED if this is flat-out disallowed (and hence
     *         not queued) and BANNED if the downloader is hammering us, and
     *         BYPASS_QUEUE if this is a File-View request that isn't hammering
     *         us. If REJECTED, <code>uploader</code>'s state will be set to
     *         LIMIT_REACHED. If BANNED, the <code>Uploader</code>'s state
     *         will be set to BANNED_GREEDY.
     */
    private synchronized QueueStatus checkAndQueue(HTTPUploadSession session, HttpContext context) {
        RequestCache rqc = REQUESTS.get(session.getHost());
        if (rqc == null)
            rqc = new RequestCache();
        // make sure we don't forget this RequestCache too soon!
        REQUESTS.put(session.getHost(), rqc);
        rqc.countRequest();
        // only enforce hammering for unauthenticated clients that don't have credentials
        ServerAuthState serverAuthState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE); 
        if (serverAuthState == null || serverAuthState.getCredentials() == null) {
            if (rqc.isHammering()) {
                if (LOG.isWarnEnabled())
                    LOG.warn("BANNED: " + session.getHost() + " (hammering)");
                return QueueStatus.BANNED;
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("friend asking: " + serverAuthState.getCredentials().getUserPrincipal().getName());
            }
        }

        FileDesc fd = session.getUploader().getFileDesc();
        URN sha1 = fd.getSHA1Urn();

        if (rqc.isDupe(sha1) && UploadSettings.CHECK_DUPES.getValue()) {
            if (LOG.isDebugEnabled())
                LOG.debug("REJECTED: request " + sha1 + " from " + session.getHost() + " (duplicate request)");
            return QueueStatus.REJECTED;    
        }

        // check the host limit unless this is a poll
        if (slotManager.positionInQueue(session) == -1
                && hostLimitReached(session.getHost())) {
            if (LOG.isDebugEnabled())
                LOG.debug("REJECTED: request " + sha1 + " from " + session.getHost() + " (host limit reached)");
            return QueueStatus.REJECTED;
        }

        int queued = slotManager.pollForSlot(session, session.getUploader()
                .supportsQueueing(), session.getUploader().isPriorityShare());

//        if (LOG.isDebugEnabled())
//            LOG.debug("queued at " + queued);

        if (queued == -1) { // not accepted nor queued.
            if (LOG.isDebugEnabled())
                LOG.debug("QUEUED: request " + sha1 + " from " + session.getHost() + " (attempt to queue failed)");
            return QueueStatus.REJECTED;
        }

        if (queued > 0 && session.poll()) {
            if (LOG.isDebugEnabled())
                LOG.debug("BANNED: request " + sha1 + " from " + session.getHost());
            slotManager.cancelRequest(session);
            // TODO we used to just drop the connection
            return QueueStatus.BANNED;
        }
        if (queued > 0) {
            if (LOG.isDebugEnabled())
                LOG.debug("QUEUED: request " + sha1 + " from " + session.getHost());
            return QueueStatus.QUEUED;
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("ACCEPTED: request " + sha1 + " from " + session.getHost());
            rqc.startedTransfer(sha1);
            return QueueStatus.ACCEPTED;
        }
    }

    /**
     * Decrements the number of active uploads for the host specified in the
     * <code>host</code> argument, removing that host from the
     * <code>Map</code> if this was the only upload allocated to that host.
     * <p>
     * This method also removes <code>uploader</code> from the
     * <code>List</code> of active uploads.
     */
    private synchronized void removeFromList(HTTPUploader uploader) {
        // if the uploader is not in the active list, we should not
        // try remove the urn from the map of unique uploaded files for that
        // host.

        if (activeUploadList.remove(uploader)) {
            if (uploader.isForcedShare())
                forcedUploads--;

            // at this point it is safe to allow other uploads from the same
            // host
            RequestCache rcq = REQUESTS.get(uploader.getHost());

            // check for nulls so that unit tests pass
            if (rcq != null && uploader.getFileDesc() != null)
                rcq.transferDone(uploader.getFileDesc().getSHA1Urn());
        }

        // Enable auto shutdown
        if (activeUploadList.size() == 0)
            activityCallback.get().uploadsComplete();
    }

    /**
     * @return false if the number of uploads from the host is strictly LESS
     *         than the MAX, although we want to allow exactly MAX uploads from
     *         the same host. This is because this method is called BEFORE we
     *         add/allow the. upload.
     */
    private synchronized boolean hostLimitReached(String host) {
        return slotManager.getNumUsersForHost(host) >= UploadSettings.UPLOADS_PER_PERSON
                .getValue();
    }

    // //////////////// Bandwidth Allocation and Measurement///////////////

    public int measuredUploadSpeed() {
        // Note that no lock is needed.
        return highestSpeed;
    }

    /**
     * Notes that some uploader has uploaded the given number of BYTES in the
     * given number of milliseconds. If bytes is too small, the data may be
     * ignored.
     * 
     * @requires this' lock held
     * @modifies this.speed, this.speeds
     */
    private void reportUploadSpeed(long milliseconds, long bytes) {
        // This is critical for ignoring 404's messages, etc.
        if (bytes < MIN_SAMPLE_BYTES)
            return;

        // Calculate the bandwidth in kiloBITS/s. We just assume that 1 kilobyte
        // is 1000 (not 1024) bytes for simplicity.
        int bandwidth = 8 * (int) ((float) bytes / (float) milliseconds);
        synchronized (speeds) {
            speeds.add(bandwidth);

            // Update maximum speed if possible
            if (speeds.size() >= MIN_SPEED_SAMPLE_SIZE) {
                int max = 0;
                for (int i = 0; i < speeds.size(); i++)
                    max = Math.max(max, speeds.get(i));
                this.highestSpeed = max;
            }
        }
    }

    public void measureBandwidth() {
        slotManager.measureBandwidth();
        for (HTTPUploader active : localUploads) {
            active.measureBandwidth();
        }
    }

    public float getMeasuredBandwidth() {
        float bw = 0;
        try {
            bw += slotManager.getMeasuredBandwidth();
        } catch (InsufficientDataException notEnough) {
        }

        for (HTTPUploader forced : localUploads) {
            try {
                bw += forced.getMeasuredBandwidth();
            } catch (InsufficientDataException e) {
            }
        }

        synchronized (this) {
            averageBandwidth = ((averageBandwidth * numMeasures) + bw)
                    / ++numMeasures;
        }
        lastMeasuredBandwidth = bw;
        return bw;
    }

    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }

    public float getLastMeasuredBandwidth() {
        return lastMeasuredBandwidth;
    }

    public UploadSlotManager getSlotManager() {
        return slotManager;
    }

    public HTTPUploadSession getOrCreateSession(HttpContext context) {
        assert started;
        
        HTTPUploadSession session = (HTTPUploadSession) context
                .getAttribute(SESSION_KEY);
        if (session == null) {
            HttpInetConnection conn = (HttpInetConnection) context
                    .getAttribute(ExecutionContext.HTTP_CONNECTION);
            InetAddress host;
            if (conn != null) {
                host = conn.getRemoteAddress();
            } else {
                // XXX this is a bad work around to make request processing
                // testable without having an underlying connection
                try {
                    host = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
            session = new HTTPUploadSession(getSlotManager(), host,
                    HttpContextParams.getIOSession(context));
            context.setAttribute(SESSION_KEY, session);
        }
        return session;
    }

    /**
     * Returns the session stored in <code>context</code>.
     * 
     * @return null, if no session exists
     */
    public HTTPUploadSession getSession(HttpContext context) {
        assert started;
        
        HTTPUploadSession session = (HTTPUploadSession) context
                .getAttribute(SESSION_KEY);
        return session;
    }

    public HTTPUploader getOrCreateUploader(HttpRequest request,
            HttpContext context, UploadType type, String filename) {
        return getOrCreateUploader(request, context, type, filename, null);
    }
    
    public HTTPUploader getOrCreateUploader(HttpRequest request,
            HttpContext context, UploadType type, String filename, String friendID) {
        assert started;
        
        HTTPUploadSession session = getOrCreateSession(context);
        HTTPUploader uploader = session.getUploader();
        if (LOG.isDebugEnabled()) {
            LOG.debug("request line: " + request.getRequestLine());
            LOG.debug("session: " + session);
            LOG.debug("uploader: " + uploader);
        }
        if (uploader != null) {
            if (!uploader.getFileName().equals(filename)
                    || !uploader.getMethod().equals(
                            request.getRequestLine().getMethod())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("uploader filename: " + uploader.getFileName() + " vs filename:" + filename);
                    LOG.debug("uploader method: " + uploader.getMethod() + " vs method: " + request.getRequestLine().getMethod());
                    LOG.debug("request line: " + request.getRequestLine());
                }
                
                
                // Because queuing is per-socket (and not per file),
                // we do not want to reset the queue status if they're
                // requesting a new file.
                if (session.isQueued()) {
                    // However, we DO want to make sure that the old file
                    // is interpreted as interrupted. Otherwise,
                    // the GUI would show two lines with the the same slot
                    // until the newer line finished, at which point
                    // the first one would display as a -1 queue position.
                    uploader.setState(UploadStatus.INTERRUPTED);
                } 
                
                cleanupFinishedUploader(uploader);

                uploader = new HTTPUploader(filename, session);
                uploader.setFriendId(friendID);
            } else {
                // reuse existing uploader object
                uploader.reinitialize();
            }
        } else {
            // first request for this session
            uploader = new HTTPUploader(filename, session);
            uploader.setFriendId(friendID);
        }

        String method = request.getRequestLine().getMethod();
        uploader.setMethod(method);
        uploader.setUploadType("HEAD".equals(method) ? UploadType.HEAD_REQUEST
                : type);
        session.setUploader(uploader);
        return uploader;
    }

    public HTTPUploader getUploader(HttpContext context) {
        assert started;
        
        HTTPUploadSession session = getSession(context);
        HTTPUploader uploader = session.getUploader();
        assert uploader != null;
        return uploader;
    }

    public QueueStatus enqueue(HttpContext context, HttpRequest request) {
        assert started;
        
        HTTPUploadSession session = getSession(context);
        assert !session.isAccepted();

        if (shouldBypassQueue(request, session.getUploader())) {
            session.setQueueStatus(QueueStatus.BYPASS);
        } else if (HttpContextParams.isLocal(context)) {
            session.setQueueStatus(QueueStatus.ACCEPTED);
        } else {
            session.setQueueStatus(checkAndQueue(session, context));
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Queued upload: " + session);

        return session.getQueueStatus();
    }

    /** For testing: removes all uploaders and clears the request cache. */
    public void cleanup() {
        assert started;
        
        for (HTTPUploader uploader : activeUploadList
                .toArray(new HTTPUploader[0])) {
            uploader.stop();
            slotManager.cancelRequest(uploader.getSession());
            removeFromList(uploader);
        }
        slotManager.cleanup();
        REQUESTS.clear();
    }

    /**
     * Manages the {@link HTTPUploadSession} associated with a connection.
     */
    private class ResponseListener implements HttpAcceptorListener {

        public void connectionOpen(NHttpConnection conn) {
        }

        public void connectionClosed(NHttpConnection conn) {
            assert started;
            
            HTTPUploadSession session = getSession(conn.getContext());
            if (session != null) {
                HTTPUploader uploader = session.getUploader();
                if (uploader != null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Closing session: " + session);

                    boolean stillInQueue = slotManager.positionInQueue(session) > -1;
                    slotManager.cancelRequest(session);
                    if (stillInQueue) {
                        // If it was queued, also set the state to INTERRUPTED
                        // (changing from COMPLETE)
                        uploader.setState(UploadStatus.INTERRUPTED);
                    } else if (uploader.getState() != UploadStatus.COMPLETE) {
                        // the complete state may have been set by
                        // responseSent() already
                        uploader.setState(UploadStatus.COMPLETE);
                    }
                    uploader.setLastResponse(null);
                    cleanupFinishedUploader(uploader);
                    session.setUploader(null);
                }
            }
        }
        
        public void responseSent(NHttpConnection conn, HttpResponse response) {
            assert started;
            
            HTTPUploadSession session = getSession(conn.getContext());
            if (session != null) {
                HTTPUploader uploader = session.getUploader();
                if (uploader != null && uploader.getLastResponse() == response) {
                    uploader.setLastResponse(null);
                    uploader.setState(UploadStatus.COMPLETE);
                }

                if (session.getQueueStatus() == QueueStatus.QUEUED) {
                    session.getIOSession().setSocketTimeout(
                            HTTPUploadSession.MAX_POLL_TIME);
                    conn.requestInput();  // make sure the new socket timeout is used.
                }
            }
        }

    }

}
