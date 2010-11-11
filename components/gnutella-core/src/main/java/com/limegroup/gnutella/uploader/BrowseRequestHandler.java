package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.ContentEncoderChannel;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.MultiIterable;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.http.HttpCoreUtils;
import org.limewire.http.entity.AbstractProducingNHttpEntity;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.GUID;
import org.limewire.nio.channel.NoInterestWritableByteChannel;

import com.google.inject.Provider;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.connection.BasicQueue;
import com.limegroup.gnutella.connection.ConnectionStats;
import com.limegroup.gnutella.connection.MessageWriter;
import com.limegroup.gnutella.connection.SentMessageHandler;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

/**
 * Responds to Gnutella browse requests by returning a list of all shared files.
 * Only supports the application/x-gnutella-packets mime-type, browsing through
 * HTML is not supported.
 */
public class BrowseRequestHandler extends SimpleNHttpRequestHandler {

    private static final Log LOG = LogFactory.getLog(BrowseRequestHandler.class);
    
    private final HTTPUploadSessionManager sessionManager;
    private final Provider<ResponseFactory> responseFactory;
    private final OutgoingQueryReplyFactory outgoingQueryReplyFactory;
    /**
     * This is set to true as default while old clients still don't send 
     * the request header correctly. Will be set to false in the future.
     */
    private boolean requestorCanDoFWT = true;

    private final HttpRequestFileViewProvider browseRequestFileListProvider;
    private final BrowseTracker tracker;

    BrowseRequestHandler(HTTPUploadSessionManager sessionManager,
            Provider<ResponseFactory> responseFactory,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            HttpRequestFileViewProvider browseRequestFileListProvider,
            BrowseTracker tracker) {
        this.sessionManager = sessionManager;
        this.responseFactory = responseFactory;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
        this.browseRequestFileListProvider = browseRequestFileListProvider;
        this.tracker = tracker;
    }
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {

        if (request.containsHeader(HTTPHeaderName.FW_NODE_INFO.toString())) {
            requestorCanDoFWT = true;
        }
        HTTPUploader uploader = null;
        try {
            // TODO handler code should not know that much about request uris
            String uri = request.getRequestLine().getUri();
            String friendID;
            if(uri.equals("/") || uri.startsWith("/?")) {
                friendID = null;
                uploader = sessionManager.getOrCreateUploader(request,
                        context, UploadType.BROWSE_HOST, "");
            } else {
                friendID = getFriend(request);
                tracker.browsed(friendID);
                uploader = sessionManager.getOrCreateUploader(request,
                        context, UploadType.BROWSE_HOST, friendID, friendID);
            }
            uploader.setState(UploadStatus.BROWSE_HOST);
            Iterable<FileView> lists = browseRequestFileListProvider.getFileViews(friendID, context);
            List<Iterable<FileDesc>> iterables = new ArrayList<Iterable<FileDesc>>();
            for (FileView list : lists) {
                iterables.add(list.pausableIterable());
            }
            Iterable<FileDesc> files = new MultiIterable<FileDesc>(iterables.toArray(new Iterable[0]));
            if (!HttpCoreUtils.hasHeaderListValue(request, "Accept", Constants.QUERYREPLY_MIME_TYPE)) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Browse request is missing Accept header");
                
                response.setStatusCode(HttpStatus.SC_NOT_ACCEPTABLE);
            } else {
                response.setEntity(new BrowseResponseEntity(uploader, files, shouldIncludeNMS1Urns(request)));
                response.setStatusCode(HttpStatus.SC_OK);
            }
        } catch (com.limegroup.gnutella.uploader.HttpException he) {
            LOG.debug("invalid request", he);
            response.setStatusCode(he.getErrorCode());
            response.setReasonPhrase(he.getMessage());
        }
        
