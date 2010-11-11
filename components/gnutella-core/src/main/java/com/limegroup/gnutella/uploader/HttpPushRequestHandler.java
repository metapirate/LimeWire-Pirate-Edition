package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.util.Base32;

import com.google.inject.Inject;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Handles HTTP push requests by proxying them and sending them to the specified
 * client.
 */
public class HttpPushRequestHandler extends SimpleNHttpRequestHandler {

    private static final Log LOG = LogFactory.getLog(HttpPushRequestHandler.class);

    public static final String P_SERVER_ID = "ServerId";

    public static final String P_GUID = "guid";

    public static final String P_FILE = "file";
    
    public static final String P_TLS = "tls";

    private HTTPUploadSessionManager sessionManager;

    private MessageRouter messageRouter;

    @Inject
    HttpPushRequestHandler(HTTPUploadSessionManager sessionManager, MessageRouter messageRouter) {
        if (sessionManager == null) {
            throw new IllegalArgumentException();
        }
        if (messageRouter == null) {
            throw new IllegalArgumentException();
        }
        
        this.sessionManager = sessionManager;
        this.messageRouter = messageRouter;
    }
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        HTTPUploader uploader = null;
        
        HttpPushRequest pushRequest = parsePushRequest(request);
        if (pushRequest == null) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            uploader = sessionManager.getOrCreateUploader(request,
                    context, UploadType.MALFORMED_REQUEST,
                    "Malformed Request");
            uploader.setState(UploadStatus.MALFORMED_REQUEST);
        } else {
            uploader = sessionManager.getOrCreateUploader(request,
                    context, UploadType.PUSH_PROXY, pushRequest.clientGUID);
            uploader.setState(UploadStatus.PUSH_PROXY);
            if (!sendRequest(pushRequest)) {
                response.setStatusCode(HttpStatus.SC_GONE);
                response.setReasonPhrase("Servent not connected");
            } else {
                response.setStatusCode(HttpStatus.SC_ACCEPTED);
                response.setReasonPhrase("Message sent");
            }
        }
        
        sessionManager.sendResponse(uploader, response);
    }

    /**
     * Returns the push request from <code>request</code>.
     *
     * @return null, if the request was not valid
     */
    private HttpPushRequest parsePushRequest(HttpRequest request) {
        String uri = request.getRequestLine().getUri();
        // start after the '?'
        int i = uri.indexOf('?');
        if (i == -1) {
            return null;
        }

        String queryString = uri.substring(i + 1);

        StringTokenizer t = new StringTokenizer(queryString, "=&");
        if (t.countTokens() < 2 || t.countTokens() % 2 != 0) {
            return null;
        }

        String clientGUID = null;
        int fileIndex = 0;
        boolean useTLS = false;

        while (t.hasMoreTokens()) {
            final String key = t.nextToken();
            final String val = t.nextToken();
            if (key.equalsIgnoreCase(P_SERVER_ID)) {
                if (clientGUID != null) // already have a name?
                    return null;
                // must convert from base32 to base 16.
                byte[] base16 = Base32.decode(val);
                if (base16.length != 16)
                    return null;
                clientGUID = new GUID(base16).toHexString();
            } else if (key.equalsIgnoreCase(P_GUID)) {
                if (clientGUID != null || val.length() != 32)
                    return null;
                // already in base16
                clientGUID = val;
            } else if (key.equalsIgnoreCase(P_FILE)) {
                if (fileIndex != 0)
                    return null;
                try {
                    fileIndex = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (fileIndex < 0)
                    return null;
            } else if (key.equalsIgnoreCase(P_TLS)) {
                useTLS = "true".equalsIgnoreCase(val);
            }
        }

        if (clientGUID == null) {
            return null;
        }

        Header header = request.getLastHeader(HTTPHeaderName.NODE
                .httpStringValue());
        if (header == null) {
            LOG.info("Missing X-Node header push proxy request");
            return null;
        }
            
        InetSocketAddress address = getNodeAddress(header.getValue());
        if (address == null) {
            LOG.info("Invalid node address for push proxy request: " + header.getValue());
            return null;
        }

        return new HttpPushRequest(clientGUID, fileIndex, address, useTLS);
    }

    private InetSocketAddress getNodeAddress(String value) {
        StringTokenizer t = new StringTokenizer(value, ":");
        if (t.countTokens() == 2) {
            try {
                InetAddress address = InetAddress.getByName(t.nextToken()
                        .trim());
                int port = Integer.parseInt(t.nextToken().trim());
                if (NetworkUtils.isValidAddress(address)
                        && NetworkUtils.isValidPort(port)) {
                    return new InetSocketAddress(address, port);
                }
            } catch (UnknownHostException badHost) {
            } catch (NumberFormatException nfe) {
            }
        }
        return null;
    }

    /**
     * Sends <code>request</code> through {@link MessageRouter}.
     * 
     * @return false, if sending failed
     */
    private boolean sendRequest(HttpPushRequest request) {
        byte[] clientGUID = GUID.fromHexString(request.getClientGUID());
        PushRequest push = new PushRequestImpl(GUID.makeGuid(), (byte) 0,
                clientGUID, request.getFileIndex(), request.getAddress()
                        .getAddress().getAddress(), request.getAddress()
                        .getPort(), Network.TCP, request.isUseTLS());
        try {
            messageRouter.sendPushRequest(push);
        } catch (IOException e) {
            LOG.debug("Sending of push proxy request failed", e);
            return false;
        }
        return true;
    }

    private static class HttpPushRequest {

        private String clientGUID;

        private int fileIndex;

        private InetSocketAddress address;
        
        private boolean useTLS;

        public HttpPushRequest(String clientGUID, int fileIndex,
                InetSocketAddress address, boolean useTLS) {
            this.clientGUID = clientGUID;
            this.fileIndex = fileIndex;
            this.address = address;
            this.useTLS = useTLS;
        }

        public String getClientGUID() {
            return clientGUID;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public InetSocketAddress getAddress() {
            return address;
        }
        
        public boolean isUseTLS() {
            return useTLS;
        }

    }

}