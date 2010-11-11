package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.settings.SharingSettings;
import org.limewire.http.BasicHeaderProcessor;
import org.limewire.http.MalformedHeaderException;
import org.limewire.http.RangeHeaderInterceptor;
import org.limewire.http.RangeHeaderInterceptor.Range;

import com.google.inject.Provider;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.http.AltLocHeaderInterceptor;
import com.limegroup.gnutella.http.FWNodeInfoInterceptor;
import com.limegroup.gnutella.http.FeatureHeaderInterceptor;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.UserAgentHeaderInterceptor;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandler;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandlerFactory;
import com.limegroup.gnutella.uploader.FileRequestParser.FileRequest;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager.QueueStatus;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

/**
 * Handles upload requests for files and THEX trees.
 * 
 * @see FileResponseEntity
 * @see THEXResponseEntity
 */
public class FileRequestHandler extends SimpleNHttpRequestHandler {

    private static final Log LOG = LogFactory.getLog(FileRequestHandler.class);

    /**
     * Constant for the amount of time to wait before retrying if we are not
     * actively downloading this file. (1 hour)
     * <p>
     * The value is meant to be used only as a suggestion to when newer ranges
     * may be available if we do not have any ranges that the downloader may
     * want.
     */
    private static final String INACTIVE_RETRY_AFTER = "" + (60 * 60);

    private final HTTPUploadSessionManager sessionManager;

    private final Library library;

    private final HTTPHeaderUtils httpHeaderUtils;

    private final HttpRequestHandlerFactory httpRequestHandlerFactory;

    private final Provider<CreationTimeCache> creationTimeCache;

    private final FileResponseEntityFactory fileResponseEntityFactory;

    private final AltLocManager altLocManager;

    private final AlternateLocationFactory alternateLocationFactory;

    private final Provider<DownloadManager> downloadManager;

    private final Provider<HashTreeCache> tigerTreeCache;

    private final PushEndpointFactory pushEndpointFactory;

    private final HashTreeWriteHandlerFactory tigerWriteHandlerFactory;

    private final HttpRequestFileViewProvider fileListProvider;

    FileRequestHandler(HTTPUploadSessionManager sessionManager, Library library,
            HTTPHeaderUtils httpHeaderUtils, HttpRequestHandlerFactory httpRequestHandlerFactory,
            Provider<CreationTimeCache> creationTimeCache,
            FileResponseEntityFactory fileResponseEntityFactory, AltLocManager altLocManager,
            AlternateLocationFactory alternateLocationFactory,
            Provider<DownloadManager> downloadManager, Provider<HashTreeCache> tigerTreeCache,
            PushEndpointFactory pushEndpointFactory,
            HashTreeWriteHandlerFactory tigerWriteHandlerFactory, 
            HttpRequestFileViewProvider fileListProvider) {
        this.sessionManager = sessionManager;
        this.library = library;
        this.httpHeaderUtils = httpHeaderUtils;
        this.httpRequestHandlerFactory = httpRequestHandlerFactory;
        this.creationTimeCache = creationTimeCache;
        this.fileResponseEntityFactory = fileResponseEntityFactory;
        this.altLocManager = altLocManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadManager = downloadManager;
        this.tigerTreeCache = tigerTreeCache;
        this.pushEndpointFactory = pushEndpointFactory;
        this.tigerWriteHandlerFactory = tigerWriteHandlerFactory;
        this.fileListProvider = fileListProvider;
    }
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Handling upload request: " + request.getRequestLine().getUri());

        FileRequest fileRequest = null;
        HTTPUploader uploader = null;

        try {
            fileRequest = FileRequestParser.parseRequest(fileListProvider, request.getRequestLine().getUri(), context);
        } catch (IOException e) {
            uploader = sessionManager.getOrCreateUploader(request, context,
                    UploadType.MALFORMED_REQUEST, "Malformed Request");
            handleMalformedRequest(response, uploader);
        }
        
        if(fileRequest != null) {
            assert uploader == null;
            uploader = findFileAndProcessHeaders(request, response, context, fileRequest);
        } else if(uploader == null) {
            uploader = sessionManager.getOrCreateUploader(request, context,
                    UploadType.FILE_NOT_FOUND, "Unknown Query");
            uploader.setState(UploadStatus.FILE_NOT_FOUND);
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        }
        
