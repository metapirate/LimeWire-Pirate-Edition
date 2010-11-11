package com.limegroup.gnutella;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.settings.SearchSettings;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.Feature;
import org.limewire.http.httpclient.SocketWrapperProtocolSocketFactory;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.BlockingConnectObserver;
import org.limewire.net.SocketsManager;
import org.limewire.util.StringUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Handles all stuff necessary for browsing of networks hosts. 
 * Has a instance component, one per browse host, and a static Map of instances
 * that is used to coordinate between replies to PushRequests.
 */
public class BrowseHostHandler {
    
    private static final Log LOG = LogFactory.getLog(BrowseHostHandler.class);
    
    /**
     * Various internal states for Browse-Hosting.
     */
    private static final int NOT_STARTED = -1;
    private static final int STARTED = 0;
    private static final int DIRECTLY_CONNECTING = 1;
    private static final int PUSHING = 2;
    private static final int EXCHANGING = 3;
    private static final int FINISHED = 4;
    private static final int CONNECTING = 5;

    static final int DIRECT_CONNECT_TIME = 10000; // 10 seconds.

    private static final long EXPIRE_TIME = 15000; // 15 seconds

    /** The GUID to be used for incoming QRs from the Browse Request. */
    private GUID _guid = null;

    /** The total length of the http-reply. */
    private volatile long _replyLength = 0;    
    /** The current length of the reply. */
    private volatile long _currentLength = 0;    
    /** The current state of this BH. */
    private volatile int _state = NOT_STARTED;    
    /** The time this state started. */
    private volatile long _stateStarted = 0;

    private final SocketsManager socketsManager;
    private final Provider<ForMeReplyHandler> forMeReplyHandler;

    private final MessageFactory messageFactory;
    private final Provider<HttpParams> httpParams;
    private final NetworkManager networkManager;

    private final PushEndpointFactory pushEndpointFactory;



    /**
     * @param sessionGuid The GUID you have associated on the front end with the
     *        results of this Browse Host request.
     * @param clientProvider used to make an HTTP client request over an *incoming* Socket
     */
    BrowseHostHandler(GUID sessionGuid, 
                      SocketsManager socketsManager,
                      Provider<ForMeReplyHandler> forMeReplyHandler,
                      MessageFactory messageFactory,
                      Provider<HttpParams> httpParams,
                      NetworkManager networkManager,
                      PushEndpointFactory pushEndpointFactory) {
        _guid = sessionGuid;
        this.socketsManager = socketsManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.messageFactory = messageFactory;
        this.httpParams = httpParams;
        this.networkManager = networkManager;
        this.pushEndpointFactory = pushEndpointFactory;
    }