        if (uploader == null) {
            uploader = sessionManager.getOrCreateUploader(request, context, UploadType.BROWSE_HOST, I18nMarker.marktr("Browse"));
        }
        sessionManager.sendResponse(uploader, response);
    }
    
    static boolean shouldIncludeNMS1Urns(HttpRequest request) {
        boolean includeNMS1Urns = HttpCoreUtils.hasHeaderListValue(request, HTTPHeaderName.NMS1.httpStringValue(), "1");
        if (includeNMS1Urns) {
            return true;
        }
        Map<String, String> query = HttpCoreUtils.parseQuery(request.getRequestLine().getUri(), null);
        String nms1 = query.get("nms1");
        if (nms1 != null && nms1.equals("1")) {
            return true;
        }
        return false;
    }
    
    /**
     * Parses out the last element of the request uri's path and returns it.
     * @throws com.limegroup.gnutella.uploader.HttpException if there was no such element
     */
    String getFriend(HttpRequest request) throws com.limegroup.gnutella.uploader.HttpException {
        RequestLine requestLine = request.getRequestLine();
        try {
            URI uri = new URI(requestLine.getUri());
            String path = uri.getPath();
            if (path == null) {
                throw new com.limegroup.gnutella.uploader.HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
            }
            if (path.endsWith("/")) {
                int previousSlash = path.lastIndexOf('/', path.length() - 2);
                if (previousSlash != -1) {
                    return path.substring(previousSlash + 1, path.length() - 1);
                }
            } else {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    return path.substring(lastSlash + 1);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        throw new com.limegroup.gnutella.uploader.HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
    }

    public class BrowseResponseEntity extends AbstractProducingNHttpEntity {

        private static final int RESPONSES_PER_REPLY = 10;
        
        private static final int MAX_PENDING_REPLIES = 5;

        private final HTTPUploader uploader;

        private Iterator<FileDesc> iterator;
        
        private MessageWriter sender;
        
        private volatile int pendingMessageCount = 0;

        private GUID sessionGUID = new GUID();
        
        private final boolean includeNMS1Urn;

        public BrowseResponseEntity(HTTPUploader uploader, Iterable<FileDesc> files, boolean includeNMS1Urn) {
            this.uploader = uploader;
            iterator = files.iterator();
            this.includeNMS1Urn = includeNMS1Urn;
            
            // XXX LW can't handle chunked responses: CORE-199
            //setChunked(true);
            
            setContentType(Constants.QUERYREPLY_MIME_TYPE);
        }

        @Override
        public long getContentLength() {
            return -1;
        }
        
        @Override
        public void initialize(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
            SentMessageHandler sentMessageHandler = new SentMessageHandler() {
                public void processSentMessage(Message m) {
                    uploader.addAmountUploaded(m.getTotalLength());
                    pendingMessageCount--;
                }                
            };
            
            sender = new MessageWriter(new ConnectionStats(), new BasicQueue(), sentMessageHandler);
            sender.setWriteChannel(new NoInterestWritableByteChannel(new ContentEncoderChannel(
                    contentEncoder)));
        }
        
        @Override
        public boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
            addMessages();
            
            boolean more = sender.handleWrite();
            assert more || pendingMessageCount == 0;
            
            activateTimeout();
            return more || iterator.hasNext();
        }
        
        /**
         * Adds a query reply with {@link #RESPONSES_PER_REPLY} responses to the
         * message queue.
         */
        private void addMessages() {
            if (pendingMessageCount >= MAX_PENDING_REPLIES) {
                return;
            }
            
            List<Response> responses = new ArrayList<Response>(RESPONSES_PER_REPLY); 
            for (int i = 0; iterator.hasNext() && i < RESPONSES_PER_REPLY; i++) {
                FileDesc fileDesc = iterator.next();
                Response response = responseFactory.get().createResponse(fileDesc, includeNMS1Urn); 
                responses.add(response);
            }
            
            Iterable<QueryReply> it = outgoingQueryReplyFactory.createReplies(responses.toArray(new Response[0]),
                    10, null, sessionGUID.bytes(), (byte)1, false, requestorCanDoFWT);
            
            for (QueryReply queryReply : it) {
                sender.send(queryReply);
                pendingMessageCount++;
            }
        }

        public void finish() {
            deactivateTimeout();
            sender = null;
        }

        @Override
        public void timeout() {
            if (LOG.isDebugEnabled())
                LOG.debug("Browse request timed out");

            uploader.stop();
        }
        
    }

}