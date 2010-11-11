package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedsizeHashMap;
import org.limewire.collection.NoMoreStorageException;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ManagedThread;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.messagehandlers.DualMessageHandler;
import com.limegroup.gnutella.messagehandlers.LimeACKHandler;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QuerySettings;
import com.limegroup.gnutella.search.ResultCounter;
import com.limegroup.gnutella.search.SearchResultHandler;

/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
//@Singleton // not here because it's not constructed, subclass is!
public abstract class MessageRouterImpl implements MessageRouter {
    
    private static final Log LOG = LogFactory.getLog(MessageRouterImpl.class);
        
    /**
     * Constant for the number of old connections to use when forwarding
     * traffic from old connections.
     */
    private static final int OLD_CONNECTIONS_TO_USE = 15;

    /**
     * The GUID we attach to QueryReplies to allow PushRequests in
     * responses.
     */
    protected byte[] _clientGUID;

    /**
     * The maximum size for <tt>RouteTable</tt>s.
     */
    private int MAX_ROUTE_TABLE_SIZE = 50000;  //actually 100,000 entries

    /**
     * Maps PingRequest GUIDs to PingReplyHandlers.  Stores 2-4 minutes,
     * typically around 2500 entries, but never more than 100,000 entries.
     */
    private RouteTable _pingRouteTable = 
        new RouteTable(2*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typically around 13000 entries, but never more than 100,000 entries.
     */
    private RouteTable _queryRouteTable = 
        new RouteTable(5*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typically around 3500 entries, but never more than 100,000
     * entries.  
     */
    private RouteTable _pushRouteTable = 
        new RouteTable(7*60, MAX_ROUTE_TABLE_SIZE);
    
    /**
     * Maps HeadPong guids to the originating pingers.  Short-lived since
     * we expect replies from our leaves quickly.
     */
    private RouteTable _headPongRouteTable = 
    	new RouteTable(10, MAX_ROUTE_TABLE_SIZE);

    /**
     * The amount of time after which to expire an OOBSession.
     */
    private static final long OOB_SESSION_EXPIRE_TIME = 2 * 60 * 1000;

    /** Time between sending HopsFlow messages.
     */
    private static final long HOPS_FLOW_INTERVAL = 15 * 1000; // 15 seconds

    private final BypassedResultsCache _bypassedResultsCache;
    
    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap<String, String> _udpConnectBacks = 
        new FixedsizeHashMap<String, String>(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a UDPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_UDP_CONNECTBACK_FORWARDS = 5;

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * TCP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap<String, String> _tcpConnectBacks = 
        new FixedsizeHashMap<String, String>(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a TCPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_TCP_CONNECTBACK_FORWARDS = 5;        
    
    /**
     * The processingqueue to add tcpconnectback socket connections to.
     */
    private static final ExecutorService TCP_CONNECT_BACKER =
        ExecutorsHelper.newProcessingQueue("TCPConnectBack");
    
	/**
	 * A handle to the thread that deals with QRP Propagation
	 */
	private final QRPPropagator QRP_PROPAGATOR = new QRPPropagator();


    /**
     * Variable for the most recent <tt>QueryRouteTable</tt> created
     * for this node.  If this node is an Ultrapeer, the routing
     * table will include the tables from its leaves.
     */
    private QueryRouteTable _lastQueryRouteTable;

    /**
     * The maximum number of response to send to a query that has
     * a "high" number of hops.
     */
    private static final int HIGH_HOPS_RESPONSE_LIMIT = 10;

    /** Keeps track of Listeners of GUIDs. */
    private volatile Map<byte[], List<MessageListener>> _messageListeners =
        Collections.emptyMap();
    
    /**
     * Lock that registering & unregistering listeners can hold
     * while replacing the listeners map / lists.
     */
    private final Object MESSAGE_LISTENER_LOCK = new Object();

    /**
     * The time we last received a request for a query key.
     */
    private long _lastQueryKeyTime;
    
    /** Handlers for TCP messages. */
    private ConcurrentMap<Class<? extends Message>, MessageHandler> messageHandlers =
        new ConcurrentHashMap<Class<? extends Message>, MessageHandler>(30, 0.75f, 3);
    
    /** Handler for UDP messages. */
    private ConcurrentMap<Class<? extends Message>, MessageHandler> udpMessageHandlers =
        new ConcurrentHashMap<Class<? extends Message>, MessageHandler>(15, 0.75f, 3);
    
    /** Handler for TCP messages. */
    private ConcurrentMap<Class<? extends Message>, MessageHandler> multicastMessageHandlers =
        new ConcurrentHashMap<Class<? extends Message>, MessageHandler>(5, 0.75f, 3);
        
    /** The length of time a multicast guid should stay alive. */
    private static final long MULTICAST_GUID_EXPIRE_TIME = 60 * 1000;
    
    /** How long to remember cached udp reply handlers. */
    private static final int UDP_REPLY_CACHE_TIME = 60 * 1000;
    
    protected final NetworkManager networkManager;
    protected final QueryRequestFactory queryRequestFactory;
    protected final QueryHandlerFactory queryHandlerFactory;
    protected final OnDemandUnicaster onDemandUnicaster;
    protected final HeadPongFactory headPongFactory;
    protected final PingReplyFactory pingReplyFactory;
    protected final ConnectionManager connectionManager;
    protected final ReplyHandler forMeReplyHandler;
    protected final QueryUnicaster queryUnicaster;
    protected final FileViewManager fileManager;
    protected final DHTManager dhtManager;
    protected final UploadManager uploadManager;
    protected final DownloadManager downloadManager;
    protected final UDPService udpService;
    protected final Provider<SearchResultHandler> searchResultHandler;
    protected final SocketsManager socketsManager;
    protected final HostCatcher hostCatcher;
    protected final QueryReplyFactory queryReplyFactory;
    protected final StaticMessages staticMessages;
    protected final Provider<MessageDispatcher> messageDispatcher;
    protected final MulticastService multicastService;
    protected final QueryDispatcher queryDispatcher;
    protected final Provider<ActivityCallback> activityCallback;
    protected final ConnectionServices connectionServices;
    protected final ScheduledExecutorService backgroundExecutor;
    protected final Provider<PongCacher> pongCacher;
    protected final UDPReplyHandlerCache udpReplyHandlerCache;
    protected final GuidMap multicastGuidMap;
    private final Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory;
    private final Provider<OOBHandler> oobHandlerFactory;
    private final Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager;
    protected final Provider<LimeACKHandler> limeAckHandler;
    protected final QRPUpdater qrpUpdater;
    private final URNFilter urnFilter;
    private final SpamServices spamServices;    
    private final PingRequestFactory pingRequestFactory;
    private final MessageHandlerBinder messageHandlerBinder;
    private final ConnectionListener connectionListener = new ConnectionListener();
    private final OutgoingQueryReplyFactory outgoingQueryReplyFactory;
    private final QuerySettings querySettings;
    
    /**
     * Creates a MessageRouter. Must call initialize before using.
     */
    @Inject
    // TODO: Create a MessageRouterController,
    //       or split MessageRouter into a number of different classes,
    //       instead of passing all of these...
    protected MessageRouterImpl(NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            QueryHandlerFactory queryHandlerFactory,
            OnDemandUnicaster onDemandUnicaster, 
            HeadPongFactory headPongFactory,
            PingReplyFactory pingReplyFactory,
            ConnectionManager connectionManager,
            @Named("forMeReplyHandler") ReplyHandler forMeReplyHandler,
            QueryUnicaster queryUnicaster,
            FileViewManager fileManager,
            DHTManager dhtManager,
            UploadManager uploadManager,
            DownloadManager downloadManager,
            UDPService udpService,
            Provider<SearchResultHandler> searchResultHandler,
            SocketsManager socketsManager,
            HostCatcher hostCatcher,
            QueryReplyFactory queryReplyFactory,
            StaticMessages staticMessages,
            Provider<MessageDispatcher> messageDispatcher,
            MulticastService multicastService,
            QueryDispatcher queryDispatcher,
            Provider<ActivityCallback> activityCallback,
            ConnectionServices connectionServices,
            ApplicationServices applicationServices,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<PongCacher> pongCacher,
            GuidMapManager guidMapManager,	
            UDPReplyHandlerCache udpReplyHandlerCache,
            Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
            PingRequestFactory pingRequestFactory,
            MessageHandlerBinder messageHandlerBinder,
            Provider<OOBHandler> oobHandlerFactory,
            Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager,
            Provider<LimeACKHandler> limeACKHandler,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            QRPUpdater qrpUpdater,
            URNFilter urnFilter,
            SpamServices spamServices,
            QuerySettings querySettings) {
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.queryHandlerFactory = queryHandlerFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.headPongFactory = headPongFactory;
        this.pingReplyFactory = pingReplyFactory;
        this.connectionManager = connectionManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.queryUnicaster = queryUnicaster;
        this.fileManager = fileManager;
        this.dhtManager = dhtManager;
        this.uploadManager = uploadManager;
        this.downloadManager = downloadManager;
        this.udpService = udpService;
        this.searchResultHandler = searchResultHandler;
        this.socketsManager = socketsManager;
        this.hostCatcher = hostCatcher;
        this.queryReplyFactory = queryReplyFactory;
        this.staticMessages = staticMessages;
        this.messageDispatcher = messageDispatcher;
        this.multicastService = multicastService;
        this.queryDispatcher = queryDispatcher;
        this.activityCallback = activityCallback;
        this.connectionServices = connectionServices;
        this.backgroundExecutor = backgroundExecutor;
        this.pongCacher = pongCacher;
        this.udpCrawlerPingHandlerFactory = udpCrawlerPingHandlerFactory;
        this.pingRequestFactory = pingRequestFactory;
        this.messageHandlerBinder = messageHandlerBinder;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
        this.multicastGuidMap = guidMapManager.getMap();
        this.udpReplyHandlerCache = udpReplyHandlerCache;
        this.oobHandlerFactory = oobHandlerFactory;
        this.MACCalculatorRepositoryManager = MACCalculatorRepositoryManager;
        this.limeAckHandler = limeACKHandler;
        this.qrpUpdater = qrpUpdater;
        this.urnFilter = urnFilter;
        this.spamServices = spamServices;
        this.querySettings = querySettings;

        _clientGUID = applicationServices.getMyGUID();
        _bypassedResultsCache = new BypassedResultsCache(activityCallback, downloadManager);
    }
    
    /** Sets a new handler to the given handlerMap, for the given class. */
    private boolean setHandler(ConcurrentMap<Class<? extends Message>, MessageHandler> handlerMap,
                            Class<? extends Message> clazz, MessageHandler handler) {
        
        if (handler != null) {
            MessageHandler old = handlerMap.put(clazz, handler);
            if(old != null) {
                LOG.warn("Ejecting old handler: " + old + " for clazz: " + clazz);
            }
            return true;
        } else {
            return handlerMap.remove(clazz) != null;
        }
    }
    
    /** 
     * Adds the given handler to the handlerMap for the given class.
     * If a handler already existed, this will construct a DualMessageHandler
     * so that both are handlers are notified.
     * 
     * @param handlerMap
     * @param clazz
     * @param handler
     */
    private void addHandler(ConcurrentMap<Class<? extends Message>, MessageHandler> handlerMap,
                            Class<? extends Message> clazz, MessageHandler handler) {
        MessageHandler existing = handlerMap.get(clazz);
        if(existing != null) {
            // non-blocking addition -- continue trying until we succesfully
            // replace the prior handler w/ a dual version of that handler
            while(true) {
                MessageHandler dual = new DualMessageHandler(handler, existing);
                if(handlerMap.replace(clazz, existing, dual))
                    break;
                existing = handlerMap.get(clazz);
                dual = new DualMessageHandler(handler, existing);
            }
        } else {
            setHandler(handlerMap, clazz, handler);
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#setMessageHandler(java.lang.Class, com.limegroup.gnutella.messagehandlers.MessageHandler)
     */
    public void setMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        setHandler(messageHandlers, clazz, handler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#addMessageHandler(java.lang.Class, com.limegroup.gnutella.messagehandlers.MessageHandler)
     */
    public void addMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        addHandler(messageHandlers, clazz, handler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getMessageHandler(java.lang.Class)
     */
    public MessageHandler getMessageHandler(Class<? extends Message> clazz) {
        return messageHandlers.get(clazz);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#setUDPMessageHandler(java.lang.Class, com.limegroup.gnutella.messagehandlers.MessageHandler)
     */
    public void setUDPMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        setHandler(udpMessageHandlers, clazz, handler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#addUDPMessageHandler(java.lang.Class, com.limegroup.gnutella.messagehandlers.MessageHandler)
     */
    public void addUDPMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        addHandler(udpMessageHandlers, clazz, handler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getUDPMessageHandler(java.lang.Class)
     */
    public MessageHandler getUDPMessageHandler(Class<? extends Message> clazz) {
        return udpMessageHandlers.get(clazz);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#setMulticastMessageHandler(java.lang.Class, com.limegroup.gnutella.messagehandlers.MessageHandler)
     */
    public void setMulticastMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        setHandler(multicastMessageHandlers, clazz, handler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#addMulticastMessageHandler(java.lang.Class, com.limegroup.gnutella.messagehandlers.MessageHandler)
     */
    public void addMulticastMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        addHandler(multicastMessageHandlers, clazz, handler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getMulticastMessageHandler(java.lang.Class)
     */
    public MessageHandler getMulticastMessageHandler(Class<? extends Message> clazz) {
        return multicastMessageHandlers.get(clazz);
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this).in(ServiceStage.EARLY);
    }
    
    public void initialize() {
    }
    
    public String getServiceName() {
        return I18nMarker.marktr("Message Routing");
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#initialize()
     */
    public void start() {
        
		// TODO listener leaking, we should have a shutdown event
		connectionManager.addEventListener(connectionListener);
		
	    QRP_PROPAGATOR.start();

        // schedule a runner to clear guys we've connected back to
        backgroundExecutor.scheduleWithFixedDelay(new ConnectBackExpirer(), 10 * CLEAR_TIME, 
                               10 * CLEAR_TIME, TimeUnit.MILLISECONDS);
        // schedule a runner to send hops-flow messages
        backgroundExecutor.scheduleWithFixedDelay(new HopsFlowManager(uploadManager, connectionManager), HOPS_FLOW_INTERVAL*10, 
                               HOPS_FLOW_INTERVAL, TimeUnit.MILLISECONDS);
        backgroundExecutor.scheduleWithFixedDelay(new UDPReplyCleaner(), UDP_REPLY_CACHE_TIME, UDP_REPLY_CACHE_TIME, TimeUnit.MILLISECONDS);
        
        // runner to clean up OOB sessions
        OOBHandler oobHandler = oobHandlerFactory.get();
        backgroundExecutor.scheduleWithFixedDelay(oobHandler, CLEAR_TIME, CLEAR_TIME, TimeUnit.MILLISECONDS);
        
        messageHandlerBinder.bind(this);
        
        setMessageHandler(PingRequest.class, new PingRequestHandler());
        setMessageHandler(PingReply.class, new PingReplyHandler());
        setMessageHandler(QueryRequest.class, new QueryRequestHandler());
        setMessageHandler(QueryReply.class, new QueryReplyHandler());
        setMessageHandler(ResetTableMessage.class, new ResetTableHandler());
        setMessageHandler(PatchTableMessage.class, new PatchTableHandler());
        setMessageHandler(TCPConnectBackVendorMessage.class, new TCPConnectBackHandler());
        setMessageHandler(UDPConnectBackVendorMessage.class, new UDPConnectBackHandler());
        setMessageHandler(TCPConnectBackRedirect.class, new TCPConnectBackRedirectHandler());
        setMessageHandler(UDPConnectBackRedirect.class, new UDPConnectBackRedirectHandler());
        setMessageHandler(PushProxyRequest.class, new PushProxyRequestHandler());
        setMessageHandler(QueryStatusResponse.class, new QueryStatusResponseHandler());
        setMessageHandler(HeadPing.class, new HeadPingHandler());
        setMessageHandler(HeadPong.class, new HeadPongHandler());
        setMessageHandler(DHTContactsMessage.class, new DHTContactsMessageHandler());
        VendorMessageHandler vendorMessageHandler = new VendorMessageHandler();
        addMessageHandler(VendorMessage.class, vendorMessageHandler);
        addMessageHandler(CapabilitiesVM.class, vendorMessageHandler);
        
        setUDPMessageHandler(QueryRequest.class, new UDPQueryRequestHandler());
        setUDPMessageHandler(QueryReply.class, new UDPQueryReplyHandler(oobHandler));
        setUDPMessageHandler(PingRequest.class, new UDPPingRequestHandler());
        setUDPMessageHandler(PingReply.class, new UDPPingReplyHandler());
        LimeACKHandler ackHandler = limeAckHandler.get();
        setUDPMessageHandler(LimeACKVendorMessage.class, ackHandler);
        setUDPMessageHandler(ReplyNumberVendorMessage.class, oobHandler);
        setUDPMessageHandler(UDPCrawlerPing.class, udpCrawlerPingHandlerFactory.get());
        setUDPMessageHandler(HeadPing.class, new UDPHeadPingHandler());
        
        setMulticastMessageHandler(QueryRequest.class, new MulticastQueryRequestHandler());
        setMulticastMessageHandler(PingRequest.class, new MulticastPingRequestHandler());
    }
    
    public void stop() {
        connectionManager.removeEventListener(connectionListener);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#originateQueryGUID(byte[])
     */
    public void originateQueryGUID(byte[] guid) {
        _queryRouteTable.routeReply(guid, forMeReplyHandler);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#queryKilled(com.limegroup.gnutella.GUID)
     */
    public void queryKilled(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentException("Input GUID is null!");
        _bypassedResultsCache.queryKilled(guid);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#downloadFinished(com.limegroup.gnutella.GUID)
     */
    public void downloadFinished(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentException("Input GUID is null!");
        _bypassedResultsCache.downloadFinished(guid);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getQueryLocs(com.limegroup.gnutella.GUID)
     */
    public Set<GUESSEndpoint> getQueryLocs(GUID guid) {
        return _bypassedResultsCache.getQueryLocs(guid);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getPingRouteTableDump()
     */
    public String getPingRouteTableDump() {
        return _pingRouteTable.toString();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getQueryRouteTableDump()
     */
    public String getQueryRouteTableDump() {
        return _queryRouteTable.toString();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getPushRouteTableDump()
     */
    public String getPushRouteTableDump() {
        return _pushRouteTable.toString();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#removeConnection(com.limegroup.gnutella.ReplyHandler)
     */
    private void removeConnection(ReplyHandler rh) {
        queryDispatcher.removeReplyHandler(rh);
        _pingRouteTable.removeReplyHandler(rh);
        _queryRouteTable.removeReplyHandler(rh);
        _pushRouteTable.removeReplyHandler(rh);
        _headPongRouteTable.removeReplyHandler(rh);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#handleMessage(com.limegroup.gnutella.messages.Message, com.limegroup.gnutella.RoutedConnection)
     */
    public void handleMessage(Message msg, 
                              ReplyHandler receivingConnection) {
        // Increment hops and decrease TTL.
        msg.hop();
        MessageHandler msgHandler = getMessageHandler(msg.getHandlerClass());
        if (msgHandler != null) {
            msgHandler.handleMessage(msg, null, receivingConnection);
        } else if (msg instanceof VendorMessage) {
            msgHandler = getMessageHandler(VendorMessage.class);
            if (msgHandler != null) {
                msgHandler.handleMessage(msg, null, receivingConnection);
            }
        }
        
        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
		//forwardQueryRouteTables();
        notifyMessageListener(msg, receivingConnection);
    }

    /**
     * Notifies any message listeners of this message's guid about the message.
     * This holds no locks.
     */
    private final void notifyMessageListener(Message msg, ReplyHandler handler) {
        List<MessageListener> all = _messageListeners.get(msg.getGUID());
        if(all != null) {
            for(MessageListener next : all) {
                next.processMessage(msg, handler);
            }
        }
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#handleUDPMessage(com.limegroup.gnutella.messages.Message, java.net.InetSocketAddress)
     */	
	public void handleUDPMessage(Message msg, InetSocketAddress addr) {
	    if(LOG.isTraceEnabled()) {
	        LOG.trace("Handling UDP message " + msg + " from " + addr);
	    }
	    
	    // Increment hops and decrement TTL.
	    msg.hop();

        if(msg instanceof QueryReply) {
            // check to see if it was from the multicast map.
            byte[] origGUID = multicastGuidMap.getOriginalGUID(msg.getGUID());
            if(origGUID != null) {
                msg = queryReplyFactory.createQueryReply(origGUID, (QueryReply)msg);
                ((QueryReply)msg).setMulticastAllowed(true);
            }
        }
        
        ReplyHandler replyHandler = udpReplyHandlerCache.getUDPReplyHandler(addr);
        MessageHandler msgHandler = getUDPMessageHandler(msg.getHandlerClass());
        if (msgHandler != null) {
            msgHandler.handleMessage(msg, addr, replyHandler);
        }  else if (msg instanceof VendorMessage) {
            msgHandler = getUDPMessageHandler(VendorMessage.class);
            if (msgHandler != null) {
                msgHandler.handleMessage(msg, addr, replyHandler);
            }
        }
        
        notifyMessageListener(msg, replyHandler);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#handleMulticastMessage(com.limegroup.gnutella.messages.Message, java.net.InetSocketAddress)
     */
	public void handleMulticastMessage(Message msg, InetSocketAddress addr) {
    
        // Use this assert for testing only -- it is a dangerous assert
        // to have in the field, as not all messages currently set the
        // network int appropriately.
        // If someone sends us messages we're not prepared to handle,
        // this could cause widespreaad AssertFailures
        // Assert.that(msg.isMulticast(),
        // "non multicast message in handleMulticastMessage: " + msg);

        // no multicast messages should ever have been
        // set with a TTL greater than 1.
        if (msg.getTTL() > 1) {
            return;
        }

        // Increment hops and decrement TTL.
        msg.hop();
        
        if (NetworkUtils.isLocalAddress(addr.getAddress())
                && !ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.getValue()) {
            return;
        }

        ReplyHandler replyHandler = udpReplyHandlerCache.getUDPReplyHandler(addr);
        
        MessageHandler msgHandler = getMulticastMessageHandler(msg.getHandlerClass());
        if (msgHandler != null) {
            msgHandler.handleMessage(msg, addr, replyHandler);
        } else if (msg instanceof VendorMessage) {
            msgHandler = getMulticastMessageHandler(VendorMessage.class);
            if (msgHandler != null) {
                msgHandler.handleMessage(msg, addr, replyHandler);
            }
        }

        notifyMessageListener(msg, replyHandler);
    }


    /**
     * Returns true if the Query has a valid AddressSecurityToken. false if it isn't present
     * or valid.
     */
    protected boolean hasValidQueryKey(InetAddress ip, int port, 
                                       QueryRequest qr) {
        AddressSecurityToken qk = qr.getQueryKey();
        if (qk == null)
            return false;
        
        return qk.isFor(ip, port);
    }

	/**
	 * Sends an ack back to the GUESS client node.  
	 */
	protected void sendAcknowledgement(InetSocketAddress addr, byte[] guid) {
		Endpoint host = connectionManager.getConnectedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
                
                reply = pingReplyFactory.createGUESSReply(guid, (byte)1, host);
            } catch(UnknownHostException e) {
				reply = createPingReply(guid);
            }
		} else {
			reply = createPingReply(guid);
		}
		
		// No GUESS endpoints existed and our IP/port was invalid.
		if( reply == null )
		    return;

        udpService.send(reply, addr.getAddress(), addr.getPort());
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or a <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply createPingReply(byte[] guid) {
		GUESSEndpoint endpoint = queryUnicaster.getUnicastEndpoint();
		if(endpoint == null) {
		    if(networkManager.isIpPortValid())
                return pingReplyFactory.create(guid, (byte)1);
            else
                return null;
		} else {
            return pingReplyFactory.createGUESSReply(guid, (byte)1, 
                                              endpoint.getPort(),
                                              endpoint.getInetAddress().getAddress());
		}
	}



	
    /**
     * Checks the routing table to see if the request has already been seen.
     * If not, calls handlePingRequest.
     */
    final void handlePingRequestPossibleDuplicate(
        PingRequest request, ReplyHandler handler) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handlePingRequest(request, handler);
    }

    /**
     * Checks the routing table to see if the request has already been seen.
     * If not, calls handlePingRequest.
     */
    final void handleUDPPingRequestPossibleDuplicate(													 
        PingRequest request, ReplyHandler handler, InetSocketAddress  addr) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handleUDPPingRequest(request, handler, addr);
    }

    /**
     * Checks the routing table to see if the request has already been seen.
     * If not, calls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplicate(
        QueryRequest request, ReplyHandler receivingConnection) {
        
        // With the new handling of probe queries (TTL 1, Hops 0), we have a few
        // new options:
        // 1) If we have a probe query....
        //  a) If you have never seen it before, put it in the route table and
        //  set the ttl appropriately
        //  b) If you have seen it before, then just count it as a duplicate
        // 2) If it isn't a probe query....
        //  a) Is it an extension of a probe?  If so re-adjust the TTL.
        //  b) Is it a 'normal' query (no probe extension or already extended)?
        //  Then check if it is a duplicate:
        //    1) If it a duplicate, just count it as one
        //    2) If it isn't, put it in the route table but no need to setTTL

        // we msg.hop() before we get here....
        // hops may be 1 or 2 because we may be probing with a leaf query....
        final boolean isProbeQuery = 
            ((request.getTTL() == 0) && 
             ((request.getHops() == 1) || (request.getHops() == 2)));

		ResultCounter counter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 receivingConnection);

		if(counter != null) {  // query is new (probe or normal)
            // 1a: set the TTL of the query so it can be potentially extended  
            if (isProbeQuery) 
                _queryRouteTable.setTTL(counter, (byte)1);

            // 1a and 2b2
            // if a new probe or a new request, do everything (so input true
            // below)
            handleQueryRequest(request, receivingConnection, counter, true);
		} else if (!isProbeQuery) {// probe extension?
            if (wasProbeQuery(request)) {
                // rebroadcast out but don't locally evaluate....
                handleQueryRequest(request, receivingConnection, counter, 
                                   false);
            }
        }
    }
	
    private boolean wasProbeQuery(QueryRequest request) {
        // if the current TTL is large enough and the old TTL was 1, then this
        // was a probe query....
        // NOTE: that i'm setting the ttl to be the actual ttl of the query.  i
        // could set it to some max value, but since we only allow TTL 1 queries
        // to be extended, it isn't a big deal what i set it to.  in fact, i'm
        // setting the ttl to the correct value if we had full expanding rings
        // of queries.
        return ((request.getTTL() > 0) && 
                _queryRouteTable.getAndSetTTL(request.getGUID(), (byte)1, 
                                              (byte)(request.getTTL()+1)));
    }

	/**
	 * Special handler for UDP queries.  Checks the routing table to see if
	 * the request has already been seen, handling it if not.
	 *
	 * @param query the UDP <tt>QueryRequest</tt> 
	 * @param handler the <tt>ReplyHandler</tt> that will handle the reply
	 * @return false if it was a duplicate, true if it was not.
	 */
	final boolean handleUDPQueryRequestPossibleDuplicate(QueryRequest request,
													  ReplyHandler handler)  {
		ResultCounter counter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 handler);
		if(counter != null) {
            handleQueryRequest(request, handler, counter, true);
            return true;
		}
		return false;
	}

    /**
     * Handles pings from the network.  With the addition of pong caching, this
     * method will either respond with cached pongs, or it will ignore the ping
     * entirely if another ping has been received from this connection very
     * recently.  If the ping is TTL=1, we will always process it, as it may
     * be a hearbeat ping to make sure the connection is alive and well.
     *
     * @param ping the ping to handle
     * @param handler the <tt>ReplyHandler</tt> instance that sent the ping
     */
    final private void handlePingRequest(PingRequest ping,
                                         ReplyHandler handler) {
        // Send it along if it's a heartbeat ping or if we should allow new 
        // pings on this connection.
        if(ping.isHeartbeat() || handler.allowNewPings()) {
            respondToPingRequest(ping, handler);
        }
    }


    /**
     * Responds to a UDP ping or query key request.
     */
    protected void handleUDPPingRequest(PingRequest pingRequest,
										ReplyHandler handler, 
										InetSocketAddress addr) {
        if (pingRequest.isQueryKeyRequest())
            sendQueryKeyPong(pingRequest, addr);
        else
            respondToUDPPingRequest(pingRequest, addr, handler);
    }
    

    /**
     * Generates an AddressSecurityToken for the source (described by addr)
     * and sends it in an AddressSecurityToken pong
     */
    protected void sendQueryKeyPong(PingRequest pr, InetSocketAddress addr) {

        // check if we're getting bombarded
        long now = System.currentTimeMillis();
        if (now - _lastQueryKeyTime < SearchSettings.QUERY_KEY_DELAY.getValue())
            return;
        
        _lastQueryKeyTime = now;
        
        // after find more sources and OOB queries, everyone can dole out query
        // keys....

        // generate a AddressSecurityToken (quite quick - current impl. (DES) is super
        // fast!
        InetAddress address = addr.getAddress();
        int port = addr.getPort();
        AddressSecurityToken key = new AddressSecurityToken(address, port, MACCalculatorRepositoryManager.get());
        
        // respond with Pong with QK, as GUESS requires....
        if (networkManager.isIpPortValid()) {
            PingReply reply = 
                pingReplyFactory.createQueryKeyReply(pr.getGUID(), (byte)1, key);
            udpService.send(reply, addr.getAddress(), addr.getPort());
        }
    }


    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {
        if (reply.getQueryKey() != null) {
            // this is a PingReply in reply to my AddressSecurityToken Request - 
            //consume the Pong and return, don't process as usual....
            onDemandUnicaster.handleQueryKeyPong(reply);
            return;
        }

        // do not process the pong if different from the host
        // described in the reply 
        if(reply.getPort() == port && reply.getInetAddress().equals(address))
            handlePingReply(reply, handler);
    }

    
    /**
     * The default handler for QueryRequests. This implementation updates
     * stats, does the broadcast, and generates a response.
     *
     * You can customize behavior in three ways:
     *   1. Override. You can assume that duplicate messages
     *      (messages with the same GUID that arrived via different paths) have
     *      already been filtered.  If you want stats updated, you'll
     *      have to call super.handleQueryRequest.
     *   2. Override broadcastQueryRequest.  This allows you to use the default
     *      handling framework and just customize request routing.
     *   3. Implement respondToQueryRequest.  This allows you to use the default
     *      handling framework and just customize responses.
     *
     * @param locallyEvaluate false if you don't want to send the query to
     * leaves and yourself, true otherwise....
     */
    public void handleQueryRequest(QueryRequest request,
									  ReplyHandler handler, 
									  ResultCounter counter,
                                      boolean locallyEvaluate) {
        // Apply the personal filter to decide whether the callback
        // should be informed of the query
        if(!spamServices.isPersonalSpam(request)) {
            activityCallback.get().handleQuery(request,
                    handler.getAddress(), handler.getPort());
        }
        
		// if it's a request from a leaf and we GUESS, send it out via GUESS --
		// otherwise, broadcast it if it still has TTL
		//if(handler.isSupernodeClientConnection() && 
		// RouterService.isGUESSCapable()) 
		//unicastQueryRequest(request, handler);
        //else if(request.getTTL() > 0) {
        updateMessage(request, handler);

		if(handler.isSupernodeClientConnection() && counter != null) {
            LOG.trace("Query request from leaf");
            if(request.desiresOutOfBandReplies()) {
                LOG.trace("Leaf wants OOB replies");
                // this query came from a leaf - so check if it desires OOB
                // responses and make sure that the IP it advertises is legit -
                // if it isn't drop away....
                // no need to check the port - if you are attacking yourself you
                // got problems
                String remoteAddr = handler.getInetAddress().getHostAddress();
                String myAddress = 
                    NetworkUtils.ip2string(networkManager.getAddress());
                if(request.getReplyAddress().equals(remoteAddr)) {
                    LOG.trace("OOB address is leaf's address");
                    ; // continue below, everything looks good
                } else if(request.getReplyAddress().equals(myAddress) && 
                         networkManager.isOOBCapable()) {
                    LOG.trace("OOB address is my address");
                    // i am proxying - maybe i should check my success rate but
                    // whatever...
                    ; 
                } else {
                    LOG.trace("OOB address is wrong, dropping query");
                    return;
                }
            }

            // don't send it to leaves here -- the dynamic querier will 
            // handle that
            locallyEvaluate = false;
            
            // do respond with files that we may have, though
            respondToQueryRequest(request, _clientGUID, handler);
            
            multicastQueryRequest(request);
            
			if(handler.isGoodLeaf()) {
				sendDynamicQuery(queryHandlerFactory.createHandlerForNewLeaf(
                                                                request, 
																handler,
                                                                counter));
			} else {
				sendDynamicQuery(queryHandlerFactory.createHandlerForOldLeaf(
                                                                request,
																handler,
                                                                counter));
			}
		} else if(request.getTTL() > 0 && connectionServices.isSupernode()) {
            // send the request to intra-Ultrapeer connections -- this does
			// not send the request to leaves
            if(handler.isGoodUltrapeer()) {
                // send it to everyone
                forwardQueryToUltrapeers(request, handler);
            } else {
                // otherwise, only send it to some connections
                forwardLimitedQueryToUltrapeers(request, handler);
            }
		}
			
        if (locallyEvaluate) {
            // always forward any queries to leaves -- this only does
            // anything when this node's an Ultrapeer
            forwardQueryRequestToLeaves(request, handler);
            
            // if (I'm firewalled AND the source is firewalled) AND 
            // NOT(he can do a FW transfer and so can i) then don't reply...
            if ((request.isFirewalledSource() &&
                 !networkManager.acceptedIncomingConnection()) &&
                !(request.canDoFirewalledTransfer() &&
                  networkManager.canDoFWT())
                )
                return;
            respondToQueryRequest(request, _clientGUID, handler);
        }
    }

    /**
     * Adds the sender of the reply message to an internal cache under the guid of the message
     * if the sender can receive unsolicited UDP.
     * 
     * @see {@link #getQueryLocs(GUID)} for retrieval
     */
    public boolean addBypassedSource(ReplyNumberVendorMessage reply, ReplyHandler handler) {
        
        //if the reply cannot receive unsolicited udp, there is no point storing it
        if (!reply.canReceiveUnsolicited()) {
            return false;
        }

        GUESSEndpoint ep = new GUESSEndpoint(handler.getInetAddress(), handler.getPort());
        return _bypassedResultsCache.addBypassedSource(new GUID(reply.getGUID()), ep);
    }

    /**
     * Adds the sender of the reply message to an internal cache under the guid of the message
     * if the sender can receive unsolicited UDP.
     * 
     * @see {@link #getQueryLocs(GUID)} for retrieval
     */
    public boolean addBypassedSource(QueryReply reply, ReplyHandler handler) {
        if (reply.isFirewalled())
            return false;
        GUESSEndpoint ep = new GUESSEndpoint(handler.getInetAddress(), handler.getPort());
        return _bypassedResultsCache.addBypassedSource(new GUID(reply.getGUID()), ep);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getNumOOBToRequest(com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage)
     */
    public int getNumOOBToRequest(ReplyNumberVendorMessage reply) {
    	GUID qGUID = new GUID(reply.getGUID());
    	
        int numResults = searchResultHandler.get().getNumResultsForQuery(qGUID);
        
        LOG.debug(qGUID + " results to request " + numResults);
                
    	
        if (numResults < 0) // this may be a proxy query
    		numResults = queryDispatcher.getLeafResultsForQuery(qGUID);

        if (numResults < 0 || numResults > querySettings.getUltrapeerResults()) {
            return -1;
        }
        
    	return reply.getNumResults();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#isQueryAlive(com.limegroup.gnutella.GUID)
     */
    public boolean isQueryAlive(GUID guid) {
        return _queryRouteTable.getReplyHandler(guid.bytes()) != null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#isHostUnicastQueried(com.limegroup.gnutella.GUID, org.limewire.io.IpPort)
     */
    public boolean isHostUnicastQueried(GUID guid, IpPort host) {
        return onDemandUnicaster.isHostQueriedForGUID(guid, host);
    }
    
    /**
     * Forwards the UDPConnectBack to neighboring peers
     * as a UDPConnectBackRedirect request.
     */
    protected void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connection source) {

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new UDPConnectBackRedirect(guidToUse, sourceAddr, 
                                                 portToContact);

        int sentTo = 0;
        List<RoutedConnection> peers =
            new ArrayList<RoutedConnection>(connectionManager.getInitializedConnections());
        Collections.shuffle(peers);
        for(RoutedConnection currMC : peers) {
            if(sentTo >= MAX_UDP_CONNECTBACK_FORWARDS)
                break;
            
            if(currMC == source)
                continue;

            if (currMC.getConnectionCapabilities().remoteHostSupportsUDPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }
    }


    /**
     * Sends a ping to the person requesting the connectback request.
     */
    protected void handleUDPConnectBackRedirect(UDPConnectBackRedirect udp,
                                               Connection source) {
        // only allow other UPs to send you this message....
        if (!source.getConnectionCapabilities().isSupernodeSupernodeConnection())
            return;

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress addrToContact = udp.getConnectBackAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact.getAddress(),
                                         portToContact);
        if (connectionManager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        String addrString = addrToContact.getHostAddress();
        if (!shouldServiceRedirect(_udpConnectBacks,addrString))
            return;

        // mutating twice restores the original guid
        UDPService.mutateGUID(guidToUse.bytes(), addrToContact, portToContact);
        PingRequest pr =
            pingRequestFactory.createPingRequest(guidToUse.bytes(),
                                                 (byte)1, (byte)0);
        udpService.send(pr, addrToContact, portToContact);
    }
    
    /**
     * @param map the map that keeps track of recent redirects
     * @param key the key which we would (have) store(d) in the map
     * @return whether we should service the redirect request
     * @modifies the map
     */
    private boolean shouldServiceRedirect(FixedsizeHashMap<String, String> map, String key) {
        synchronized(map) {
            String placeHolder = map.get(key);
            if (placeHolder == null) {
                try {
                    map.put(key, key);
                    return true;
                } catch (NoMoreStorageException nomo) {
                    return false;  // we've done too many connect backs, stop....
                }
            } else 
                return false;  // we've connected back to this guy recently....
        }
    }



    /**
     * Forwards the request to neighboring Ultrapeers as a
     * TCPConnectBackRedirect message.
     */
    protected void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connection source) {
        final int portToContact = tcp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new TCPConnectBackRedirect(sourceAddr, portToContact);

        int sentTo = 0;
        List<RoutedConnection> peers =
            new ArrayList<RoutedConnection>(connectionManager.getInitializedConnections());
        Collections.shuffle(peers);
        for(RoutedConnection currMC : peers) {
            if(sentTo >= MAX_TCP_CONNECTBACK_FORWARDS)
                break;
            
            if(currMC == source)
                continue;

            if (currMC.getConnectionCapabilities().remoteHostSupportsTCPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }        
    }

    /**
     * Basically, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     */
    protected void handleTCPConnectBackRedirect(TCPConnectBackRedirect tcp,
                                                Connection source) {
        // only allow other UPs to send you this message....
        if (!source.getConnectionCapabilities().isSupernodeSupernodeConnection())
            return;

        final int portToContact = tcp.getConnectBackPort();
        final String addrToContact =tcp.getConnectBackAddress().getHostAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact, portToContact);
        if (connectionManager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        if (!shouldServiceRedirect(_tcpConnectBacks,addrToContact))
            return;

        TCP_CONNECT_BACKER.execute(new Runnable() {
            public void run() {
                Socket sock = null;
                try {
                    try {
                        sock = socketsManager.connect(new InetSocketAddress(addrToContact, portToContact), 6000, ConnectType.TLS);
                        sock.setSoTimeout(6000);
                        OutputStream os = sock.getOutputStream();
                        os.write(StringUtils.toAsciiBytes("CONNECT BACK\r\n\r\n"));
                        os.flush();
                    } catch (IOException noTls) {
                        IOUtils.close(sock);
                        sock = socketsManager.connect(new InetSocketAddress(addrToContact, portToContact), 12000, ConnectType.PLAIN);                        
                        sock.setSoTimeout(12000);
                        OutputStream os = sock.getOutputStream();
                        os.write(StringUtils.toAsciiBytes("CONNECT BACK\r\n\r\n"));
                        os.flush();
                    }
                    if(LOG.isTraceEnabled())
                        LOG.trace("Succesful connectback to: " + addrToContact);
                    try {
                        Thread.sleep(500); // let the other side get it.
                    } catch(InterruptedException ignored) {
                        LOG.warn("Interrupted connectback", ignored);
                    }
                } catch (IOException ignored) {
                    LOG.warn("IOX during connectback", ignored);
                } finally {
                    IOUtils.close(sock);
                }
            }
        });
    }


    /**
     * 1) confirm that the connection is Ultrapeer to Leaf, then send your
     * listening port in a PushProxyAcknowledgement.
     * 2) Also cache the client's client GUID.
     */
    protected void handlePushProxyRequest(PushProxyRequest ppReq,
                                          RoutedConnection source) {
        if (source.isSupernodeClientConnection() 
                && networkManager.isIpPortValid()) {
            String stringAddr = 
                NetworkUtils.ip2string(networkManager.getAddress());
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(stringAddr);
            } catch(UnknownHostException uhe) {
                ErrorService.error(uhe); // impossible
            }

            if(_pushRouteTable.routeReply(ppReq.getClientGUID().bytes(), source) != null) {
                source.setPushProxyFor(true);

                PushProxyAcknowledgement ack =
                    new PushProxyAcknowledgement(addr,networkManager.getPort(),
                                                 ppReq.getClientGUID());
                source.send(ack);
            }
        }
    }

    /** This method should be invoked when this node receives a
     *  QueryStatusResponse message from the wire.  If this node is an
     *  Ultrapeer, we should update the Dynamic Querier about the status of
     *  the leaf's query.
     */    
    protected void handleQueryStatus(QueryStatusResponse resp,
                                     RoutedConnection leaf) {
        // message only makes sense if i'm a UP and the sender is a leaf
        if (!leaf.isSupernodeClientConnection())
            return;

        GUID queryGUID = resp.getQueryGUID();
        int numResults = resp.getNumResults();
        
        // get the QueryHandler and update the stats....
        queryDispatcher.updateLeafResultsForQuery(queryGUID, numResults);
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#sendPingRequest(com.limegroup.gnutella.messages.PingRequest, com.limegroup.gnutella.RoutedConnection)
     */
    public void sendPingRequest(PingRequest request,
                                RoutedConnection connection) {
        Objects.nonNull(request, "ping");
        Objects.nonNull(connection, "connection");
        _pingRouteTable.routeReply(request.getGUID(), forMeReplyHandler);
        connection.send(request);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#broadcastPingRequest(com.limegroup.gnutella.messages.PingRequest)
     */
    public void broadcastPingRequest(PingRequest ping) {
        Objects.nonNull(ping, "ping");
        _pingRouteTable.routeReply(ping.getGUID(), forMeReplyHandler);
        broadcastPingRequest(ping, forMeReplyHandler, connectionManager);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#sendDynamicQuery(com.limegroup.gnutella.messages.QueryRequest)
     */
	public void sendDynamicQuery(QueryRequest query) {
        Objects.nonNull(query, "query");
		// get the result counter so we can track the number of results
		ResultCounter counter = 
			_queryRouteTable.routeReply(query.getGUID(), forMeReplyHandler);
		if(connectionServices.isSupernode()) {
            QueryHandler qh =
                queryHandlerFactory.createHandlerForMe(query, counter);
			sendDynamicQuery(qh);
		} else {
            originateLeafQuery(query);
		} 
		
		// always send the query to your multicast people
        originateMulticastQuery(query);
    }
    
    /**
     * Originates a multicast query from this host.
     * This will alter the GUID of the query and store it in a mapping
     * of new -> old GUID.  When replies come in, if they have the new GUID,
     * they are reset to be the old one and the multicast flag is allowed.
     * 
     * @param query
     * @return the newGUID that the multicast query is using.
     */
    protected void originateMulticastQuery(QueryRequest query) {
        byte[] newGUID = GUID.makeGuid();
        QueryRequest mquery = queryRequestFactory.createMulticastQuery(newGUID, query);
        multicastGuidMap.addMapping(query.getGUID(), newGUID, MULTICAST_GUID_EXPIRE_TIME);
		multicastQueryRequest(mquery);
	}

	/**
	 * Initiates a dynamic query.  Only Ultrapeer should call this method,
	 * as this technique relies on fairly high numbers of connections to 
	 * dynamically adjust the TTL based on the number of results received, 
	 * the number of remaining connections, etc.
	 *
	 * @param qh the <tt>QueryHandler</tt> instance that generates
	 *  queries for this dynamic query
	 */
	private void sendDynamicQuery(QueryHandler qh) {
        Objects.nonNull(qh, "query handler");
        queryDispatcher.addQuery(qh);
	}

    /**
     * Broadcasts the ping request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated PingReplyHandler.  This is called from the default
     * handlePingRequest and the default broadcastPingRequest(PingRequest). If
     * there are more than 3 initialized connections, most will be skipped.
     * Unstable connections are skipped, and leaves only send their own pings
     * to their ultrapeers.
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    private void broadcastPingRequest(PingRequest request,
                                      ReplyHandler receivingConnection,
                                      ConnectionManager manager) {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.
        List<RoutedConnection> list = manager.getInitializedConnections();
        boolean randomlyForward = list.size() > 3;
        for(RoutedConnection mc : list) {
            if(mc == receivingConnection || !mc.isStable()) continue;
            if(receivingConnection == forMeReplyHandler || 
               !mc.getConnectionCapabilities().isClientSupernodeConnection()) {
                double percentToIgnore = mc.supportsPongCaching() ? 0.7 : 0.9;
                if(!randomlyForward || Math.random() >= percentToIgnore)
                    mc.send(request);
            }
        }
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#forwardQueryRequestToLeaves(com.limegroup.gnutella.messages.QueryRequest, com.limegroup.gnutella.ReplyHandler)
     */
	public final void forwardQueryRequestToLeaves(QueryRequest query,
                                                  ReplyHandler handler) {
		if(!connectionServices.isSupernode()) return;
        //use query routing to route queries to client connections
        //send queries only to the clients from whom query routing 
        //table has been received
        List<RoutedConnection> list = connectionManager.getInitializedClientConnections();
        List<RoutedConnection> hitConnections = new ArrayList<RoutedConnection>();
        for(RoutedConnection mc : list) {
            if(mc == handler) continue;
            if(mc.shouldForwardQuery(query)) {
                hitConnections.add(mc);
            }
        }
        //forward only to a quarter of the leaves in case the query is
        //very popular.
        if(list.size() > 8 && 
           (double)hitConnections.size()/(double)list.size() > .8) {
        	int startIndex = (int) Math.floor(
        			Math.random() * hitConnections.size() * 0.75);
            hitConnections = 
                hitConnections.subList(startIndex, startIndex+hitConnections.size()/4);
        }
        
        for(RoutedConnection mc : hitConnections) {
            // sendRoutedQueryToHost is not called because 
            // we have already ensured it hits the routing table
            // by filling up the 'hitsConnection' list.
            mc.send(query);
        }
	}

	/**
	 * Factored-out method that sends a query to a connection that supports
	 * query routing.  The query is only forwarded if there's a hit in the
	 * query routing entries.
	 *
	 * @param query the <tt>QueryRequest</tt> to potentially forward
	 * @param mc the <tt>RoutedConnection</tt> to forward the query to
	 * @param handler the <tt>ReplyHandler</tt> that will be entered into
	 *  the routing tables to handle any replies
	 * @return <tt>true</tt> if the query was sent, otherwise <tt>false</tt>
	 */
	private boolean sendRoutedQueryToHost(QueryRequest query, RoutedConnection mc,
										  ReplyHandler handler) {
		if (mc.shouldForwardQuery(query)) {
			//A new client with routing entry, or one that hasn't started
			//sending the patch.
			mc.send(query);
			return true;
		}
		return false;
	}

    /**
     * Adds the QueryRequest to the unicaster module.  Not much work done here,
     * see QueryUnicaster for more details.
     */
    protected void unicastQueryRequest(QueryRequest query,
                                       ReplyHandler conn) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
				
		queryUnicaster.addQuery(query, conn);
	}
	
    /**
     * Send the query to the multicast group.
     */
    protected void multicastQueryRequest(QueryRequest query) {
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
				
		multicastService.send(query);
	}	


    /**
     * Broadcasts the query request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated QueryReplyHandler.  This is called from teh default
     * handleQueryRequest and the default broadcastQueryRequest(QueryRequest)
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    // default access for testing
    void forwardQueryToUltrapeers(QueryRequest query,
                                          ReplyHandler handler) {
		// Note the use of initializedConnections only.
		// Note that we have zero allocations here.
		
		//Broadcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forward any queries not originating from me 
		//along leaf to ultrapeer connections.
	 
		List<RoutedConnection> list = connectionManager.getInitializedConnections();

        int forwarded = 0;
		for(RoutedConnection mc : list) {      
            forwarded += forwardQueryToUltrapeer(query, handler, mc) ? 1 : 0;  
        }
    }

    /**
     * Performs a limited broadcast of the specified query.  This is
     * useful, for example, when receiving queries from old-style 
     * connections that we don't want to forward to all connected
     * Ultrapeers because we don't want to overly magnify the query.
     *
     * @param query the <tt>QueryRequest</tt> instance to forward
     * @param handler the <tt>ReplyHandler</tt> from which we received
     *  the query
     */
    // default access for testing
    void forwardLimitedQueryToUltrapeers(QueryRequest query,
                                                 ReplyHandler handler) {
		//Broadcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forward any queries not originating from me 
		//along leaf to ultrapeer connections.
	 
		List<RoutedConnection> list = connectionManager.getInitializedConnections();
        int limit = list.size();

        int connectionsNeededForOld = OLD_CONNECTIONS_TO_USE;
		for(int i=0; i<limit; i++) {
            
            // if we've already queried enough old connections for
            // an old-style query, break out
            if(connectionsNeededForOld == 0) break;

			RoutedConnection mc = list.get(i);
            
            // if the query is comiing from an old connection, try to
            // send it's traffic to old connections.  Only send it to
            // new connections if we only have a minimum number left
            if(mc.isGoodUltrapeer() && 
               (limit-i) > connectionsNeededForOld) {
                continue;
            }
            forwardQueryToUltrapeer(query, handler, mc);
            
            // decrement the connections to use
            connectionsNeededForOld--;
		}    
    }

    /**
     * Forwards the specified query to the specified Ultrapeer.  This
     * encapsulates all necessary logic for forwarding queries to
     * Ultrapeers, for example handling last hop Ultrapeers specially
     * when the receiving Ultrapeer supports Ultrapeer query routing,
     * meaning that we check it's routing tables for a match before sending 
     * the query.
     *
     * @param query the <tt>QueryRequest</tt> to forward
     * @param handler the <tt>ReplyHandler</tt> that sent the query
     * @param ultrapeer the Ultrapeer to send the query to
     */
    // default access for testing
    boolean forwardQueryToUltrapeer(QueryRequest query, 
                                         ReplyHandler handler,
                                         RoutedConnection ultrapeer) {    
        // don't send a query back to the guy who sent it
        if(ultrapeer == handler) return false;

        // make double-sure we don't send a query received
        // by a leaf to other Ultrapeers
        if(ultrapeer.getConnectionCapabilities().isClientSupernodeConnection()) return false;

        // make sure that the ultrapeer understands feature queries.
        if(query.isFeatureQuery() && 
           !ultrapeer.getConnectionCapabilities().getRemoteHostSupportsFeatureQueries())
             return false;

        // is this the last hop for the query??
		boolean lastHop = query.getTTL() == 1; 
           
        // if it's the last hop to an Ultrapeer that sends
        // query route tables, route it.
        if(lastHop && ultrapeer.isUltrapeerQueryRoutingConnection()) {
            return sendRoutedQueryToHost(query, ultrapeer, handler);
        } else {
            // otherwise, just send it out
            ultrapeer.send(query);
        }
        return true;
    }


    /**
     * Originate a new query from this leaf node.
     *
     * @param qr the <tt>QueryRequest</tt> to send
     */
    // default access for testing
    void originateLeafQuery(QueryRequest qr) {
		List<RoutedConnection> list = connectionManager.getInitializedConnections();

        // only send to at most 5 Ultrapeers, as we could have more
        // as a result of race conditions - also, don't send what is new
        // requests down too many connections
        final int max = qr.isWhatIsNewRequest() ? 2 : 5; // PRO FEATURE
        int start = !qr.isWhatIsNewRequest() ? 0 : (int) (Math.floor(Math.random()*(list.size()-1)));
        int limit = Math.min(max, list.size());
        final boolean wantsOOB = qr.desiresOutOfBandReplies();
        if(wantsOOB)
            LOG.trace("Query asks for OOB replies");
        else
            LOG.trace("Query does not ask for OOB replies");
        for(int i=start; i<start+limit; i++) {
			RoutedConnection mc = list.get(i);
            QueryRequest qrToSend = qr;
            if(wantsOOB && mc.getConnectionCapabilities().remoteHostSupportsLeafGuidance() < 0) {
                LOG.trace("Unmarking OOB query");
                qrToSend = queryRequestFactory.unmarkOOBQuery(qr);
            }
            mc.originateQuery(qrToSend);
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#originateQuery(com.limegroup.gnutella.messages.QueryRequest, com.limegroup.gnutella.RoutedConnection)
     */
    public boolean sendInitialQuery(QueryRequest query, RoutedConnection mc) {
        Objects.nonNull(query, "query");
        Objects.nonNull(mc, "connection");
        // if this is a feature query & the other side doesn't
        // support it, then don't send it
        // This is an optimization of network traffic, and doesn't
        // necessarily need to exist.  We could be shooting ourselves
        // in the foot by not sending this, rendering Feature Searches
        // inoperable for some users connected to bad Ultrapeers.
        if (query.isFeatureQuery()
                && !mc.getConnectionCapabilities().getRemoteHostSupportsFeatureQueries()) {
            return false;
        }
        
        query.originate(); // ensure it gets put in the right priority.
        mc.send(query);
        return true;
    }
    
    /**
     * Respond to the ping request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendPingReply(PingReply).
     * This method is called from the default handlePingRequest.
     */
    protected abstract void respondToPingRequest(PingRequest request,
                                                 ReplyHandler handler);

	/**
	 * Responds to a ping received over UDP -- implementations
	 * handle this differently from pings received over TCP, as it is 
	 * assumed that the requester only wants pongs from other nodes
	 * that also support UDP messaging.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param addr the <tt>InetSocketAddress</tt> containing the ping
     * @param handler the <tt>ReplyHandler</tt> instance from which the
     *  ping was received and to which pongs should be sent
	 */
    protected abstract void respondToUDPPingRequest(PingRequest request, 
													InetSocketAddress addr,
                                                    ReplyHandler handler);


    /**
     * Respond to the query request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is called from the default handleQueryRequest.
     */
    protected abstract boolean respondToQueryRequest(QueryRequest queryRequest,
                                                     byte[] clientGUID,
                                                     ReplyHandler handler);

    /**
     * The default handler for PingRequests.  This implementation uses the
     * ping route table to route a ping reply.  If an appropriate route
     * doesn't exist, records the error statistics.  On sucessful routing,
     * the PingReply count is incremented.<p>
     *
     * In all cases, the ping reply is recorded into the host catcher.<p>
     *
     * Override as desired, but you probably want to call super.handlePingReply
     * if you do.
     */
    protected void handlePingReply(PingReply reply,
                                   ReplyHandler handler) {
        //update hostcatcher (even if the reply isn't for me)
        boolean newAddress = hostCatcher.add(reply);

        if(newAddress && !reply.isUDPHostCache()) {
            pongCacher.get().addPong(reply);
        }

        //First route to originator in usual manner.
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(reply.getGUID());

        if (replyHandler != null) {
            replyHandler.handlePingReply(reply, handler);
        } else {
            handler.countDroppedMessage();
        }
        boolean supportsUnicast = reply.supportsUnicast();
        
        //Then, if a marked pong from an Ultrapeer that we've never seen before,
        //send to all leaf connections except replyHandler (which may be null),
        //irregardless of GUID.  The leafs will add the address then drop the
        //pong as they have no routing entry.  Note that if Ultrapeers are very
        //prevalent, this may consume too much bandwidth.
		//Also forward any GUESS pongs to all leaves.
        if (newAddress && (reply.isUltrapeer() || supportsUnicast)) {
            List<RoutedConnection> list =
                connectionManager.getInitializedClientConnections();
            for(RoutedConnection c : list) {
                assert c != null : "null c.";
                if (c!=handler && c!=replyHandler && c.allowNewPongs()) {
                    c.handlePingReply(reply, handler);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#handleQueryReply(com.limegroup.gnutella.messages.QueryReply, com.limegroup.gnutella.ReplyHandler)
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler handler) {
        Objects.nonNull(queryReply, "query reply");
        Objects.nonNull(handler, "reply handler");
        
        // check the altloc count
        if (!altCountOk(queryReply))
            return;
        
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        int classC = ByteUtils.beb2int(queryReply.getIPBytes(), 0);
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                        queryReply.getTotalLength(),
                                        queryReply.getUniqueResultCount(),
                                        queryReply.getPartialResultCount(),
                                        classC, !urnFilter.isBlacklisted(queryReply));

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       handler);
            //Simple flow control: don't route this message along other
            //connections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.

            ReplyHandler rh = rrp.getReplyHandler();
            
            // remember more stats
            _queryRouteTable.countHopsTTLNet(queryReply);
            // if this reply is for us, remember even more stats
            if (rh == forMeReplyHandler)
                _queryRouteTable.timeStampResults(queryReply);
            
            if(!shouldDropReply(rrp, rh, queryReply)) {                
                rh.handleQueryReply(queryReply, handler);
                // also add to the QueryUnicaster for accounting - basically,
                // most results will not be relevant, but since it is a simple
                // HashSet lookup, it isn't a prohibitive expense...
                queryUnicaster.handleQueryReply(queryReply);

            } else {
                LOG.trace("Dropping reply");
                handler.countDroppedMessage();
            }
        } else {
            LOG.trace("Dropping reply with no route table entry");
            handler.countDroppedMessage();
        }
    }

    private boolean altCountOk(QueryReply qr) {
        try {
            for (Response r : qr.getResultsAsList()) {
                if (r.getLocations().size() > FilterSettings.MAX_ALTS_PER_RESPONSE.getValue())
                    return false;
            }
        } catch (BadPacketException bpe) {
            // may get routed to someone who can parse it
        }

        return true;
    }
    
    /**
     * Checks if the <tt>QueryReply</tt> should be dropped for various reasons.
     *
     * Reason 1) The reply has already routed enough traffic.  Based on per-TTL
     * hard limits for the number of bytes routed for the given reply guid.
     * This algorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits have more liberal limits than high TTL
     * hits.  This ensures that hits that are closer to the query originator
     * -- hits for which we've already done most of the work, are not 
     * dropped unless we've routed a really large number of bytes for that
     * guid.  This method also checks that hard number of results that have
     * been sent for this GUID.  If this number is greater than a specified
     * limit, we simply drop the reply.
     *
     * Reason 2) The reply was meant for me -- DO NOT DROP.
     *
     * Reason 3) The TTL is 0, drop.
     *
     * @param rrp the <tt>ReplyRoutePair</tt> containing data about what's 
     *  been routed for this GUID
     * @param ttl the time to live of the query hit
     * @return <tt>true if the reply should be dropped, otherwise <tt>false</tt>
     */
    private boolean shouldDropReply(RouteTable.ReplyRoutePair rrp,
                                    ReplyHandler rh,
                                    QueryReply qr) {
        byte ttl = qr.getTTL();
                                           
        // Reason 2 --  The reply is meant for me, do not drop it.
        if( rh == forMeReplyHandler ) return false;
        
        // Reason 3 -- drop if TTL is 0.
        if( ttl == 0 ) return true;

        // Reason 1 ...
        
        int resultsRouted = rrp.getResultsRouted();

        // drop the reply if we've already sent more than the specified number
        // of results for this GUID
        // FIXME: could this cause spam to kill queries?
        if(resultsRouted > 100) return true;

        int bytesRouted = rrp.getBytesRouted();
        // send replies with ttl above 2 if we've routed under 50K 
        if(ttl > 2 && bytesRouted < 50    * 1024) return false;
        // send replies with ttl 1 if we've routed under 200K 
        if(ttl == 1 && bytesRouted < 200 * 1024) return false;
        // send replies with ttl 2 if we've routed under 100K 
        if(ttl == 2 && bytesRouted < 100  * 1024) return false;

        // if none of the above conditions holds true, drop the reply
        // FIXME: could this cause spam to kill queries?
        return true;
    }

    /**
     * Returns the appropriate handler from the _pushRouteTable.
     * This enforces that requests for my clientGUID will return
     * FOR_ME_REPLY_HANDLER, even if it's not in the table. Will also
     * iterate through leaves if they are not in the table.
     */
    public ReplyHandler getPushHandler(byte[] guid) {
        ReplyHandler replyHandler = _pushRouteTable.getReplyHandler(guid);
        if(replyHandler != null) {
            return replyHandler;
        } else if(Arrays.equals(_clientGUID, guid)) {
            return forMeReplyHandler;
        } else {
            if (!connectionServices.isSupernode()) {
                return null;
            }
            for (RoutedConnection leaf : connectionManager.getInitializedClientConnections()) {
                if (leaf.isPushProxyFor() && Arrays.equals(leaf.getClientGUID(), guid)) {
                    return leaf;
                }
            }
            return null;
        }
    }

    /**
     * Uses the ping route table to send a PingReply to the appropriate
     * connection.  Since this is used for PingReplies orginating here, no
     * stats are updated.
     */
    protected void sendPingReply(PingReply pong, ReplyHandler handler) {
        Objects.nonNull(pong, "pong");
        Objects.nonNull(handler, "reply handler");
        handler.handlePingReply(pong, null);
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    protected void sendQueryReply(QueryReply queryReply) throws IOException {
        Objects.nonNull(queryReply, "query reply");
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount(),
											 queryReply.getPartialResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            rrp.getReplyHandler().handleQueryReply(queryReply, null);
        }
        else
            throw new IOException("no route for reply");
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#sendPushRequest(com.limegroup.gnutella.messages.PushRequest)
     */
    public void sendPushRequest(PushRequest push) throws IOException {
        Objects.nonNull(push, "push");
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(push.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(push, forMeReplyHandler);
        else
            throw new IOException("no route for push");
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#sendMulticastPushRequest(com.limegroup.gnutella.messages.PushRequest)
     */
    public void sendMulticastPushRequest(PushRequest push) {
        Objects.nonNull(push, "push");
        // must have a TTL of 1
        assert push.getTTL() == 1 : "multicast push ttl not 1";
        
        multicastService.send(push);
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#responsesToQueryReplies(com.limegroup.gnutella.Response[], com.limegroup.gnutella.messages.QueryRequest)
     */
    public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
                                            QueryRequest queryRequest) {
        return responsesToQueryReplies(responses, queryRequest, 10, null);
    }


    /**
     * Converts the passed responses to QueryReplies. Each QueryReply can
     * accomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in case the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt have any side effect, 
     * and does not modify the state of this object
     * @param responses The responses to be converted
     * @param queryRequest The query request corresponding to which we are
     * generating query replies.
     * @param REPLY_LIMIT the maximum number of responses to have in each reply.
     * @param security token might be null
     * @return Iterable of QueryReply
     */
    public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
                                             QueryRequest queryRequest,
                                             final int REPLY_LIMIT, SecurityToken securityToken) {
        int numResponses = responses.length;
        int numHops = queryRequest.getHops();

        // limit the responses if we're not delivering this 
        // out-of-band and we have a lot of responses
        if(REPLY_LIMIT > 1 && 
           numHops > 2 && 
           numResponses > HIGH_HOPS_RESPONSE_LIMIT) {
            int j = 
                (int)(Math.random() * numResponses) % 
                (numResponses - HIGH_HOPS_RESPONSE_LIMIT);

            Response[] newResponses = 
                new Response[HIGH_HOPS_RESPONSE_LIMIT];
            for(int i=0; i<10; i++, j++) {
                newResponses[i] = responses[j];
            }
            responses = newResponses;
            numResponses = responses.length;
        }
        
        // decrement the number of responses we have left
        return outgoingQueryReplyFactory.createReplies(responses, queryRequest, securityToken, REPLY_LIMIT);
    }

    /**
     * Handles a message to reset the query route table for the given
     * connection.
     *
     * @param rtm the <tt>ResetTableMessage</tt> for resetting the query
     *  route table
     * @param mc the <tt>RoutedConnection</tt> for which the query route
     *  table should be reset
     */
    private void handleResetTableMessage(ResetTableMessage rtm,
                                         RoutedConnection mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // reset the query route table for this connection
        synchronized (mc.getQRPLock()) {
            mc.resetQueryRouteTable(rtm);
        }

        // if this is coming from a leaf, make sure we update
        // our tables so that the dynamic querier has correct
        // data
        if(mc.isLeafConnection()) {
            _lastQueryRouteTable = createRouteTable();
        }
    }

    /**
     * Handles a message to patch the query route table for the given
     * connection.
     *
     * @param rtm the <tt>PatchTableMessage</tt> for patching the query
     *  route table
     * @param mc the <tt>RoutedConnection</tt> for which the query route
     *  table should be patched
     */
    private void handlePatchTableMessage(PatchTableMessage ptm,
                                         RoutedConnection mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // patch the query route table for this connection
        synchronized(mc.getQRPLock()) {
            mc.patchQueryRouteTable(ptm);
        }

        // if this is coming from a leaf, make sure we update
        // our tables so that the dynamic querier has correct
        // data
        if(mc.isLeafConnection()) {
            // Leaves should not send full tables
            if(mc.getRoutedConnectionStatistics().getQueryRouteTablePercentFull() == 100) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Leaf " + mc + " sent full query routing table");
                mc.close();
            }
            _lastQueryRouteTable = createRouteTable();
        }
    }

    private void updateMessage(QueryRequest request, ReplyHandler handler) {
        
        if (SearchSettings.SEND_LIME_RESPONSES.getBoolean() &&
                request.isQueryForLW() && staticMessages.getLimeReply() != null) {
            QueryReply qr = queryReplyFactory.createQueryReply(request.getGUID(), staticMessages.getLimeReply());
            qr.setHops((byte)0);
            qr.setTTL((byte)(request.getHops()+1));
            try {
                sendQueryReply(qr);
            } catch (IOException ignored) {}
        }
        
        if (!(handler instanceof Connection)) 
            return;
    }

    /**
     * Utility method for checking whether or not the given connection
     * is able to pass QRP messages.
     *
     * @param c the <tt>Connection</tt> to check
     * @return <tt>true</tt> if this is a QRP-enabled connection,
     *  otherwise <tt>false</tt>
     */
    private static boolean isQRPConnection(Connection c) {
        if (c.getConnectionCapabilities().isSupernodeClientConnection())
            return true;
        if (c.getConnectionCapabilities().isUltrapeerQueryRoutingConnection())
            return true;
        return false;
    }

    /** Thread the processing of QRP Table delivery. */
    private class QRPPropagator extends ManagedThread {
        public QRPPropagator() {
            setName("QRPPropagator");
            setDaemon(true);
        }

        /** While the connection is not closed, sends all data delay. */
        @Override
        public void run() {
            try {
                while (true) {
					// Check for any scheduled QRP table propagations
					// every 10 seconds
                    Thread.sleep(10*1000);
    				forwardQueryRouteTables();
                }
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }


    /**
     * Sends updated query routing tables to all connections which haven't
     * been updated in a while.  You can call this method as often as you want;
     * it takes care of throttling.
     *     @modifies connections
     */
    // default access for testing
    void forwardQueryRouteTables() {
		//Check the time to decide if it needs an update.
		long time = System.currentTimeMillis();

		//For all connections to new hosts c needing an update...
		List<RoutedConnection> list =
            connectionManager.getInitializedConnections();
		QueryRouteTable table = null;
		List<RouteTableMessage> patches = null;
		QueryRouteTable lastSent = null;
		
		for(RoutedConnection c : list) {

			// continue if I'm an Ultrapeer and the node on the
			// other end doesn't support Ultrapeer-level query
			// routing
			if(connectionServices.isSupernode()) { 
				// only skip it if it's not an Ultrapeer query routing
				// connection
				if(!c.isUltrapeerQueryRoutingConnection()) { 
					continue;
				}
			} 				
			// otherwise, I'm a leaf, and don't send routing
			// tables if it's not a connection to an Ultrapeer
			// or if query routing is not enabled on the connection
			else if (!(c.getConnectionCapabilities().isClientSupernodeConnection() && 
					   c.getConnectionCapabilities().isQueryRoutingEnabled())) {
				continue;
			}
			
			// See if it is time for this connections QRP update
			// This call is safe since only this thread updates time
			if (time<c.getRoutedConnectionStatistics().getNextQRPForwardTime())
				continue;

			c.getRoutedConnectionStatistics().incrementNextQRPForwardTime(time);
				
			// Create a new query route table if we need to
			if (table == null) {
				table = createRouteTable();     //  Ignores busy leaves
                _lastQueryRouteTable = table;
			} 

			//..and send each piece.
			
			// Because we tend to send the same list of patches to lots of
			// Connections, we can reuse the list of RouteTableMessages
			// between those connections if their last sent
			// table is exactly the same.
			// This allows us to only reduce the amount of times we have
			// to call encode.
			
			//  (This if works for 'null' sent tables too)
			if( lastSent == c.getRoutedConnectionStatistics().getQueryRouteTableSent() ) {
			    // if we have not constructed the patches yet, then do so.
			    if( patches == null )
			        patches = table.encode(lastSent, true);
			}
			// If they aren't the same, we have to encode a new set of
			// patches for this connection.
			else {
			    lastSent = c.getRoutedConnectionStatistics().getQueryRouteTableSent();
			    patches = table.encode(lastSent, true);
            }
            
            // If sending QRP tables is turned off, don't send them.  
            if(!ConnectionSettings.SEND_QRP.getValue()) {
                return;
            }
            
            for(RouteTableMessage next : patches)
		        c.send(next);
    	    
            c.getRoutedConnectionStatistics().setQueryRouteTableSent(table);
		}
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getQueryRouteTable()
     */
    public QueryRouteTable getQueryRouteTable() {
        return _lastQueryRouteTable;
    }

    /**
     * Creates a query route table appropriate for forwarding to connection c.
     * This will not include information from c.
     *     @requires queryUpdateLock held
     */
    //default access for testing
    QueryRouteTable createRouteTable() {
        QueryRouteTable ret = qrpUpdater.getQRT();
        
        // Add leaves' files if we're an Ultrapeer.
        if(connectionServices.isSupernode()) {
            addQueryRoutingEntriesForLeaves(ret);
        }
        return ret;
    }


	/**
	 * Adds all query routing tables of leaves to the query routing table for
	 * this node for propagation to other Ultrapeers at 1 hop.
	 * 
	 * Added "busy leaf" support to prevent a busy leaf from having its QRT
	 * 	table added to the Ultrapeer's last-hop QRT table.  This should reduce
	 *  BW costs for UPs with busy leaves.  
	 *
	 * @param qrt the <tt>QueryRouteTable</tt> to add to
	 */
	private void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {
		List<RoutedConnection> leaves =
            connectionManager.getInitializedClientConnections();
		
		for(RoutedConnection mc : leaves) {
        	synchronized (mc.getQRPLock()) {
        	    //	Don't include busy leaves
        	    if( !mc.isBusyLeaf() ){
                	QueryRouteTable qrtr = mc.getRoutedConnectionStatistics().getQueryRouteTableReceived();
					if(qrtr != null) {
						qrt.addAll(qrtr);
					}
        	    }
			}
		}
	}

    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#registerMessageListener(byte[], com.limegroup.gnutella.MessageListener)
     */
    public void registerMessageListener(byte[] guid, MessageListener ml) {
        ml.registered(guid);
        synchronized(MESSAGE_LISTENER_LOCK) {
            Map<byte[], List<MessageListener>> listeners =
                new TreeMap<byte[], List<MessageListener>>(GUID.GUID_BYTE_COMPARATOR);
            listeners.putAll(_messageListeners);
            List<MessageListener> all = listeners.get(guid);
            if(all == null) {
                all = new ArrayList<MessageListener>(1);
                all.add(ml);
            } else {
                List<MessageListener> temp = new ArrayList<MessageListener>(all.size() + 1);
                temp.addAll(all);
                all = temp;
                all.add(ml);
            }
            listeners.put(guid, Collections.unmodifiableList(all));
            _messageListeners = Collections.unmodifiableMap(listeners);
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#unregisterMessageListener(byte[], com.limegroup.gnutella.MessageListener)
     */
    public void unregisterMessageListener(byte[] guid, MessageListener ml) {
        boolean removed = false;
        synchronized(MESSAGE_LISTENER_LOCK) {
            List<MessageListener> all = _messageListeners.get(guid);
            if(all != null) {
                all = new ArrayList<MessageListener>(all);
                if(all.remove(ml)) {
                    removed = true;
                    Map<byte[], List<MessageListener>> listeners =
                        new TreeMap<byte[], List<MessageListener>>(GUID.GUID_BYTE_COMPARATOR);
                    listeners.putAll(_messageListeners);
                    if(all.isEmpty())
                        listeners.remove(guid);
                    else
                        listeners.put(guid, Collections.unmodifiableList(all));
                    _messageListeners = Collections.unmodifiableMap(listeners);
                }
            }
        }
        if(removed)
            ml.unregistered(guid);
    }


    /**
     * Replies to a head ping sent from the given ReplyHandler.
     */
    private void handleHeadPing(HeadPing ping, ReplyHandler handler) {
        if (DownloadSettings.DROP_HEADPINGS.getValue())
            return;
        
        GUID clientGUID = ping.getClientGuid();
        ReplyHandler pingee;
        
        if(clientGUID != null)
            pingee = getPushHandler(clientGUID.bytes());
        else
            pingee = forMeReplyHandler; // handle ourselves.
        
        //drop the ping if no entry for the given clientGUID
        if (pingee == null) 
           return; 
        
        //don't bother routing if this is intended for me. 
        // TODO:  Clean up ReplyHandler interface so we aren't
        //        afraid to use it like it's intended.
        //        That way, we can do pingee.handleHeadPing(ping)
        //        and not need this anti-OO instanceof check.
        if (pingee instanceof ForMeReplyHandler) {
            // If it's for me, reply directly to the person who sent it.
            HeadPong pong = headPongFactory.create(ping);
            handler.reply(pong); // 
        } else {
            // Otherwise, remember who sent it and forward it on.
            //remember where to send the pong to. 
            //the pong will have the same GUID as the ping. 
            // Note that this uses the messageGUID, not the clientGUID
            _headPongRouteTable.routeReply(ping.getGUID(), handler); 
            
            //and send off the routed ping 
            if ( !(handler instanceof Connection) ||
                    ((Connection)handler).getConnectionCapabilities().supportsVMRouting())
                pingee.reply(ping);
            else
                pingee.reply(new HeadPing(ping)); 
        }
   }
    
    /**
     * Pure testing method. 
     */
    Map<byte[], List<MessageListener>> getMessageListenerMap() {
        return _messageListeners;
    }
    
    
    /** 
     * Handles a pong received from the given handler.
     */ 
    private void handleHeadPong(HeadPong pong, ReplyHandler handler) { 
        ReplyHandler forwardTo =  _headPongRouteTable.getReplyHandler(pong.getGUID()); 

        // TODO: Clean up ReplyHandler interface so we're not afraid
        //       to use it correctly.
        //       Ideally, we'd do forwardTo.handleHeadPong(pong)
        //       instead of this instanceof check
         
        // if this pong is for me, process it as usual (not implemented yet)
        if (forwardTo != null && !(forwardTo instanceof ForMeReplyHandler)) { 
            forwardTo.reply(pong); 
            _headPongRouteTable.removeReplyHandler(forwardTo); 
        } 
    } 
    
    private void handleDHTContactsMessage(DHTContactsMessage msg, ReplyHandler handler) {
        dhtManager.handleDHTContactsMessage(msg);
    }
    
    /**
     * Should only be used for testing.
     */
    RouteTable getPushRouteTable() {
        return _pushRouteTable;
    }
    
    RouteTable getHeadPongRouteTable() {
        return _headPongRouteTable;
    }
    
    /** Expires the UDP-Reply cache. */
    private class UDPReplyCleaner implements Runnable {
        public void run() {
            messageDispatcher.get().dispatch(new Runnable() {
                public void run() {
                    udpReplyHandlerCache.clear();
                }
            });
        }
        
    }

    /** This is run to clear out the registry of connect back attempts...
     *  Made package access for easy test access.
     */
    static class ConnectBackExpirer implements Runnable {
        public void run() {
            _tcpConnectBacks.clear();
            _udpConnectBacks.clear();
        }
    }

    private static class HopsFlowManager implements Runnable {
        private final UploadManager uploadManager;
        private final ConnectionManager connectionManager;
        
        public HopsFlowManager(UploadManager uploadManager, ConnectionManager connectionManager) {
            this.uploadManager = uploadManager;
            this.connectionManager = connectionManager;
        }
        
        /* in case we don't want any queries any more */
        private static final byte BUSY_HOPS_FLOW = 0;

    	/* in case we want to reenable queries */
    	private static final byte FREE_HOPS_FLOW = 5;

        /* small optimization:
           send only HopsFlowVendorMessages if the busy state changed */
        private static boolean _oldBusyState = false;
           
        public void run() {
            // only leafs should use HopsFlow
            if (connectionManager.isSupernode())
                return;
            // busy hosts don't want to receive any queries, if this node is not
            // busy, we need to reset the HopsFlow value
            boolean isBusy = !uploadManager.mayBeServiceable();
            
            // state changed? don't bother the ultrapeer with information
            // that it already knows. we need to inform new ultrapeers, though.
            final List<RoutedConnection> connections =
                connectionManager.getInitializedConnections();
            final HopsFlowVendorMessage hops = 
                new HopsFlowVendorMessage(isBusy ? BUSY_HOPS_FLOW :
                                          FREE_HOPS_FLOW);
            if (isBusy == _oldBusyState) {
                for(RoutedConnection c : connections) {
                    // Yes, we may tell a new ultrapeer twice, but
                    // without a buffer of some kind, we might forget
                    // some ultrapeers. The clean solution would be
                    // to remember the hops-flow value in the connection.
                    if (c != null 
                        && c.getConnectionTime() + 1.25 * HOPS_FLOW_INTERVAL 
                            > System.currentTimeMillis()
                        && c.getConnectionCapabilities().isClientSupernodeConnection() )
                        c.send(hops);
                }
            } else { 
                _oldBusyState = isBusy;
                for(RoutedConnection c : connections) {
                    if (c != null && c.getConnectionCapabilities().isClientSupernodeConnection())
                        c.send(hops);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.MessageRouter#getOOBExpireTime()
     */
    public long getOOBExpireTime() {
    	return OOB_SESSION_EXPIRE_TIME;
    }
    
    /*
     * ===================================================
     *                   "REGULAR" HANDLER
     * ===================================================
     */
    private class PingRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handlePingRequestPossibleDuplicate((PingRequest)msg, handler);
        }
    }
    
    private class PingReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handlePingReply((PingReply)msg, handler);
        }
    }
    
    private class QueryRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleQueryRequestPossibleDuplicate(
                (QueryRequest)msg, (RoutedConnection)handler);
        }
    }
    
    private class QueryReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            // if someone sent a TCP QueryReply with the MCAST header,
            // that's bad, so ignore it.
            QueryReply qmsg = (QueryReply)msg;
            handleQueryReply(qmsg, handler);
        }
    }
    
    private class ResetTableHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleResetTableMessage((ResetTableMessage)msg,
                    (RoutedConnection)handler);
        }
    }
    
    private class PatchTableHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handlePatchTableMessage((PatchTableMessage)msg,
                    (RoutedConnection)handler); 
        }
    }
    
    private class TCPConnectBackHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleTCPConnectBackRequest((TCPConnectBackVendorMessage) msg,
                    (RoutedConnection)handler);
        }
    }
    
    private class UDPConnectBackHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUDPConnectBackRequest((UDPConnectBackVendorMessage) msg,
                    (RoutedConnection)handler);
        }
    }
    
    private class TCPConnectBackRedirectHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleTCPConnectBackRedirect((TCPConnectBackRedirect) msg,
                    (RoutedConnection)handler);
        }
    }
    
    private class UDPConnectBackRedirectHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUDPConnectBackRedirect((UDPConnectBackRedirect) msg,
                    (RoutedConnection)handler);
        }
    }
    
    private class PushProxyRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handlePushProxyRequest((PushProxyRequest) msg, (RoutedConnection)handler);
        }
    }
    
    private class QueryStatusResponseHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleQueryStatus((QueryStatusResponse) msg, (RoutedConnection)handler);
        }
    }
    
    private class HeadPingHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            //TODO: add the statistics recording code
            handleHeadPing((HeadPing)msg, handler);
        }
    }
    
    private class HeadPongHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleHeadPong((HeadPong)msg, handler); 
        }
    }
    
    private class DHTContactsMessageHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleDHTContactsMessage((DHTContactsMessage)msg, handler); 
        }
    }
    
    public static class VendorMessageHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            Connection c = (Connection)handler;
            c.handleVendorMessage((VendorMessage)msg);
        }
    }
    
    /*
     * ===================================================
     *                     UDP HANDLER
     * ===================================================
     */
    private class UDPQueryRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            InetAddress address = addr.getAddress();
            int port = addr.getPort();
            
            // TODO: compare AddressSecurityToken with old generation params.  if it matches
            //send a new one generated with current params 
            if (hasValidQueryKey(address, port, (QueryRequest) msg)) {
                sendAcknowledgement(addr, msg.getGUID());
                // a TTL above zero may indicate a malicious client, as UDP
                // messages queries should not be sent with TTL above 1.
                //if(msg.getTTL() > 0) return;
                handleUDPQueryRequestPossibleDuplicate((QueryRequest)msg, handler);
            }
        }
    }
    
    private class UDPPingRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUDPPingRequestPossibleDuplicate((PingRequest)msg, handler, addr);
        }
    }
    
    private class UDPPingReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUDPPingReply((PingReply)msg, handler, addr.getAddress(), addr.getPort());
        }
    }
    
    private class UDPHeadPingHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            //TODO: add the statistics recording code
            handleHeadPing((HeadPing)msg, handler);
        }
    }
    
    /*
     * ===================================================
     *                  MULTICAST HANDLER
     * ===================================================
     */
    public class MulticastQueryRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr,
                ReplyHandler handler) {
            handleUDPQueryRequestPossibleDuplicate((QueryRequest) msg, handler);
        }
    }
    
    public class MulticastPingRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, 
                ReplyHandler handler) {
            handleUDPPingRequestPossibleDuplicate((PingRequest)msg, handler, addr);
        }
    }
    
    /**
     * This class handles UDP query replies and forwards them to the 
     * {@link OOBHandler} if they are not replies to multicast or unicast
     * queries.
     */
    public class UDPQueryReplyHandler implements MessageHandler {

        private final OOBHandler oobHandler;
        
        public UDPQueryReplyHandler(OOBHandler oobHandler) {
            this.oobHandler = oobHandler;
        }
        
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            QueryReply reply = (QueryReply)msg;
            if (reply.isReplyToMulticastQuery()
                    || isHostUnicastQueried(new GUID(reply.getGUID()), handler)) {
                handleQueryReply(reply, handler);
            }
            else {
                oobHandler.handleMessage(msg, addr, handler);
            }
        }
        
    }
    
    private class ConnectionListener implements ConnectionLifecycleListener {

        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
            if (evt.isConnectionClosedEvent()) {
                removeConnection(evt.getConnection());
            }
        }
        
    }
}