        assert uploader != null;

        sessionManager.sendResponse(uploader, response);
    }

    /**
     * Looks up file and processes request headers.
     */
    private HTTPUploader findFileAndProcessHeaders(HttpRequest request, HttpResponse response,
            HttpContext context, FileRequest fileRequest) throws IOException, HttpException {
        FileDesc fileDesc = fileRequest.getFileDesc();
        // create uploader
        UploadType type = (LibraryUtils.isForcedShare(fileDesc)) ? UploadType.FORCED_SHARE
                : UploadType.SHARED_FILE;
        HTTPUploader uploader = sessionManager.getOrCreateUploader(request, context, type, fileDesc.getFileName(), fileRequest.getFriendID());
        uploader.setFileDesc(fileDesc);

        // process headers
        BasicHeaderProcessor processor = new BasicHeaderProcessor();
        RangeHeaderInterceptor rangeHeaderInterceptor = new RangeHeaderInterceptor();
        processor.addInterceptor(rangeHeaderInterceptor);
        processor.addInterceptor(new FeatureHeaderInterceptor(uploader));
        processor.addInterceptor(new AltLocHeaderInterceptor(uploader, altLocManager,
                alternateLocationFactory));
        processor.addInterceptor(new FWNodeInfoInterceptor(uploader, pushEndpointFactory));
        if (!uploader.getFileName().toUpperCase(Locale.US).startsWith("LIMEWIRE")) {
            processor.addInterceptor(new UserAgentHeaderInterceptor(uploader));
        }
        try {
            processor.process(request, context);
        } catch (ProblemReadingHeaderException e) {
            handleMalformedRequest(response, uploader);
            return uploader;
        } catch (MalformedHeaderException e) {
            handleMalformedRequest(response, uploader);
            return uploader;
        }

        if (UserAgentHeaderInterceptor.isFreeloader(uploader.getUserAgent())) {
            sessionManager.handleFreeLoader(request, response, context, uploader);
            return uploader;
        }

        if (!validateHeaders(uploader, fileRequest.isThexRequest())) {
            uploader.setState(UploadStatus.FILE_NOT_FOUND);
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return uploader;
        }

        if (!fileRequest.isThexRequest()) {
            if (rangeHeaderInterceptor.hasRequestedRanges()) {
                Range[] ranges = rangeHeaderInterceptor.getRequestedRanges();
                if (ranges.length > 1) {
                    handleInvalidRange(response, uploader, fileDesc);
                    return uploader;
                }

                uploader.setUploadBegin(ranges[0].getStartOffset(uploader.getFileSize()));
                uploader.setUploadEnd(ranges[0].getEndOffset(uploader.getFileSize()) + 1);
                uploader.setContainedRangeRequest(true);
            }

            if (!uploader.validateRange()) {
                handleInvalidRange(response, uploader, fileDesc);
                return uploader;
            }
        }

        // start upload
        if (fileRequest.isThexRequest()) {
            handleTHEXRequest(request, response, context, uploader, fileDesc);
        } else {
            handleFileUpload(context, request, response, uploader, fileDesc, fileRequest.getFriendID());
        }

        return uploader;
    }

    private void handleMalformedRequest(HttpResponse response, HTTPUploader uploader) {
        uploader.setState(UploadStatus.MALFORMED_REQUEST);
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        response.setReasonPhrase("Malformed Request");
    }

    /**
     * Enqueues <code>request</code> and handles <code>uploader</code> in
     * respect to the returned queue status.
     * 
     * @param friendId can be null if not a friend upload
     */
    private void handleFileUpload(HttpContext context, HttpRequest request, HttpResponse response,
            HTTPUploader uploader, FileDesc fd, String friendId) throws HttpException, IOException {
        if (!uploader.getSession().isAccepted()) {
            QueueStatus queued = sessionManager.enqueue(context, request);
            switch (queued) {
            case REJECTED:
                httpRequestHandlerFactory.createLimitReachedRequestHandler(uploader).handle(
                        request, response, context);
                break;
            case BANNED:
                uploader.setState(UploadStatus.BANNED_GREEDY);
                response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                response.setReasonPhrase("Banned");
                break;
            case QUEUED:
                handleQueued(context, request, response, uploader, fd, friendId);
                break;
            case ACCEPTED:
                sessionManager.addAcceptedUploader(uploader, context);
                break;
            case BYPASS: // ignore
            }
        }

        if (uploader.getSession().canUpload()) {
            handleAccept(context, request, response, uploader, fd, friendId);
        }
    }

    /**
     * Processes an accepted file upload by adding headers and setting the
     * entity.
     * 
     * @param friendId can be null if not a friend upload
     */
    protected void handleAccept(HttpContext context, HttpRequest request, HttpResponse response,
            HTTPUploader uploader, FileDesc fd, String friendId) throws IOException, HttpException {

        assert fd != null;

        response.addHeader(HTTPHeaderName.DATE.create(HTTPUtils.getDateValue()));
        response.addHeader(HTTPHeaderName.CONTENT_DISPOSITION.create("attachment; filename=\""
                + HTTPUtils.encode(uploader.getFileName(), "US-ASCII") + "\""));

        if (uploader.containedRangeRequest()) {
            // uploadEnd is an EXCLUSIVE index internally, but HTTP uses an
            // INCLUSIVE index.
            String value = "bytes " + uploader.getUploadBegin() + "-"
                    + (uploader.getUploadEnd() - 1) + "/" + uploader.getFileSize();
            response.addHeader(HTTPHeaderName.CONTENT_RANGE.create(value));
        }

        httpHeaderUtils.addAltLocationsHeader(response, uploader.getAltLocTracker(), altLocManager);
        httpHeaderUtils.addRangeHeader(response, uploader, fd);
        httpHeaderUtils.addProxyHeader(response);

        URN urn = fd.getSHA1Urn();
        if (uploader.isFirstReply()) {
            // write the creation time if this is the first reply.
            // if this is just a continuation, we don't need to send
            // this information again.
            // it's possible t do that because we don't use the same
            // uploader for different files
            if (creationTimeCache.get().getCreationTime(urn) != null) {
                response.addHeader(HTTPHeaderName.CREATION_TIME.create(creationTimeCache.get()
                        .getCreationTime(urn).toString()));
            }
        }

        // write x-features header once because the downloader is
        // supposed to cache that information anyway
        if (uploader.isFirstReply()) {
            httpHeaderUtils.addFeatures(response);
        }

        addThexUriHeader(response, fd, friendId);

        response.setEntity(fileResponseEntityFactory.createFileResponseEntity(uploader, fd
                .getFile()));
        uploader.setState(UploadStatus.UPLOADING);

        if (uploader.isPartial()) {
            response.setStatusCode(HttpStatus.SC_PARTIAL_CONTENT);
        } else {
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }
    
    /**
     * Adds the X-Thex-URI header to the http response.
     * 
     * @param friendId can be null if not a friend upload, otherwise used to
     * create the correct path for the Thex uri
     */
    private void addThexUriHeader(HttpResponse response, FileDesc fileDesc, String friendId) {
        // We do not support serving TigerTrees for incomplete files,
        // so sending the THEX-URI makes no sense for them.
        if (fileDesc instanceof IncompleteFileDesc) {
            return;
        }
        
        // write X-Thex-URI header with root hash if we have already
        // calculated the tigertree
        HashTree tree = tigerTreeCache.get().getHashTree(fileDesc);
        if (tree != null) {
            if (friendId != null) {
                // TODO: breaking dependencies: prefix same as in CoreGlueFriendService
                try {
                    String uri = "/friend/download/" + URLEncoder.encode(friendId, "UTF-8")+ tree.httpStringValue();
                    response.addHeader(HTTPHeaderName.THEX_URI.create(uri));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                response.addHeader(HTTPHeaderName.THEX_URI.create(tree));
            }
        }

    }

    /**
     * Processes an accepted THEX tree upload by adding headers and setting the
     * entity.
     */
    private void handleTHEXRequest(HttpRequest request, HttpResponse response, HttpContext context,
            HTTPUploader uploader, FileDesc fd) {
        // reset the poll interval to allow subsequent requests right away
        uploader.getSession().updatePollTime(QueueStatus.BYPASS);

        // do not count THEX transfers towards the total amount
        uploader.setIgnoreTotalAmountUploaded(true);

        HashTree tree = tigerTreeCache.get().getHashTree(fd);
        if (tree == null) {
            // tree was requested before hashing completed
            uploader.setState(UploadStatus.FILE_NOT_FOUND);
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return;
        }

        HashTreeWriteHandler tigerWriteHandler = tigerWriteHandlerFactory
                .createTigerWriteHandler(tree);

        // XXX reset range to size of THEX tree
        int outputLength = tigerWriteHandler.getOutputLength();
        uploader.setFileSize(outputLength);
        uploader.setUploadBegin(0);
        uploader.setUploadEnd(outputLength);

        // see CORE-174
        // response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd.getSHA1Urn()));

        uploader.setState(UploadStatus.THEX_REQUEST);
        response.setEntity(new THEXResponseEntity(uploader, tigerWriteHandler, uploader
                .getFileSize()));
        response.setStatusCode(HttpStatus.SC_OK);
    }

    /**
     * Processes a request for an invalid range.
     */
    private void handleInvalidRange(HttpResponse response, HTTPUploader uploader, FileDesc fd) {
        httpHeaderUtils.addAltLocationsHeader(response, uploader.getAltLocTracker(), altLocManager);
        httpHeaderUtils.addRangeHeader(response, uploader, fd);
        httpHeaderUtils.addProxyHeader(response);

        if (fd instanceof IncompleteFileDesc) {
            if (!downloadManager.get().isActivelyDownloading(fd.getSHA1Urn())) {
                response.addHeader(HTTPHeaderName.RETRY_AFTER.create(INACTIVE_RETRY_AFTER));
            }
        }

        uploader.setState(UploadStatus.UNAVAILABLE_RANGE);
        response.setStatusCode(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setReasonPhrase("Requested Range Unavailable");
    }

    /**
     * Processes a queued file upload by adding headers.
     * 
     * @param friendId can be null if not a friend upload
     */
    private void handleQueued(HttpContext context, HttpRequest request, HttpResponse response,
            HTTPUploader uploader, FileDesc fd, String friendId)  {
        // if not queued, this should never be the state
        int position = uploader.getSession().positionInQueue();
        assert (position != -1);

        String value = "position=" + (position + 1) + ", pollMin="
                + (HTTPUploadSession.MIN_POLL_TIME / 1000) + /* mS to S */
                ", pollMax=" + (HTTPUploadSession.MAX_POLL_TIME / 1000) /*
                                                                         * mS to
                                                                         * S
                                                                         */;
        response.addHeader(HTTPHeaderName.QUEUE.create(value));

        httpHeaderUtils.addAltLocationsHeader(response, uploader.getAltLocTracker(), altLocManager);
        httpHeaderUtils.addRangeHeader(response, uploader, fd);

        if (uploader.isFirstReply()) {
            httpHeaderUtils.addFeatures(response);
        }

        addThexUriHeader(response, fd, friendId);
        
        response.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);

        uploader.setState(UploadStatus.QUEUED);
        response.setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    private boolean validateHeaders(HTTPUploader uploader, boolean thexRequest) {
        final FileDesc fd = uploader.getFileDesc();
        assert fd != null;

        // If it's the wrong URN, File Not Found it.
        URN urn = uploader.getRequestedURN();
        if (urn != null && !fd.containsUrn(urn)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid content urn: " + uploader);
            }
            return false;
        }

        // handling THEX Requests
        if (thexRequest && tigerTreeCache.get().getHashTree(fd) == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Requested thex tree is not available: " + uploader);
            }
            return false;
        }

        // special handling for incomplete files
        if (fd instanceof IncompleteFileDesc) {
            // Check to see if we're allowing PFSP.
            if (!SharingSettings.ALLOW_PARTIAL_SHARING.getValue()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sharing of partial files is diabled: " + uploader);
                }
                return false;
            }

            // cannot service THEX requests for partial files
            if (thexRequest) {
                return false;
            }
        } else {
            // check if fd is up-to-date
            File file = fd.getFile();
            if (file.lastModified() != fd.lastModified()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("File has changed on disk, resharing: " + file);
                }
                library.fileChanged(file, fd.getLimeXMLDocuments());
                return false;
            }
        }

        return true;
    }

}
