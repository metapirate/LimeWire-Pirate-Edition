package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.NBThrottle;
import org.limewire.nio.Throttle;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.DeflaterWriter;
import org.limewire.nio.channel.DelayedBufferWriter;
import org.limewire.nio.channel.InflaterReader;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.channel.StatisticGatheringWriter;
import org.limewire.nio.channel.ThrottleWriter;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.GuidMap;
import com.limegroup.gnutella.GuidMapManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.AsyncIncomingHandshaker;
import com.limegroup.gnutella.handshaking.AsyncOutgoingHandshaker;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeObserver;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.Handshaker;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.OOBProxyControlVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * A Connection managed by a ConnectionManager.
 * 
 * GnutellaConnection provides a sophisticated message buffering mechanism. When
 * you call send(Message), the message is not actually delivered to the socket;
 * instead it buffered in an application-level buffer. Periodically, messages
 * are read from the buffer and written to the network. Furthermore,
 * GnutellaConnection provides a simple form of flow control. If messages are
 * queued faster than they can be written to the network, they are dropped in
 * the following order: PingRequest, PingReply, QueryRequest, QueryReply, and
 * PushRequest. See the implementation notes below for more details.
 * <p>
 * 
 * All GnutellaConnection have two underlying spam filters: a personal filter
 * (controls what I see) and a route filter (also controls what I pass along to
 * others). See SpamFilter for a description. You can change them with
 * setPersonalFilter and setRouteFilter.
 * <p>
 * 
 * GnutellaConnection maintain a large number of statistics, such as the current
 * bandwidth for upstream & downstream.
 * <p>
 * 
 * GnutellaConnection also takes care of various VendorMessage handling, in
 * particular Hops Flow, UDP ConnectBack, and TCP ConnectBack. See
 * handleVendorMessage().
 * <p>
 * 
 * This class implements ReplyHandler to route pongs and query replies that
 * originated from it.
 * <p>
 */