    public void browseHost(FriendPresence friendPresence, BrowseListener browseListener) {
        setState(STARTED);
        setState(CONNECTING);
        
        try {
            AddressFeature addressFeature = (AddressFeature)friendPresence.getFeature(AddressFeature.ID);
            if(addressFeature != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("browsing address: " + addressFeature.getFeature());
                }
                Socket socket = socketsManager.connect(addressFeature.getFeature(), new BlockingConnectObserver()).getSocket(EXPIRE_TIME, TimeUnit.MILLISECONDS);
                browseHost(socket, friendPresence);
                browseListener.browseFinished(true);
                return;
            }
        } catch (IOException ie) {
            if (LOG.isInfoEnabled())
                LOG.info("Error during browse host: " + friendPresence, ie);
        } catch (URISyntaxException e) {
            LOG.info("Error during browse host", e);
        } catch (HttpException e) {
            LOG.info("Error during browse host", e);
        } catch (InterruptedException e) {
            LOG.info("Error during browse host", e);
        } catch (TimeoutException e) {
            LOG.info("Error during browse host", e);
        }
        browseListener.browseFinished(false);
        failed();
    }
    
    /**
     * Returns the current percentage complete of the state
     * of the browse host.
     */
    public double getPercentComplete(long currentTime) {
        long elapsed;
        
        switch(_state) {
        case NOT_STARTED: return 0d;
        case STARTED: return 0d;
        case DIRECTLY_CONNECTING:
            // return how long it'll take to connect.
            elapsed = currentTime - _stateStarted;
            return (double) elapsed / DIRECT_CONNECT_TIME;
        case PUSHING:
        case CONNECTING:
            // return how long it'll take to push.
            elapsed = currentTime - _stateStarted;
            return (double) elapsed / EXPIRE_TIME;
        case EXCHANGING:
            // return how long it'll take to finish reading,
            // or stay at .5 if we dunno the length.
            if( _replyLength > 0 )
                return (double)_currentLength / _replyLength;
            else
                return 0.5;
        case FINISHED:
            return 1.0;
        default:
            throw new IllegalStateException("invalid state");
        }
    }
        
    /**
     * Sets the state and state-time.
     */
    private void setState(int state) {
        _state = state;
        _stateStarted = System.currentTimeMillis();
    }    
     
    /**
     * Indicates that this browse host has failed.
     */   
    void failed() {
        setState(FINISHED);
    }

    void browseHost(Socket socket, FriendPresence friendPresence) throws IOException, URISyntaxException, HttpException, InterruptedException {
    	try {
            setState(EXCHANGING);
            HttpResponse response = makeHTTPRequest(socket, friendPresence);
            validateResponse(response);
            readQueryRepliesFromStream(response, friendPresence);
        } finally {
            IOUtils.close(socket);
    		setState(FINISHED);
    	}
    }

    private HttpResponse makeHTTPRequest(Socket socket, FriendPresence friendPresence) throws IOException, URISyntaxException, HttpException, InterruptedException {
//        SocketWrappingHttpClient client = clientProvider.get();
//        client.setSocket(socket);
        SocketWrappingHttpClient client = new SocketWrappingHttpClient(socket);
        if(!friendPresence.getFriend().isAnonymous()) {
            String username = friendPresence.getFriend().getNetwork().getCanonicalizedLocalID();
            Feature feature = friendPresence.getFeature(AuthTokenFeature.ID);
            if(feature != null) {
                AuthTokenFeature authTokenFeature = (AuthTokenFeature)feature;
                String password = StringUtils.toUTF8String(authTokenFeature.getFeature().getToken());
                client.setCredentials(new UsernamePasswordCredentials(username, password));
            } else {
                LOG.infof("no auth token for: {0}", friendPresence);
            }
        }
        // hardcoding to "http" should work;
        // socket has already been established
        HttpGet get = new HttpGet("http://" +
                NetworkUtils.ip2string(socket.getInetAddress().getAddress()) +
                ":" + socket.getPort() +
                getPath(friendPresence));
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
        
        get.addHeader(HTTPHeaderName.HOST.create(NetworkUtils.ip2string(socket.getInetAddress().getAddress()) + ":" + socket.getPort()));
        get.addHeader(HTTPHeaderName.USER_AGENT.create(LimeWireUtils.getVendor()));
        get.addHeader(HTTPHeaderName.ACCEPT.create(Constants.QUERYREPLY_MIME_TYPE));
        get.addHeader(HTTPHeaderName.CONNECTION.create(HTTP.CONN_KEEP_ALIVE));
        if (SearchSettings.DESIRES_NMS1_URNS.getValue()) {
            get.addHeader(HTTPHeaderName.NMS1.create("1"));
        }
                
        if (!networkManager.acceptedIncomingConnection() && networkManager.canDoFWT()) {
            get.addHeader(HTTPHeaderName.FW_NODE_INFO.create(pushEndpointFactory.createForSelf()));
        }
        
        return client.execute(get);
    }

    String getPath(FriendPresence friendPresence) {
        if(friendPresence.getFriend().isAnonymous()) {
            return "/";
        } else {
            try {
                return "/friend/browse/" +  URLEncoder.encode(friendPresence.getFriend().getNetwork().getCanonicalizedLocalID(), "UTF-8") + "/";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateResponse(HttpResponse response) throws IOException {
        if(response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
            throw new IOException("HTTP status code = " + response.getStatusLine().getStatusCode()); // TODO create Exception class containing http status code
        }
        Header contentType = response.getFirstHeader("Content-Type");
        if(contentType != null && StringUtils.indexOfIgnoreCase(contentType.getValue(), Constants.QUERYREPLY_MIME_TYPE, Locale.ENGLISH) < 0) { // TODO concat all values
            throw new IOException("Unsupported Content-Type: " + contentType.getValue());
        }
        Header contentEncoding = response.getFirstHeader("Content-Encoding");
        if(contentEncoding != null) { // TODO - define acceptable encoding?
            throw new IOException("Unsupported Content-Encoding: " + contentEncoding.getValue());
        }
        Header contentLength = response.getFirstHeader("Content-Length");
        if(contentLength != null) {
            try {
                _replyLength = Long.parseLong(contentLength.getValue());
            } catch (NumberFormatException nfe) {
            }
        }
    }

    private void readQueryRepliesFromStream(HttpResponse response, FriendPresence friendPresence) {
        AddressFeature addressFeature = (AddressFeature)friendPresence.getFeature(AddressFeature.ID);
        if(response.getEntity() != null && addressFeature != null) { // address can be null if either party is concurrently logging out
            Address address = addressFeature.getFeature();
            InputStream in;
            try {
                in = response.getEntity().getContent();
            } catch (IOException e) {
                LOG.info("Unable to read a single message", e);
                return;
            }
            Message m = null;
            while(true) {
                try {
                    m = null;
                    LOG.debug("reading message");
                    m = messageFactory.read(in, Network.TCP);
                } catch (BadPacketException bpe) {
                    LOG.debug("BPE while reading", bpe);
                } catch (IOException expected){
                    LOG.debug("IOE while reading", expected);
                } // either timeout, or the remote closed.
                
                if(m == null)  {
                    LOG.debug("Unable to read create message");
                    return;
                } else {
                    if(m instanceof QueryReply) {
                        _currentLength += m.getTotalLength();
                        if(LOG.isTraceEnabled())
                            LOG.trace("BHH.browseExchange(): from " + friendPresence + " read QR:" + m);
                        QueryReply reply = (QueryReply)m;
                        reply.setGUID(_guid);
                        reply.setBrowseHostReply(true);
                        
                        forMeReplyHandler.get().handleQueryReply(reply, null, address);
                    }
                }
            }
        }
    }

	public static class PushRequestDetails {
        private FriendPresence friendPresence;
        private BrowseHostHandler bhh;
        private long timeStamp;
        
        public PushRequestDetails(BrowseHostHandler bhh, FriendPresence friendPresence) {
            this.friendPresence = friendPresence;
            timeStamp = System.currentTimeMillis();
            this.bhh = bhh;
        }

        public boolean isExpired() {
            return ((System.currentTimeMillis() - timeStamp) > EXPIRE_TIME);
        }
        
        public BrowseHostHandler getBrowseHostHandler() {
            return bhh;
        }
        
        public FriendPresence getFriendPresence() {
            return friendPresence;
        }
    }
    
    class SocketWrappingHttpClient extends DefaultHttpClient {
        private Credentials credentials;
        
        SocketWrappingHttpClient(Socket socket) {
            super(new SingleClientConnManager(getSchemeRegistry(socket)), httpParams.get());    
        }

        void setCredentials(Credentials credentials) {
            this.credentials = credentials;
        }
        
        @Override
        protected CredentialsProvider createCredentialsProvider() {
            return new CredentialsProvider() {
                public void setCredentials(AuthScope authscope, Credentials credentials) {
                    throw new UnsupportedOperationException();
                }
    
                public Credentials getCredentials(AuthScope authscope) {
                    return credentials;
                }
    
                public void clear() {
                    credentials = null;
                }
            };
        }
        
        /**
         * @return an <code>HttpRequestRetryHandler</code> that always returns
         * <code>false</code>
         */
        @Override
        protected HttpRequestRetryHandler createHttpRequestRetryHandler() {
            return new HttpRequestRetryHandler() {
                public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                    // when requests fail for unexpected reasons (eg., IOException), we don't 
                    // want to blindly re-attempt 
                    return false;
                }
            };
        }        
    }
    
    private static SchemeRegistry getSchemeRegistry(Socket socket) {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", new SocketWrapperProtocolSocketFactory(socket), 80));
        schemeRegistry.register(new Scheme("tls", new SocketWrapperProtocolSocketFactory(socket),80));
        schemeRegistry.register(new Scheme("https", new SocketWrapperProtocolSocketFactory(socket),80));
        return schemeRegistry;            
    }
    
    
}