public class GnutellaConnection extends AbstractConnection implements ReplyHandler,
        MessageReceiver, SentMessageHandler, Shutdownable, RoutedConnection,
        ConnectionRoutingStatistics, ConnectionMessageStatistics, ListenerSupport<GnutellaConnectionEvent> {

    private static final Log LOG = LogFactory.getLog(GnutellaConnection.class);
    
    public enum EventType {
        IS_PUSH_PROXY    
    }

    /**
     * The time to wait between route table updates for leaves, in milliseconds.
     */
    private long LEAF_QUERY_ROUTE_UPDATE_TIME = 1000 * 60 * 5; // 5 minutes

    /**
     * The time to wait between route table updates for Ultrapeers, in
     * milliseconds.
     */
    private long ULTRAPEER_QUERY_ROUTE_UPDATE_TIME = 1000 * 60; // 1 minute

    /**
     * The timeout to use when connecting, in milliseconds. This is NOT used for
     * bootstrap servers.
     */
    private static final int CONNECT_TIMEOUT = 6000; // 6 seconds

    /**
     * The total amount of upstream messaging bandwidth for ALL connections in
     * BYTES (not bits) per second.
     */
    private static final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH = 8000;

    /**
     * Filter for filtering out messages that are considered spam.
     */
    private volatile SpamFilter _routeFilter;

    /*
     * IMPLEMENTATION NOTE: this class uses the SACHRIFC algorithm described at
     * http://www.limewire.com/developer/sachrifc.txt. The basic idea is to use
     * one queue for each message type. Messages are removed from the queue in a
     * biased round-robin fashion. This prioritizes some messages types while
     * preventing any one message type from dominating traffic. Query replies
     * are further prioritized by "GUID volume", i.e., the number of bytes
     * already routed for that GUID. Other messages are sorted by time and
     * removed in a LIFO [sic] policy. This, coupled with timeouts, reduces
     * latency.
     */

    /** A lock for QRP activity on this connection. */
    private final Object QRP_LOCK = new Object();

    /** Non-blocking throttle for outgoing messages. */
    private final static Throttle _nbThrottle = new NBThrottle(true,
            TOTAL_OUTGOING_MESSAGING_BANDWIDTH, ConnectionSettings.NUM_CONNECTIONS.getValue(),
            CompositeQueue.QUEUE_TIME);

    /** The OutputRunner */
    private volatile OutputRunner _outputRunner;

    /** Keeps track of sent/received [dropped] & bandwidth. */
    private final ConnectionStats _connectionStats = new ConnectionStats();

    /**
     * The next time I should send a query route table to this connection.
     */
    private long _nextQRPForwardTime;

    /**
     * The bandwidth trackers for the up/downstream. These are not synchronized
     * and not guaranteed to be 100% accurate.
     */
    private BandwidthTrackerImpl _upBandwidthTracker = new BandwidthTrackerImpl();

    private BandwidthTrackerImpl _downBandwidthTracker = new BandwidthTrackerImpl();

    /**
     * True iff this should not be policed by the ConnectionWatchdog, e.g.,
     * because this is a connection to a Clip2 reflector.
     */
    private boolean _isKillable = true;

    /**
     * Use this if a HopsFlowVM instructs us to stop sending queries below this
     * certain hops value....
     */
    private volatile int hopsFlowMax = -1;

    /**
     * This member contains the time beyond which, if this host is still busy
     * (hops flow==0), that we should consider him as "truly idle" and should
     * then remove his contributions last-hop QRTs. A value of -1 means that
     * either the leaf isn't busy, or he is busy, and his busy-ness was already
     * noticed by the MessageRouter, so we shouldn't 're-notice' him on the next
     * QRT update iteration.
     */
    private volatile long _busyTime = -1;

    /**
     * Whether this connection is a push proxy for me.
     */
    private volatile boolean myPushProxy;

    /**
     * Whether I am a push proxy for this connection.
     */
    private volatile boolean pushProxyFor;

    /**
     * Variable for the <tt>QueryRouteTable</tt> received for this connection.
     */
    private volatile QueryRouteTable _lastQRPTableReceived;

    /**
     * Variable for the <tt>QueryRouteTable</tt> sent for this connection.
     */
    private volatile QueryRouteTable _lastQRPTableSent;

    /**
     * Whether or not this was a supernode <-> client connection when message
     * looping started.
     */
    private boolean supernodeClientAtLooping = false;

    private volatile Deflater deflater;

    private volatile Inflater inflater;

    /**
     * The last clientGUID a Hops=0 QueryReply had.
     */
    private volatile byte[] clientGUID = DataUtils.EMPTY_GUID;

    /**
     * Whether or not the HandshakeResponder should use locale preferencing
     * during handshaking.
     */
    private volatile boolean _useLocalPreference;

    /**
     * The maximum protocol version for which OOB proxying has been turned off
     * by leaf peer. Defaults to 0 to allow all OOB versions to be proxied.
     */
    private int _maxDisabledOOBProtocolVersion = 0;

    private final ConnectionManager connectionManager;

    private final NetworkManager networkManager;

    private final QueryRequestFactory queryRequestFactory;

    private final HeadersFactory headersFactory;

    private final HandshakeResponderFactory handshakeResponderFactory;

    private final QueryReplyFactory queryReplyFactory;

    private final MessageDispatcher messageDispatcher;

    private final Provider<SearchResultHandler> searchResultHandler;

    private final Provider<ConnectionServices> connectionServices;

    private final GuidMapManager guidMapManager;

    private final SocketsManager socketsManager;

    private final GuidMap guidMap;

    private final MessageReaderFactory messageReaderFactory;

    private final ApplicationServices applicationServices;

    @SuppressWarnings("unused")
    private final SecureMessageVerifier secureMessageVerifier;
    
    private final OutOfBandStatistics outOfBandStatistics;
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    /** writers of statistics if any */
    private static enum StatsWriters {TOP, DEFLATER, DELAYER, THROTTLE }
    private final Map<StatsWriters,StatisticGatheringWriter> statsWriters = new HashMap<StatsWriters,StatisticGatheringWriter>();
    private volatile long droppedBadHops, droppedBadAddress,droppedFW;
    
    private final EventListenerList<GnutellaConnectionEvent> listeners =
        new EventListenerList<GnutellaConnectionEvent>();

    /**
     * Creates a new outgoing connection to the specified host on the specified
     * port, using the specified kind of ConnectType.
     * 
     * @param host the address of the host we're connecting to
     * @param port the port the host is listening on
     * @param type the type of outgoing connection we want to make (TLS, PLAIN,
     *        etc)
     */
    public GnutellaConnection(String host, int port, ConnectType type,
            ConnectionManager connectionManager, NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory, HeadersFactory headersFactory,
            HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory, MessageDispatcher messageDispatcher,
            Provider<SearchResultHandler> searchResultHandler, CapabilitiesVMFactory capabilitiesVMFactory,
            SocketsManager socketsManager, Acceptor acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
            SpamFilterFactory spamFilterFactory, MessageReaderFactory messageReaderFactory,
            MessageFactory messageFactory, ApplicationServices applicationServices,
            SecureMessageVerifier secureMessageVerifier, OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
        super(host, port, type, capabilitiesVMFactory, supportedVendorMessage, networkManager,
                acceptor, networkInstanceUtils);
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.messageDispatcher = messageDispatcher;
        this.searchResultHandler = searchResultHandler;
        this.connectionServices = connectionServices;
        this.guidMapManager = guidMapManager;
        this.messageReaderFactory = messageReaderFactory;
        this.applicationServices = applicationServices;
        this.guidMap = guidMapManager.getMap();
        this._routeFilter = spamFilterFactory.createRouteFilter();
        this.secureMessageVerifier = secureMessageVerifier;
        this.socketsManager = socketsManager;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /**
     * Creates an incoming connection. ManagedConnections should only be
     * constructed within ConnectionManager.
     * 
     * @requires the word "GNUTELLA " and nothing else has just been read from
     *           socket
     * @effects wraps a connection around socket and does the rest of the
     *          Gnutella handshake.
     */
    public GnutellaConnection(Socket socket, ConnectionManager connectionManager,
            NetworkManager networkManager, QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory, HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory, MessageDispatcher messageDispatcher,
            Provider<SearchResultHandler> searchResultHandler, CapabilitiesVMFactory capabilitiesVMFactory,
            Acceptor acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
            SpamFilterFactory spamFilterFactory, MessageReaderFactory messageReaderFactory,
            MessageFactory messageFactory, ApplicationServices applicationServices,
            SecureMessageVerifier secureMessageVerifier, OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
        super(socket, capabilitiesVMFactory, supportedVendorMessage, networkManager, acceptor,
                networkInstanceUtils);
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.messageDispatcher = messageDispatcher;
        this.searchResultHandler = searchResultHandler;
        this.connectionServices = connectionServices;
        this.guidMapManager = guidMapManager;
        this.messageReaderFactory = messageReaderFactory;
        this.applicationServices = applicationServices;
        this.guidMap = guidMapManager.getMap();
        this._routeFilter = spamFilterFactory.createRouteFilter();
        this.secureMessageVerifier = secureMessageVerifier;
        this.socketsManager = null;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#initialize(com.limegroup.gnutella.connection.GnetConnectObserver)
     */
    public void initialize(GnetConnectObserver observer) throws IOException {
        if (observer == null && isOutgoing())
            throw new NullPointerException("must have an observer if outgoing!");

        Properties requestHeaders;
        HandshakeResponder responder;

        addListener(connectionManager);
        if (isOutgoing()) {
            String host = getAddress();
            if (connectionServices.get().isSupernode()) {
                requestHeaders = headersFactory.createUltrapeerHeaders(host);
                responder = handshakeResponderFactory.createUltrapeerHandshakeResponder(host);
            } else {
                requestHeaders = headersFactory.createLeafHeaders(host);
                responder = handshakeResponderFactory.createLeafHandshakeResponder(host);
            }
        } else {
            String host = getSocket().getInetAddress().getHostAddress();
            requestHeaders = null;
            if (connectionServices.get().isSupernode()) {
                responder = handshakeResponderFactory.createUltrapeerHandshakeResponder(host);
            } else {
                responder = handshakeResponderFactory.createLeafHandshakeResponder(host);
            }
        }

        // Establish the socket (if needed), handshake.
        initialize(requestHeaders, responder, CONNECT_TIMEOUT, observer);

        // Nothing else should be done here. All post-init-sequences
        // should be triggered from finishInitialize, which will be called
        // when the socket is connected (if it connects).
    }

    /**
     * Initialize the connection by doing the handshake. Throws IOException if
     * we were unable to establish a normal messaging connection for any reason.
     * Do not call send or receive if this happens.
     * 
     * @param timeout for outgoing connections, the timeout in milliseconds to
     *        use in establishing the socket, or 0 for no timeout. If the
     *        platform does not support native timeouts, it will be emulated
     *        with threads.
     * @exception IOException we were unable to connect to the host
     * @exception NoGnutellaOkException one of the participants responded with
     *            an error code other than 200 OK (possibly after several rounds
     *            of 401's)
     * @exception BadHandshakeException some other problem establishing the
     *            connection, e.g., the server responded with HTTP, closed the
     *            the connection during handshaking, etc.
     */
    protected void initialize(Properties requestHeaders, HandshakeResponder responder, int timeout,
            GnetConnectObserver observer) throws IOException {
        responder.setLocalePreferencing(_useLocalPreference);

        if (isOutgoing()) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Outgoing connection to " +
                        getAddress() + ":" + getPort() +
                        " with timeout " + timeout);
            }
            ConnectObserver connectObserver = new AsyncHandshakeConnecter(requestHeaders,
                    responder, observer);
            InetSocketAddress host = new InetSocketAddress(getAddress(), getPort());
            Socket socket = socketsManager
                    .connect(host, timeout, connectObserver, getConnectType());
            setSocket(socket);
        } else {
            if(LOG.isInfoEnabled()) {
                LOG.info("Incoming connection from " +
                        getAddress() + ":" + getPort());
            }
            startHandshake(requestHeaders, responder, observer);
        }
    }

    /**
     * Starts the handshake process.
     */
    private void startHandshake(Properties requestHeaders, HandshakeResponder responder,
            GnetConnectObserver observer) throws IOException {
        if(LOG.isInfoEnabled())
            LOG.info("Shaking hands with " + getAddress() + ":" + getPort());
        
        initializeHandshake();

        HandshakeWatcher shakeObserver = new HandshakeWatcher(observer);
        Handshaker shaker;
        if (isOutgoing())
            shaker = new AsyncOutgoingHandshaker(requestHeaders, responder, getSocket(),
                    shakeObserver);
        else
            shaker = new AsyncIncomingHandshaker(responder, getSocket(), shakeObserver);

        shakeObserver.setHandshaker(shaker);
        try {
            shaker.shake();
        } catch (IOException iox) {
            ErrorService.error(iox); // impossible.
        }
    }

    /**
     * Starts out OutputRunners & notifies UpdateManager that this connection
     * may have an update on it.
     */
    private void postHandshakeInitialize(Handshaker shaker) {
        handshakeInitialized(shaker);
        
        if (isWriteDeflated()) {
            deflater = new Deflater();
        }

        if (isReadDeflated()) {
            inflater = new Inflater();
        }

        getConnectionBandwidthStatistics().setCompressionOption(isWriteDeflated(),
                isReadDeflated(), new CompressionBandwidthTrackerImpl(inflater, deflater));

        startOutput();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#resetQueryRouteTable(com.limegroup.gnutella.routing.ResetTableMessage)
     */
    public void resetQueryRouteTable(ResetTableMessage rtm) {
        if (_lastQRPTableReceived == null) {
            _lastQRPTableReceived = new QueryRouteTable(rtm.getTableSize(), rtm.getInfinity());
        } else {
            _lastQRPTableReceived.reset(rtm);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#patchQueryRouteTable(com.limegroup.gnutella.routing.PatchTableMessage)
     */
    public void patchQueryRouteTable(PatchTableMessage ptm) {

        // we should always get a reset before a patch, but
        // allocate a table in case we don't
        if (_lastQRPTableReceived == null) {
            _lastQRPTableReceived = new QueryRouteTable();
        }
        try {
            _lastQRPTableReceived.patch(ptm);
        } catch (BadPacketException e) {
            // not sure what to do here!!
        }
    }

    /**
     * Set's a leaf's busy timer to now, if bSet is true, else clears the flag.
     * 
     * @param bSet whether to SET or CLEAR the busy timer for this host
     */
    public void setBusy(boolean bSet) {
        if (bSet) {
            if (_busyTime == -1)
                _busyTime = System.currentTimeMillis();
        } else
            _busyTime = -1;
    }

    private byte getHopsFlowMax() {
        return (byte) hopsFlowMax;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#isBusyLeaf()
     */
    public boolean isBusyLeaf() {
        if (!isSupernodeClientConnection())
            return false;
        int hfm = getHopsFlowMax();
        return hfm >= 0 && hfm < 3;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#shouldForwardQuery(com.limegroup.gnutella.messages.QueryRequest)
     */
    public boolean shouldForwardQuery(QueryRequest query) {
        // special what is queries have version numbers attached to them - make
        // sure that the remote host can answer the query....
        if (query.isFeatureQuery()) {
            if (isSupernodeClientConnection())
                return (getConnectionCapabilities().getRemoteHostFeatureQuerySelector() >= query
                        .getFeatureSelector());
            else if (getConnectionCapabilities().isSupernodeSupernodeConnection())
                return getConnectionCapabilities().getRemoteHostSupportsFeatureQueries();
            else
                return false;
        }
        return hitsQueryRouteTable(query);
    }

    /**
     * Determines whether or not this query hits the QRT.
     */
    protected boolean hitsQueryRouteTable(QueryRequest query) {
        if (_lastQRPTableReceived == null)
            return false;
        return _lastQRPTableReceived.contains(query);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionStatistics#getQueryRouteTableReceived()
     */
    public QueryRouteTable getQueryRouteTableReceived() {
        return _lastQRPTableReceived;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionStatistics#getQueryRouteTablePercentFull()
     */
    public double getQueryRouteTablePercentFull() {
        return _lastQRPTableReceived == null ? 0 : _lastQRPTableReceived.getPercentFull();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionStatistics#getQueryRouteTableSize()
     */
    public int getQueryRouteTableSize() {
        return _lastQRPTableReceived == null ? 0 : _lastQRPTableReceived.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionStatistics#getQueryRouteTableEmptyUnits()
     */
    public int getQueryRouteTableEmptyUnits() {
        return _lastQRPTableReceived == null ? -1 : _lastQRPTableReceived.getEmptyUnits();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionStatistics#getQueryRouteTableUnitsInUse()
     */
    public int getQueryRouteTableUnitsInUse() {
        return _lastQRPTableReceived == null ? -1 : _lastQRPTableReceived.getUnitsInUse();
    }

    // //////////////////// Sending, Outgoing Flow Control
    // //////////////////////

    /** Starts outgoing messages being sent. */
    private void startOutput() {
        
        if(LOG.isInfoEnabled())
            LOG.info("Starting output to " + getAddress() + ":" + getPort());

        // add some heavier stats code in betas
        if (LimeWireUtils.isBetaRelease() || LimeWireUtils.isTestingVersion()) {
            statsWriters.put(StatsWriters.TOP,new StatisticGatheringWriter());
            statsWriters.put(StatsWriters.DEFLATER, new StatisticGatheringWriter());
            statsWriters.put(StatsWriters.DELAYER, new StatisticGatheringWriter());
            statsWriters.put(StatsWriters.THROTTLE, new StatisticGatheringWriter());
        }
        
        MessageQueue queue;
        // Taking this change out until we can safely handle attacks and
        // overflow
        // TODO: make a cheaper Queue that still prevents flooding of ultrapeer
        // and ensures that clogged leaf doesn't drop QRP messages.
        // if(isSupernodeSupernodeConnection())
        queue = new CompositeQueue();
        // else
        // queue = new BasicQueue();

        // TODO: ensure socket is asynchronous!

        MessageWriter messager = new MessageWriter(_connectionStats, queue, this);
        _outputRunner = messager;
        
        ChannelWriter writer = messager;
        if (statsWriters.containsKey(StatsWriters.TOP))
            writer = addWriter(writer, statsWriters.get(StatsWriters.TOP));

        if (isWriteDeflated()) {
            writer = addWriter(writer, new DeflaterWriter(deflater));
            if (statsWriters.containsKey(StatsWriters.DEFLATER))
                writer = addWriter(writer, statsWriters.get(StatsWriters.DEFLATER));
        }
        
        writer = addWriter(writer, new DelayedBufferWriter(1400));
        if (statsWriters.containsKey(StatsWriters.DELAYER))
            writer = addWriter(writer, statsWriters.get(StatsWriters.DELAYER));
        
        writer = addWriter(writer, new ThrottleWriter(_nbThrottle));
        if (statsWriters.containsKey(StatsWriters.THROTTLE))
            writer = addWriter(writer, statsWriters.get(StatsWriters.THROTTLE));

        ((NIOMultiplexor) getSocket()).setWriteObserver(messager);
    }
    
    private <T extends InterestWritableByteChannel & ChannelWriter> ChannelWriter addWriter(ChannelWriter chain, T newWriter) {
        chain.setWriteChannel(newWriter);
        return newWriter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#send(com.limegroup.gnutella.messages.Message)
     */
    public void send(Message m) {
        if(m instanceof QueryRequest && !shouldSendQuery((QueryRequest)m))
                return;        
        _outputRunner.send(m);
    }

    /**
     * Default access for testing.
     * @return true if the query should be sent.
     */
    boolean shouldSendQuery(QueryRequest query) {
        // if Hops Flow is in effect, and this is a QueryRequest, and the
        // hoppage is too biggage, discardage time...
        int smh = hopsFlowMax;
        if (smh > -1 && query.getHops() >= smh)
            return false;
        
        // if we are an ultrapeer sending a query to a leaf, check its firewall status
        boolean send = true;
        if (isSupernodeClientConnection() && MessageSettings.ULTRAPEER_FIREWALL_FILTERING.getValue()) {
            boolean incomingTCP = getConnectionCapabilities().canAcceptIncomingTCP();
            boolean fwt = getConnectionCapabilities().canDoFWT();
            
            // if either party can accept tcp, send.
            // otherwise send if both sides can do fwt.
            if (!incomingTCP && query.isFirewalledSource()) 
                send = fwt && query.canDoFirewalledTransfer();
            if (send)
                droppedFW++;
        }
        return send;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#originateQuery(com.limegroup.gnutella.messages.QueryRequest)
     */
    public void originateQuery(QueryRequest query) {
        query.originate();
        
        if(LOG.isInfoEnabled()) {
            boolean leaf = getConnectionCapabilities().isClientSupernodeConnection();
            int control = getConnectionCapabilities().getSupportedOOBProxyControlVersion();
            boolean disabled = SearchSettings.DISABLE_OOB_V2.getBoolean();
            LOG.info("Originating query, leaf " + leaf +
                    ", proxy control version " + control +
                    ", OOBv2 disabled " + disabled);
        }

        if (getConnectionCapabilities().isClientSupernodeConnection()
                && getConnectionCapabilities().getSupportedOOBProxyControlVersion() == -1
                && SearchSettings.DISABLE_OOB_V2.getBoolean()) {
            LOG.info("Creating do not proxy OOB query");
            // don't proxy if we are a leaf and the ultrapeer
            // does not know OOB v3 and they would proxy for us
            query = queryRequestFactory.createDoNotProxyQuery(query);
            query.originate();
        }

        send(query);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#shutdown()
     */
    public void shutdown() {
        close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#close()
     */
    @Override
    protected void closeImpl() {
        IOUtils.close(deflater);
        IOUtils.close(inflater);

        if (_outputRunner != null)
            _outputRunner.shutdown();

        // release pointer to our _guidMap so it can be gc()'ed
        guidMapManager.removeMap(guidMap);
    }

    // ////////////////////////////////////////////////////////////////////////

    /**
     * Handles core Gnutella request/reply protocol. If asynchronous messaging
     * is supported, this immediately returns and messages are processed
     * asynchronously via processMessage calls. Otherwise, if reading blocks,
     * this will run until the connection is closed.
     * 
     * @requires this is initialized
     * @modifies the network underlying this, manager
     * @effects receives request and sends appropriate replies.
     * 
     * @throws IOException passed on from the receive call; failures to forward
     *         or route messages are silently swallowed, allowing the message
     *         loop to continue.
     */
    public void startMessaging() {
        supernodeClientAtLooping = isSupernodeClientConnection();

        LOG.info("Starting asynchronous connection");
        try {
            getSocket().setSoTimeout(0); // no timeout for reading.
        } catch (IOException iox) {
            // ignore for now...
        }

        MessageReader reader = messageReaderFactory.createMessageReader(this);
        if (isReadDeflated())
            reader.setReadChannel(new InflaterReader(inflater));

        ((NIOMultiplexor) getSocket()).setReadObserver(reader);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#messagingClosed()
     */
    public void messagingClosed() {
        // we must run this in another thread, as manager.remove
        // obtains locks, but this can be called from the NIO thread
        if (connectionManager != null) {
            messageDispatcher.dispatch(new Runnable() {
                public void run() {
                    connectionManager.remove(GnutellaConnection.this);
                }
            });
        }
    }

    @Override
    public void processReadMessage(Message m) {
        super.processReadMessage(m);
        _connectionStats.addReceived();
        handleMessageInternal(m);
    }

    public void processSentMessage(Message m) {
        processWrittenMessage(m);
        messageDispatcher.dispatchTCPMessageSent(m, this);
    }

    /**
     * Handles a message without updating appropriate statistics.
     */
    private void handleMessageInternal(Message m) {
        // Run through the route spam filter and drop accordingly.
        if (isSpam(m)) {
            _connectionStats.addReceivedDropped();
        } else {
            if (m instanceof QueryReply) {
                QueryReply reply = (QueryReply)m;
                _connectionStats.replyReceived(reply);
                
                if (m.getHops() == 0)
                    clientGUID = reply.getClientGUID();
                
                if (MessageSettings.RETURN_PATH_IN_REPLIES.getValue() && 
                        connectionManager.isActiveSupernode() &&
                        !reply.hasSecureData()) {
                    m = queryReplyFactory.createWithReturnPathInfo(reply, 
                            myIp == null ? null : new IpPortImpl(myIp,networkManager.getPort()), 
                                    this);
                }
            } else if (m instanceof PushProxyRequest && m.getHops() == 0) {
                PushProxyRequest pushProxyRequest = (PushProxyRequest)m;
                clientGUID = pushProxyRequest.getClientGUID().bytes();
            }

            if (m instanceof QueryRequest)
                _connectionStats.queryReceived();
            // special handling for proxying.
            if (supernodeClientAtLooping) {
                if (m instanceof QueryRequest)
                    m = tryToProxy((QueryRequest) m);
                else if (m instanceof QueryStatusResponse)
                    m = morphToStopQuery((QueryStatusResponse) m);
            }
            messageDispatcher.dispatchTCP(m, this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionStatistics#getNumQueryReplies()
     */
    public long getNumQueryReplies() {
        return _connectionStats.getRepliesReceived();
    }
    
    public long getNumQueriesReceived() {
        return _connectionStats.getQueriesReceived();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#getNetwork()
     */
    public Network getNetwork() {
        return Network.TCP;
    }

    // raise access for MessageReceiver
    @Override
    public byte getSoftMax() {
        return super.getSoftMax();
    }

    private QueryRequest tryToProxy(QueryRequest query) {
        // we must have the following qualifications:
        // 1) Leaf must be sending SuperNode a query
        // 2) Leaf must support Leaf Guidance
        // 3) Query must not be OOB.
        // 3.5) The query originator should not disallow proxying.
        // 4) We must be able to OOB and have great success rate.
        if (getConnectionCapabilities().remoteHostSupportsLeafGuidance() < 1) {
            LOG.info("Not OOB proxying: remote host doesn't support leaf guidance");
            return query;
        }
        if (query.desiresOutOfBandRepliesV3()) {
            LOG.info("Not OOB proxying: query already requests OOBv3");
            return query;
        }

        if (query.doNotProxy()) {
            LOG.info("Not OOB proxying: query says no");
            return query;
        }

        if (_maxDisabledOOBProtocolVersion >= ReplyNumberVendorMessage.VERSION) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Not OOB proxying: disabled version is "
                        + _maxDisabledOOBProtocolVersion);
            }
            return query;
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("query might be proxied for max disabled version "
                        + _maxDisabledOOBProtocolVersion + " " + Arrays.toString(query.getGUID()));
            }
        }

        if(!networkManager.isOOBCapable()) {
            LOG.info("Not OOB proxying: not OOB capable");
            return query;
        }
        if(!outOfBandStatistics.isSuccessRateGreat()) {
            LOG.info("Not OOB proxying: success rate isn't great");
            return query;
        }
        if(!outOfBandStatistics.isOOBEffectiveForProxy()) {
            LOG.info("Not OOB proxying: OOB isn't effective enough");
            return query;
        }

        // everything is a go - we need to do the following:
        // 1) mutate the GUID of the query - you should maintain every param of
        // the query except the new GUID and the OOB minspeed flag
        // 2) set up mappings between the old guid and the new guid.
        // after that, everything is set. all you need to do is map the guids
        // of the replies back to the original guid. also, see if a you get a
        // QueryStatusResponse message and morph it...
        // THIS IS SOME MAJOR HOKERY-POKERY!!!
        LOG.info("OOB proxying query");

        // 1) mutate the GUID of the query
        byte[] origGUID = query.getGUID();
        byte[] oobGUID = new byte[origGUID.length];
        System.arraycopy(origGUID, 0, oobGUID, 0, origGUID.length);
        GUID.addressEncodeGuid(oobGUID, networkManager.getAddress(), networkManager.getPort());
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(oobGUID);

        query = queryRequestFactory.createProxyQuery(query, oobGUID);

        // 2) set up mappings between the guids
        guidMap.addMapping(origGUID, oobGUID);

        outOfBandStatistics.addSentQuery();
        return query;
    }

    private QueryStatusResponse morphToStopQuery(QueryStatusResponse resp) {
        GUID oobGUID = guidMap.getNewGUID(resp.getQueryGUID());
        // if we had a match, then just construct a new one....
        if (oobGUID != null)
            return new QueryStatusResponse(oobGUID, resp.getNumResults());
        else
            return resp;
    }

    boolean isSpam(Message m) {
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
            return !_routeFilter.allow(m);
        
        // leafs can only send hops == 0
        if (isSupernodeClientConnection() && m.getHops() != 0) {
            droppedBadHops++;
            return true;
        }
        
        // replies with hops 0 must match the address.
        if (m instanceof QueryReply && m.getHops() == 0) {
            QueryReply reply = (QueryReply)m;
            byte [] ip = reply.getIPBytes();
            if (!networkInstanceUtils.isPrivateAddress(ip) &&
                    !reply.hasSecureData() &&
                    !Arrays.equals(ip, getAddressBytes())) {
                droppedBadAddress++;
                return true;
            }
        }
        return !_routeFilter.allow(m);
    }

    public void countDroppedMessage() {
        LOG.trace("Dropped a message");
        _connectionStats.addReceivedDropped();
    }

    public void setRouteFilter(SpamFilter filter) {
        _routeFilter = filter;
    }

    public void handlePingReply(PingReply pingReply, ReplyHandler receivingConnection) {
        send(pingReply);
    }

    public void handleQueryReply(QueryReply queryReply, ReplyHandler receivingConnection) {

        boolean checkOOB = true;
        if (guidMap != null) {
            byte[] origGUID = guidMap.getOriginalGUID(queryReply.getGUID());
            if (origGUID != null) {
                checkOOB = false;
                byte prevHops = queryReply.getHops();
                queryReply = queryReplyFactory.createQueryReply(origGUID, queryReply);
                queryReply.setTTL((byte) 2); // we ttl 1 more than necessary
                queryReply.setHops(prevHops);
            }
            // ---------------------
        }

        // drop UDP replies that are not being proxied.
        if (checkOOB && queryReply.isUDP() && !queryReply.isReplyToMulticastQuery())
            return;

        // if the remote side reported an ip and this reply is from us, make sure
        // its source ip matches.
        if (myIp != null && 
                queryReply.isLocal() &&
                !networkInstanceUtils.isPrivateAddress(queryReply.getIPBytes()) &&
                !queryReply.hasSecureData()) // don't mess with signed results
            queryReply = queryReplyFactory.createWithNewAddress(myIp, queryReply);
        send(queryReply);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#getClientGUID()
     */
    public byte[] getClientGUID() {
        return clientGUID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RoutedConnection#handlePushRequest(com.limegroup.gnutella.messages.PushRequest,
     *      com.limegroup.gnutella.ReplyHandler)
     */
    public void handlePushRequest(PushRequest pushRequest, ReplyHandler receivingConnection) {
        send(pushRequest);
    }
    
    @Override
    public void handleVendorMessage(VendorMessage vm) {
        // let Connection do as needed....
        super.handleVendorMessage(vm);

        // now i can process
        if (vm instanceof HopsFlowVendorMessage) {
            // update the softMaxHops value so it can take effect....
            HopsFlowVendorMessage hops = (HopsFlowVendorMessage) vm;

            if (isSupernodeClientConnection())
                // If the connection is to a leaf, and it is busy (HF == 0)
                // then set the global busy leaf flag appropriately
                setBusy(hops.getHopValue() == 0);

            hopsFlowMax = hops.getHopValue();
        } else if (vm instanceof PushProxyAcknowledgement) {
            // this connection can serve as a PushProxy, so note this....
            PushProxyAcknowledgement ack = (PushProxyAcknowledgement) vm;
            if (Arrays.equals(ack.getGUID(), applicationServices.getMyGUID())) {
                myPushProxy = true;
                fireEvent(new GnutellaConnectionEvent(this, EventType.IS_PUSH_PROXY));
            }
            // else mistake on the server side - the guid should be my client
            // guid - not really necessary but whatever
        } else if (vm instanceof CapabilitiesVM) {
            // fire a vendor event
            connectionManager.dispatchEvent(new ConnectionLifecycleEvent(this,
                    ConnectionLifecycleEventType.CONNECTION_CAPABILITIES, this));

        } else if (vm instanceof MessagesSupportedVendorMessage) {
            // If this is a ClientSupernodeConnection and the host supports
            // leaf guidance (because we have to tell them when to stop)
            // then see if there are any old queries that we can re-originate
            // on this connection.
            if (getConnectionCapabilities().isClientSupernodeConnection()
                    && (getConnectionCapabilities().remoteHostSupportsLeafGuidance() >= 0)) {
                List<QueryRequest> queries = searchResultHandler.get().getQueriesToReSend();
                for (QueryRequest qr : queries) {
                    send(qr);
                }
            }

            // see if you need a PushProxy - the remoteHostSupportsPushProxy
            // test incorporates my leaf status in it.....
            if (getConnectionCapabilities().remoteHostSupportsPushProxy() > -1) {
                // get the client GUID and send off a PushProxyRequest
                GUID clientGUID = new GUID(applicationServices.getMyGUID());
                PushProxyRequest req = new PushProxyRequest(clientGUID);
                send(req);
            }

            // do i need to send any ConnectBack messages????
            if (!networkManager.canReceiveUnsolicited()
                    && connectionManager.canSendConnectBack(Network.UDP)
                    && (getConnectionCapabilities().remoteHostSupportsUDPRedirect() > -1)) {
                GUID connectBackGUID = networkManager.getUDPConnectBackGUID();
                Message udp = new UDPConnectBackVendorMessage(networkManager.getPort(),
                        connectBackGUID);
                send(udp);
                connectionManager.connectBackSent(Network.UDP);
            }

            if (!networkManager.acceptedIncomingConnection()
                    && connectionManager.canSendConnectBack(Network.TCP)
                    && (getConnectionCapabilities().remoteHostSupportsTCPRedirect() > -1)) {
                Message tcp = new TCPConnectBackVendorMessage(networkManager.getPort());
                send(tcp);
                connectionManager.connectBackSent(Network.TCP);
            }

            // disable oobv2 explicitly.
            if (getConnectionCapabilities().isClientSupernodeConnection()
                    && SearchSettings.DISABLE_OOB_V2.getBoolean()
                    && getConnectionCapabilities().getSupportedOOBProxyControlVersion() != -1) {
                Message stopv2 = new OOBProxyControlVendorMessage(
                        OOBProxyControlVendorMessage.Control.DISABLE_VERSION_2);
                send(stopv2);
            }
        } else if (vm instanceof OOBProxyControlVendorMessage) {
            _maxDisabledOOBProtocolVersion = ((OOBProxyControlVendorMessage) vm)
                    .getMaximumDisabledVersion();
            if(LOG.isTraceEnabled()) {
                LOG.trace("Max disabled OOB version set to " +
                        _maxDisabledOOBProtocolVersion);
            }
        }
    }

    public void addListener(EventListener<GnutellaConnectionEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<GnutellaConnectionEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    private void fireEvent(GnutellaConnectionEvent event) {
        listeners.broadcast(event);    
    }

    public int getNumMessagesSent() {
        return _connectionStats.getSent();
    }

    public int getNumMessagesReceived() {
        return _connectionStats.getReceived();
    }

    public int getNumSentMessagesDropped() {
        return _connectionStats.getSentDropped();
    }

    public long getNumReceivedMessagesDropped() {
        return _connectionStats.getReceivedDropped();
    }

    public void measureBandwidth() {
        _upBandwidthTracker.measureBandwidth(ByteUtils.long2int(getConnectionBandwidthStatistics()
                .getBytesSent()));
        _downBandwidthTracker.measureBandwidth(ByteUtils
                .long2int(getConnectionBandwidthStatistics().getBytesReceived()));
    }

    public float getMeasuredUpstreamBandwidth() {
        float retValue = 0; // initialize to default
        try {
            retValue = _upBandwidthTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    public float getMeasuredDownstreamBandwidth() {
        float retValue = 0;
        try {
            retValue = _downBandwidthTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    public long getNextQRPForwardTime() {
        return _nextQRPForwardTime;
    }

    public void incrementNextQRPForwardTime(long curTime) {
        if (isLeafConnection()) {
            _nextQRPForwardTime = curTime + LEAF_QUERY_ROUTE_UPDATE_TIME;
        } else {
            // otherwise, it's an Ultrapeer
            _nextQRPForwardTime = curTime + ULTRAPEER_QUERY_ROUTE_UPDATE_TIME;
        }
    }

    public boolean isKillable() {
        return _isKillable;
    }

    public QueryRouteTable getQueryRouteTableSent() {
        return _lastQRPTableSent;
    }

    public void setQueryRouteTableSent(QueryRouteTable qrt) {
        _lastQRPTableSent = qrt;
    }

    public boolean isMyPushProxy() {
        return myPushProxy;
    }

    public boolean isPushProxyFor() {
        return pushProxyFor;
    }

    public void setPushProxyFor(boolean pushProxyFor) {
        this.pushProxyFor = pushProxyFor;
    }

    public Object getQRPLock() {
        return QRP_LOCK;
    }

    public void setLocalePreferencing(boolean b) {
        _useLocalPreference = b;
    }

    public void reply(Message m) {
        send(m);
    }

    /**
     * A ConnectObserver that continues the handshaking process in the same
     * thread, expecting that performHandshake(...) callback to the observer.
     */
    private class AsyncHandshakeConnecter implements ConnectObserver {

        private Properties requestHeaders;

        private HandshakeResponder responder;

        private GnetConnectObserver observer;

        AsyncHandshakeConnecter(Properties requestHeaders, HandshakeResponder responder,
                GnetConnectObserver observer) {
            this.requestHeaders = requestHeaders;
            this.responder = responder;
            this.observer = observer;
        }

        public void handleConnect(Socket s) throws IOException {
            // _socket may not really have been set yet, this ensures it is.
            setSocket(s);
            if(LOG.isInfoEnabled()) {
                LOG.info("Connected to " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
            startHandshake(requestHeaders, responder, observer);
        }

        public void shutdown() {
            if(LOG.isInfoEnabled()) {
                LOG.info("Shutting down connection to " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
            observer.shutdown();
        }

        // ignored.
        public void handleIOException(IOException iox) {
            if(LOG.isInfoEnabled()) {
                LOG.info(iox + ", " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
        }
    }

    /**
     * A HandshakeObserver that notifies the GnetConnectObserver when
     * handshaking finishes.
     */
    private class HandshakeWatcher implements HandshakeObserver {

        private Handshaker shaker;

        private GnetConnectObserver observer;

        HandshakeWatcher(GnetConnectObserver observer) {
            this.observer = observer;
        }

        void setHandshaker(Handshaker shaker) {
            this.shaker = shaker;
        }

        public void shutdown() {
            if(LOG.isInfoEnabled()) {
                LOG.info("Shutting down connection to " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
            setHeaders(shaker.getReadHeaders(), shaker.getWrittenHeaders());
            close();
            observer.shutdown();
        }

        public void handleHandshakeFinished(Handshaker shaker) {
            if(LOG.isInfoEnabled()) {
                LOG.info("Finished handshake with " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
            postHandshakeInitialize(shaker);
            observer.handleConnect();
        }

        public void handleBadHandshake() {
            if(LOG.isInfoEnabled()) {
                LOG.info("Bad handshake with " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
            setHeaders(shaker.getReadHeaders(), shaker.getWrittenHeaders());
            close();
            observer.handleBadHandshake();
        }

        public void handleNoGnutellaOk(int code, String msg) {
            if(LOG.isInfoEnabled()) {
                LOG.info("No Gnutella OK in handshake with " +
                        getAddress() + ":" + getPort() +
                        ", TLS " + SSLUtils.isTLSEnabled(socket));
            }
            setHeaders(shaker.getReadHeaders(), shaker.getWrittenHeaders());
            close();
            observer.handleNoGnutellaOk(code, msg);
        }
    }

    public ConnectionRoutingStatistics getRoutedConnectionStatistics() {
        return this;
    }

    // all ReplyHandler things, to override, override from
    // ConnectionCapabilities...

    final public boolean isGoodLeaf() {
        return getConnectionCapabilities().isGoodLeaf();
    }

    final public boolean isGoodUltrapeer() {
        return getConnectionCapabilities().isGoodUltrapeer();
    }

    final public boolean isHighDegreeConnection() {
        return getConnectionCapabilities().isHighDegreeConnection();
    }

    final public boolean isLeafConnection() {
        return getConnectionCapabilities().isLeafConnection();
    }

    final public boolean isSupernodeClientConnection() {
        return getConnectionCapabilities().isSupernodeClientConnection();
    }

    final public boolean isUltrapeerQueryRoutingConnection() {
        return getConnectionCapabilities().isUltrapeerQueryRoutingConnection();
    }

    final public boolean supportsPongCaching() {
        return getConnectionCapabilities().supportsPongCaching();
    }

    final public ConnectionMessageStatistics getConnectionMessageStatistics() {
        return this;
    }
}
