package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.HTTP;
import org.limewire.collection.BitNumbers;
import org.limewire.collection.Function;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.channel.ThrottleReader;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.nio.statemachine.IOState;
import org.limewire.nio.statemachine.IOStateMachine;
import org.limewire.nio.statemachine.IOStateObserver;
import org.limewire.nio.statemachine.ReadSkipState;
import org.limewire.nio.statemachine.ReadState;
import org.limewire.rudp.RUDPSocket;
import org.limewire.util.OSUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.AssertFailure;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocUtils;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.SimpleReadHeaderState;
import com.limegroup.gnutella.http.SimpleWriteHeaderState;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics.StatisticType;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.ThexReader;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;

/**
 * Downloads a file over an HTTP connection. This class is as simple as
 * possible. It does not deal with retries, prioritizing hosts, etc. Nor does it
 * check whether a file already exists; it just writes over anything on disk.
 * <p>
 * 
 * It is necessary to explicitly initialize an HTTPDownloader with the
 * connectTCP(..) followed by a connectHTTP(..) method. (Hence HTTPDownloader
 * behaves much like Connection.) Typical use is as follows:
 * 
 * <pre>
 * HTTPDownloader dl = new HTTPDownloader(host, port);
 * dl.connectTCP(timeout);
 * dl.connectHTTP(startByte, stopByte);
 * dl.doDownload();
 * </pre>
 * 
 * LOCKING: _writtenGoodLocs and _goodLocs are both synchronized on _goodLocs
 * <p>
 * LOCKING: _writtenBadLocs and _badLocs are both synchronized on _badLocs
 */

public class HTTPDownloader implements BandwidthTracker {

    private static final Log LOG = LogFactory.getLog(HTTPDownloader.class);

    /**
     * The length of the buffer used in downloading.
     */
    public static final int BUF_LENGTH = 2048;

    /**
     * The smallest possible time in seconds to wait before retrying a busy
     * host.
     */
    private static final int MIN_RETRY_AFTER = 60; // 60 seconds

    /**
     * The maximum possible time in seconds to wait before retrying a busy host.
     */
    private static final int MAX_RETRY_AFTER = 60 * 60; // 1 hour

    /**
     * The smallest possible file to be shared with partial file sharing. Non
     * final for testing purposes.
     */
    static volatile int MIN_PARTIAL_FILE_BYTES = 1 * 1024 * 1024; // 1MB

    private final RemoteFileDesc _rfd;

    private final RemoteFileDescContext rfdContext;

    private long _index;

    private String _filename;

    private byte[] _guid;

    /**
     * The total amount we've downloaded, including all previous HTTP
     * connections.
     * <p>
     * LOCKING: this
     */
    private long _totalAmountRead;

    /**
     * The amount we've downloaded.
     * <p>
     * LOCKING: this
     */
    private long _amountRead;

    /**
     * The amount we'll have downloaded if the download completes properly. Note
     * that the amount still left to download is _amountToRead - _amountRead.
     * <p>
     * LOCKING: this
     */
    private long _amountToRead;

    /**
     * Whether to disconnect after reading the amount we have wanted to read.
     */
    private volatile boolean _disconnect;

    /**
     * The index to start reading from the server.
     * <p>
     * LOCKING: this
     */
    private long _initialReadingPoint;

    /**
     * The index to actually start writing to the file. LOCKING:this
     */
    private long _initialWritingPoint;

    /**
     * The content-length of the output, useful only for when we want to read &
     * discard the body of the HTTP message.
     */
    private long _contentLength;

    /**
     * Whether or not the body has been consumed.
     */
    private volatile boolean _bodyConsumed = true;

    private Socket _socket; // initialized in HTTPDownloader(Socket) or connect

    private IOStateMachine _stateMachine;

    private Observer observerHandler;

    private SimpleReadHeaderState _headerReader;

    private boolean _requestingThex;

    private ThexReader _thexReader;

    private final VerifyingFile _incompleteFile;

    /**
     * The new alternate locations we've received for this file.
     */
    private Set<RemoteFileDesc> _locationsReceived;

    /**
     * The good locations to send the uploaders as in the alts list.
     */
    private Set<DirectAltLoc> _goodLocs;

    /**
     * The firewalled locations to send to uploaders that are interested.
     */
    private Set<PushAltLoc> _goodPushLocs;

    /**
     * The bad firewalled locations to send to uploaders that are interested.
     */
    private Set<PushAltLoc> _badPushLocs;

    /**
     * The list to send in the n-alts list.
     */
    private Set<DirectAltLoc> _badLocs;

    /**
     * The list of already written alts, used to stop duplicates.
     */
    private Set<DirectAltLoc> _writtenGoodLocs;

    /**
     * The list of already written n-alts, used to stop duplicates.
     */
    private Set<DirectAltLoc> _writtenBadLocs;

    /**
     * The list of already written push alts, used to stop duplicates.
     */
    private Set<PushAltLoc> _writtenPushLocs;

    /**
     * The list of already written bad push alts, used to stop duplicates.
     */
    private Set<PushAltLoc> _writtenBadPushLocs;

    private boolean _browseEnabled = false; // also for now

    private volatile String _server = "";

    private String _thexUri = null;

    private String _root32 = null;

    /**
     * Whether or not the retrieval of THEX succeeded. This is stored here, as
     * opposed to the RemoteFileDesc, because we may want to re-use the
     * RemoteFileDesc to try and get the THEX tree later on from this host, if
     * the first attempt failed from corruption.
     * <p>
     * Failures are stored in the RemoteFileDesc because if it failed we never
     * want to try it again, ever.
     */
    private boolean _thexSucceeded = false;

    /** For implementing the BandwidthTracker interface. */
    private BandwidthTrackerImpl bandwidthTracker = new BandwidthTrackerImpl();

    /**
     * Whether or not this HTTPDownloader is currently attempting to read
     * information from the network.
     */
    private boolean _isActive = false;

    private Range _requestedInterval = null;
    
    /**
     * Whether the other side wants to receive firewalled altlocs.
     */
    private boolean _wantsFalts = false;

    /** Whether to count the bandwidth used by this downloader. */
    private final boolean _inNetwork;

    private final NetworkManager networkManager;

    private final AlternateLocationFactory alternateLocationFactory;

    private final DownloadManager downloadManager;

    private final CreationTimeCache creationTimeCache;

    private final BandwidthManager bandwidthManager;

    private final Provider<PushEndpointCache> pushEndpointCache;

    private final PushEndpointFactory pushEndpointFactory;

    private final RemoteFileDescFactory remoteFileDescFactory;

    private final ThexReaderFactory thexReaderFactory;

    private final TcpBandwidthStatistics tcpBandwidthStatistics;

    private final NetworkInstanceUtils networkInstanceUtils;

    HTTPDownloader(Socket socket, RemoteFileDescContext rfdContext, VerifyingFile incompleteFile,
            boolean inNetwork, boolean requireSocket, NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory, DownloadManager downloadManager,
            CreationTimeCache creationTimeCache, BandwidthManager bandwidthManager,
            Provider<PushEndpointCache> pushEndpointCache, PushEndpointFactory pushEndpointFactory,
            RemoteFileDescFactory remoteFileDescFactory, ThexReaderFactory thexReaderFactory,
            TcpBandwidthStatistics tcpBandwidthStatistics, NetworkInstanceUtils networkInstanceUtils) {

        if (requireSocket && socket == null)
            throw new NullPointerException("null socket");

        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadManager = downloadManager;
        this.creationTimeCache = creationTimeCache;
        this.bandwidthManager = bandwidthManager;
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFactory = pushEndpointFactory;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.thexReaderFactory = thexReaderFactory;
        this.tcpBandwidthStatistics = tcpBandwidthStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
        this.rfdContext = rfdContext;
        _rfd = rfdContext.getRemoteFileDesc();
        _socket = socket;
        _incompleteFile = incompleteFile;
        _filename = _rfd.getFileName();
        _index = _rfd.getIndex();
        _guid = _rfd.getClientGUID();
        _amountToRead = 0;
        _browseEnabled = _rfd.isBrowseHostEnabled();
        _locationsReceived = new HashSet<RemoteFileDesc>();
        _goodLocs = new HashSet<DirectAltLoc>();
        _badLocs = new HashSet<DirectAltLoc>();
        _goodPushLocs = new HashSet<PushAltLoc>();
        _badPushLocs = new HashSet<PushAltLoc>();
        _writtenGoodLocs = new HashSet<DirectAltLoc>();
        _writtenBadLocs = new HashSet<DirectAltLoc>();
        _writtenPushLocs = new HashSet<PushAltLoc>();
        _writtenBadPushLocs = new HashSet<PushAltLoc>();
        _amountRead = 0;
        _totalAmountRead = 0;
        _inNetwork = inNetwork;
    }

    // //////////////////////Alt Locs methods////////////////////////

    /**
     * Accessor for the alternate locations received from the server for this
     * download attempt.
     * 
     * @return the <tt>AlternateLocationCollection</tt> containing the received
     *         locations, can be <tt>null</tt> if we could not create a
     *         collection, or could be empty
     */
    Collection<RemoteFileDesc> getLocationsReceived() {
        return _locationsReceived;
    }

    /**
     * Stores the location in the specific sets. Both 'remove' sets have the
     * location removed while the lock is held on removeWithLock, and addTo is
     * added if it isn't within contains (while the lock is held on addTo.
     */
    private static <T extends AlternateLocation> void storeLocation(T loc, Set<T> removeWithLock,
            Set<T> removeAlso, Set<T> addTo, Set<T> contains) {
        synchronized (removeWithLock) {
            removeAlso.remove(loc);
            removeWithLock.remove(loc);
        }

        synchronized (addTo) {
            if (!contains.contains(loc))
                addTo.add(loc);
        }
    }

    /** Stores the location as a success, for writing as an X-Alt or X-FAlt. */
    void addSuccessfulAltLoc(AlternateLocation loc) {
        if (loc instanceof DirectAltLoc) {
            DirectAltLoc direct = (DirectAltLoc) loc;
            storeLocation(direct, _badLocs, _writtenBadLocs, _goodLocs, _writtenGoodLocs);
        } else if (loc instanceof PushAltLoc) {
            PushAltLoc push = (PushAltLoc) loc;
            storeLocation(push, _badPushLocs, _writtenBadPushLocs, _goodPushLocs, _writtenPushLocs);
        } else {
            throw new IllegalStateException("bad location of class: " + loc.getClass());
        }
    }

    /**
     * Stores the location as a failed alternate location, for writing as X-NAlt
     * or X-NFAlt.
     */
    void addFailedAltLoc(AlternateLocation loc) {
        if (loc instanceof DirectAltLoc) {
            DirectAltLoc direct = (DirectAltLoc) loc;
            storeLocation(direct, _goodLocs, _writtenGoodLocs, _badLocs, _writtenBadLocs);
        } else if (loc instanceof PushAltLoc) {
            PushAltLoc push = (PushAltLoc) loc;
            storeLocation(push, _goodPushLocs, _writtenPushLocs, _badPushLocs, _writtenBadPushLocs);
        } else {
            throw new IllegalStateException("bad location of class: " + loc.getClass());
        }
    }

    // /////////////////////////////// Connection /////////////////////////////

    /**
     * Initializes the TCP connection by installing readers & writers on the
     * socket and setting the appropriate keepAlive & soTimeout socket options.
     * <p>
     * If the TCP connection could not be initialized, this throws an
     * IOException.
     */
    public void initializeTCP() throws IOException {
        if (_socket == null)
            throw new IllegalStateException("no socket!");

        try {
            _socket.setKeepAlive(true);
        } catch (IOException iox) {
            if (!OSUtils.isWindowsVista() && !OSUtils.isWindows7())
                throw iox;
            LOG.warn("couldn't set keepalive");
        }
        observerHandler = new Observer();
        ((AbstractNBSocket)_socket).setReadThrottleChannel(new ThrottleReader(bandwidthManager.getReadThrottle()));
        _stateMachine = new IOStateMachine(observerHandler, new LinkedList<IOState>(), BUF_LENGTH);
        ((NIOMultiplexor) _socket).setReadObserver(_stateMachine);
        ((NIOMultiplexor) _socket).setWriteObserver(_stateMachine);

        // Note : once we have established the TCP connection with the host we
        // want to download from we set the soTimeout. Its reset in doDownload
        // Note2 : this may throw an IOException.
        _socket.setSoTimeout(Constants.TIMEOUT);
    }

    /**
     * Same as connectHTTP(start, stop, supportQueueing, -1).
     */
    public void connectHTTP(long start, long stop, boolean supportQueueing, IOStateObserver observer) {
        connectHTTP(start, stop, supportQueueing, -1, observer);
    }
    
    boolean isEncrypted() {
        return SSLUtils.isTLSEnabled(_socket);
    }

    /**
     * Sends a GET request using an already open socket, and reads all headers.
     * The actual ranges downloaded MAY NOT be the same as the 'start' and
     * 'stop' parameters, as HTTP allows the server to respond with any
     * satisfiable subrange of the request.
     * <p>
     * Users of this class should examine getInitialReadingPoint() and
     * getAmountToRead() to determine what the effective start & stop ranges
     * are, and update external data structures appropriately.
     * <p>
     * 
     * <pre>
     * int newStart = dloader.getInitialReadingPoint();
     * 
     * int newStop = (dloader.getAmountToRead() - 1) + newStart; // INCLUSIVE
     * </pre>
     * 
     * or
     * 
     * <pre>
     * int newStop = dloader.getAmountToRead() + newStart; // EXCLUSIVE
     * </pre>
     * <p>
     * 
     * @param start the byte at which the HTTPDownloader should begin
     * @param stop the index just past the last byte to read; stop-1 is the last
     *        byte the HTTPDownloader should download
     *        <p>
     * @exception TryAgainLaterException the host is busy
     * @exception FileNotFoundException the host doesn't recognize the file
     * @exception NotSharingException the host isn't sharing files (BearShare)
     * @exception IOException miscellaneous error
     * @exception QueuedException uploader has queued us
     * @exception RangeNotAvailableException uploader has ranges other than
     *            requested
     * @exception ProblemReadingHeaderException could not parse headers
     * @exception UnknownCodeException unknown response code
     */
    public void connectHTTP(long start, long stop, boolean supportQueueing, long amountDownloaded,
            IOStateObserver observer) {
        if (start < 0)
            throw new IllegalArgumentException("invalid start: " + start);
        if (stop <= start)
            throw new IllegalArgumentException("stop(" + stop + ") <= start(" + start + ")");
        
        rfdContext.setLastHttpCode(-1);

        synchronized (this) {
            _isActive = true;
            _amountToRead = stop - start;
            _amountRead = 0;
            _initialReadingPoint = start;
            _initialWritingPoint = start;
            _bodyConsumed = false;
            _contentLength = 0;
            _requestedInterval = Range.createRange(_initialReadingPoint, stop - 1);
        }

        observerHandler.setDelegate(observer);

        List<Header> headers = new ArrayList<Header>();
        Set<HTTPHeaderValue> features = new HashSet<HTTPHeaderValue>();

        headers.add(HTTPHeaderName.HOST.create(getHostAddress()));
        headers.add(HTTPHeaderName.USER_AGENT.create(ConstantHTTPHeaderValue.USER_AGENT));

        if (supportQueueing) {
            headers.add(HTTPHeaderName.QUEUE.create(ConstantHTTPHeaderValue.QUEUE_VERSION));
            features.add(ConstantHTTPHeaderValue.QUEUE_FEATURE);
        }

        // if I'm not firewalled or I can do FWT, say that I want pushlocs.
        // if I am firewalled, send the version of the FWT protocol I support.
        // (which implies that I want only altlocs that support FWT)
        if (networkManager.acceptedIncomingConnection() || networkManager.canDoFWT()) {
            features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
            if (!networkManager.acceptedIncomingConnection())
                features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        }

        // Add ourselves to the mesh if the partial file is valid
        // if I'm firewalled add myself only if the other guy wants falts
        if (isPartialFileValid() && (networkManager.acceptedIncomingConnection() || _wantsFalts)) {
            AlternateLocation me = alternateLocationFactory.create(_rfd.getSHA1Urn());
            if (me != null)
                addSuccessfulAltLoc(me);
        }

        URN sha1 = _rfd.getSHA1Urn();
        if (sha1 != null)
            headers.add(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(sha1));

        writeAlternateLocations(headers, HTTPHeaderName.ALT_LOCATION, _goodLocs, _writtenGoodLocs,
                true);
        writeAlternateLocations(headers, HTTPHeaderName.NALTS, _badLocs, _writtenBadLocs, false);

        // if the other side indicated they want firewalled altlocs, send some
        //
        // Note: we send both types of firewalled altlocs to the uploader since
        // even if
        // it can't support FWT it can still spread them to other downloaders.
        //
        // Note2: we can't know whether the other side wants to receive pushlocs
        // until
        // we read their headers. Therefore pushlocs will be sent from the
        // second
        // http request on.
        if (_wantsFalts) {
            writeAlternateLocations(headers, HTTPHeaderName.FALT_LOCATION, _goodPushLocs,
                    _writtenPushLocs, false);
            writeAlternateLocations(headers, HTTPHeaderName.BFALT_LOCATION, _badPushLocs,
                    _writtenBadPushLocs, false);
        }

        headers
                .add(HTTPHeaderName.RANGE
                        .create("bytes=" + _initialReadingPoint + "-" + (stop - 1)));

        if (networkManager.acceptedIncomingConnection()
                && !networkInstanceUtils.isPrivateAddress(networkManager.getAddress())) {
            int port = networkManager.getPort();
            String host = NetworkUtils.ip2string(networkManager.getAddress());
            headers.add(HTTPHeaderName.NODE.create(host + ":" + port));
            features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
            // // Legacy chat header. Replaced by X-Features header / X-Node
            // // header
            // if (ChatSettings.CHAT_ENABLED.getValue()) {
            // headers.add(HTTPHeaderName.CHAT.create(host + ":" + port));
            // features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
            // }
        }

        // if this node is firewalled, send its push proxy info and guid so
        // the uploader can connect back for browses and such
        if (!networkManager.acceptedIncomingConnection()) {
            headers.add(HTTPHeaderName.FW_NODE_INFO.create(pushEndpointFactory.createForSelf()));
            features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
        }

        // Write X-Features header.
        if (features.size() > 0)
            headers.add(HTTPHeaderName.FEATURES.create(new HTTPHeaderValueCollection(features)));

        // Write X-Downloaded header to inform uploader about
        // how many bytes already transferred for this file
        if (amountDownloaded > 0) {
            headers.add(HTTPHeaderName.DOWNLOADED.create("" + amountDownloaded));
        }

        Credentials credentials = rfdContext.getCredentials();
        if (credentials != null) {
            headers.add(createBasicAuthHeader(credentials));
        }

        SimpleWriteHeaderState writer = new SimpleWriteHeaderState(
                "GET " + _rfd.getUrlPath() + " HTTP/1.1", headers);
        SimpleReadHeaderState reader = new SimpleReadHeaderState(
                DownloadSettings.MAX_HEADERS.getValue(),
                DownloadSettings.MAX_HEADER_SIZE.getValue());

        _stateMachine.addStates(new IOState[] { writer, reader });
        _headerReader = reader;
    }

    Header createBasicAuthHeader(Credentials credentials) {
        return BasicScheme.authenticate(credentials, HTTP.DEFAULT_PROTOCOL_CHARSET, false);
    }
    
    String getServer() {
        return _server;
    }

    private String getHostAddress() {
        Address address = rfdContext.getAddress();
        // if we're connecting to a connectable, use the unresolved address
        // as host name, to allow virtual hosts to work which don't know 
        // what to do with the resolved ip address from the socket, this mainly
        // addresses magnet downloads from webservers
        if (address instanceof Connectable) {
            Connectable connectable = (Connectable)address;
            return connectable.getAddress() + ":" + connectable.getPort();
        }
        Socket socket = _socket;
        return socket != null ? socket.getInetAddress().getHostAddress() + ":" + socket.getPort()
                : "unknown host";
    }

    /**
     * Adds some locations to the set of locations we'll write, and stores them
     * in the already-written set.
     */
    private <T extends AlternateLocation> void writeAlternateLocations(List<Header> headers,
            HTTPHeaderName header, Set<T> locs, Set<T> stored, boolean includeTLS) {
        // We don't want to hold locks while doing network operations, so we use
        // this variable to clone the location before writing to the network
        List<HTTPHeaderValue> valuesToWrite = null;
        BitNumbers bn = null;
        synchronized (locs) {
            if (locs.size() > 0) {
                valuesToWrite = new ArrayList<HTTPHeaderValue>(locs.size());
                if (includeTLS)
                    bn = new BitNumbers(locs.size());
                for (T loc : locs) {
                    // we should not have empty proxies unless this is ourselves
                    if (loc instanceof PushAltLoc) {
                        PushAltLoc pushLoc = (PushAltLoc) loc;
                        if (pushLoc.getPushAddress().getProxies().isEmpty()) {
                            if (pushLoc.getPushAddress().isLocal())
                                continue;
                            else
                                assert false : "empty pushloc in downloader";
                        }
                    } else if (loc instanceof DirectAltLoc) {
                        IpPort host = ((DirectAltLoc) loc).getHost();
                        if (includeTLS && host instanceof Connectable
                                && ((Connectable) host).isTLSCapable()) {
                            assert bn != null;
                            bn.set(valuesToWrite.size());
                        }
                    }
                    valuesToWrite.add(loc);
                    stored.add(loc);
                }
                locs.clear();
            }
        }

        if (valuesToWrite != null) {
            if (bn != null && !bn.isEmpty()) {
                final String hex = bn.toHexString();
                valuesToWrite.add(0, new HTTPHeaderValue() {
                    public String httpStringValue() {
                        return DirectAltLoc.TLS_IDX + hex;
                    }
                });
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("writing alts: "
                        + header.create(new HTTPHeaderValueCollection(valuesToWrite)));
            }

            headers.add(header.create(new HTTPHeaderValueCollection(valuesToWrite)));
        }
    }

    /**
     * Consumes the body of the HTTP message that was previously exchanged, if
     * necessary.
     */
    void consumeBody(IOStateObserver observer) {
        if (!_bodyConsumed) {
            if (_contentLength != -1)
                consumeBody(_contentLength, observer);
            else
                observer.handleIOException(new IOException("no content length"));
        } else {
            observer.handleStatesFinished();
        }
        _bodyConsumed = true;
    }

    /** Determines if the body needs to be consumed. */
    boolean isBodyConsumed() {
        return _bodyConsumed;
    }

    /**
     * Returns the ConnectionStatus from the request. Can be one of: Connected
     * -- means to immediately assignAndRequest. Queued -- means to sleep while
     * queued. ThexResponse -- means the thex tree was received.
     */
    public void requestHashTree(URN sha1, IOStateObserver observer) {
        if (LOG.isDebugEnabled())
            LOG.debug("requesting HashTree for " + _thexUri + " from " + _rfd.getAddress());
        
        rfdContext.setLastHttpCode(-1);

        observerHandler.setDelegate(observer);

        List<Header> headers = new ArrayList<Header>();
        headers.add(HTTPHeaderName.HOST.create(getHostAddress()));
        headers.add(HTTPHeaderName.USER_AGENT.create(ConstantHTTPHeaderValue.USER_AGENT));
        Credentials credentials = rfdContext.getCredentials();
        if (credentials != null) {
            headers.add(createBasicAuthHeader(credentials));
        }
        
        SimpleWriteHeaderState writer = new SimpleWriteHeaderState(
                "GET " + _thexUri + " HTTP/1.1", headers);
        SimpleReadHeaderState reader = new SimpleReadHeaderState(
                DownloadSettings.MAX_HEADERS.getValue(),
                DownloadSettings.MAX_HEADER_SIZE.getValue());

        _headerReader = reader;
        _requestingThex = true;
        _bodyConsumed = false;
        _stateMachine.addStates(new IOState[] { writer, reader });
    }

    boolean isRequestingThex() {
        return _requestingThex;
    }

    public ConnectionStatus parseThexResponseHeaders() {
        _requestingThex = false;
        try {
            int code = parseHTTPCode(_headerReader.getConnectLine(), _rfd);
            rfdContext.setLastHttpCode(code);
            boolean failed = false;
            if (code < 200 || code >= 300)
                failed = true;
            return parseThexHeaders(code, failed);
        } catch (IOException failed) {
            return ConnectionStatus.getNoFile();
        }

    }

    public void downloadThexBody(URN sha1, IOStateObserver observer) {
        _thexReader = thexReaderFactory.createHashTreeReader(sha1.httpStringValue(), _root32, _rfd
                .getSize());
        observerHandler.setDelegate(observer);
        _stateMachine.addState(_thexReader);
    }

    public HashTree getHashTree() {
        // LOG.debug("Retrieving hash tree, expected length: " + _contentLength
        // + ", read: " + _thexReader.getAmountProcessed());
        _contentLength -= _thexReader.getAmountProcessed();
        if (_contentLength == 0)
            _bodyConsumed = true;
        HashTree tree = null;
        try {
            tree = _thexReader.getHashTree();
        } catch (IOException iox) {
            LOG.warn("Failed to create tree", iox);
        }
        if (tree == null)
            rfdContext.setTHEXFailed();
        else
            _thexSucceeded = true;

        return tree;
    }

    /**
     * Parses the headers of a thex response. Ensures a content-length is
     * included, and if queued returns a queued response.
     */
    private ConnectionStatus parseThexHeaders(int code, boolean failed) {
        if (LOG.isDebugEnabled())
            LOG.debug(_rfd + " consuming headers");

        _contentLength = -1;
        for (Map.Entry<String, String> entry : _headerReader.getHeaders().entrySet()) {
            String header = entry.getKey();
            if (HTTPHeaderName.CONTENT_LENGTH.is(header))
                _contentLength = readContentLength(entry.getValue());
            if (code == 503 && HTTPHeaderName.QUEUE.is(header)) {
                String value = entry.getValue();
                int queueInfo[] = { -1, -1, -1 };
                parseQueueHeaders(value, queueInfo);
                int min = queueInfo[0];
                int max = queueInfo[1];
                int pos = queueInfo[2];
                if (min != -1 && max != -1 && pos != -1) {
                    _bodyConsumed = true;
                    return ConnectionStatus.getQueued(pos, min);
                }
            }
        }

        if (_contentLength == 0)
            _bodyConsumed = true;

        if (failed || _contentLength == -1)
            return ConnectionStatus.getNoFile();
        else
            return ConnectionStatus.getConnected();
    }

    /**
     * Consumes the body portion of an HTTP Message.
     */
    private void consumeBody(long contentLength, IOStateObserver observer) {
        if (LOG.isTraceEnabled())
            LOG.trace("enter consumeBody(" + contentLength + ")");

        if (contentLength < 0)
            observer.handleIOException(new IOException("unknown content-length, can't consume"));

        observerHandler.setDelegate(observer);
        _stateMachine.addState(new ReadSkipState(contentLength));
    }

    /*
     * Reads the headers from this, setting _initialReadingPoint and
     * _amountToRead. Throws any of the exceptions listed in connect().
     */
    public void parseHeaders() throws IOException {
        String connectLine = _headerReader.getConnectLine();
        Map<String, String> headers = _headerReader.getHeaders();

        if (connectLine == null || connectLine.equals(""))
            throw new IOException();

        int code = parseHTTPCode(connectLine, _rfd);
        rfdContext.setLastHttpCode(code);
        _contentLength = -1;
        // Note: According to the specification there are 5 headers, LimeWire
        // ignores 2 of them - queue length, and maxUploadSlots.
        int[] refQueueInfo = { -1, -1, -1 };
        // Now read each header...
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String header = entry.getKey();
            String value = entry.getValue();

            // For "Content-Range" headers, we store what the remote side is
            // going to give us. Users should examine the interval and
            // update external structures appropriately.
            if (HTTPHeaderName.CONTENT_RANGE.is(header))
                validateContentRange(parseContentRange(value));
            else if (HTTPHeaderName.CONTENT_LENGTH.is(header))
                _contentLength = readContentLength(value);
            else if (HTTPHeaderName.CONTENT_URN.is(header))
                checkContentUrnHeader(value, _rfd.getSHA1Urn());
            else if (HTTPHeaderName.GNUTELLA_CONTENT_URN.is(header))
                checkContentUrnHeader(value, _rfd.getSHA1Urn());
            else if (HTTPHeaderName.ALT_LOCATION.is(header))
                readAlternateLocations(value, true);
            else if (HTTPHeaderName.QUEUE.is(header))
                parseQueueHeaders(value, refQueueInfo);
            else if (HTTPHeaderName.SERVER.is(header)) {
                _server = value;
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Server is: " + value);
                }
            } else if (HTTPHeaderName.AVAILABLE_RANGES.is(header))
                parseAvailableRangesHeader(value, _rfd);
            else if (HTTPHeaderName.RETRY_AFTER.is(header))
                parseRetryAfterHeader(value, rfdContext);
            else if (HTTPHeaderName.CREATION_TIME.is(header))
                parseCreationTimeHeader(value, _rfd);
            else if (HTTPHeaderName.FEATURES.is(header))
                parseFeatureHeader(value);
            else if (HTTPHeaderName.THEX_URI.is(header))
                parseTHEXHeader(value);
            else if (HTTPHeaderName.FALT_LOCATION.is(header))
                parseFALTHeader(value);
            else if (HTTPHeaderName.PROXIES.is(header))
                parseProxiesHeader(value);
            else if (HTTPHeaderName.FWTPORT.is(header))
                parseFWTPortHeader(value);
        }

        // Accept any 2xx's, but reject other codes.
        if ((code < 200) || (code >= 300)) {
            if (code == 404) // file not found
                throw new com.limegroup.gnutella.downloader.FileNotFoundException();
            else if (code == 410) // not shared.
                throw new NotSharingException();
            else if (code == 416) {// requested range not available
                // See if the uploader is up to mischief
                if (rfdContext.isPartialSource()) {
                    for (Range next : rfdContext.getAvailableRanges()) {
                        if (_requestedInterval.isSubrange(next))
                            throw new ProblemReadingHeaderException("Bad ranges sent");
                    }
                } else {// Uploader sent 416 and no ranges
                    throw new ProblemReadingHeaderException("no ranges sent");
                }
                // OK. The uploader is not messing with us.
                throw new RangeNotAvailableException();
            } else if (code == 503) { // busy or queued, or range not available.
                int min = refQueueInfo[0];
                int max = refQueueInfo[1];
                int pos = refQueueInfo[2];
                if (min != -1 && max != -1 && pos != -1) {
                    _bodyConsumed = true;
                    throw new QueuedException(min, max, pos);
                }

                // per the PFSP spec, a 503 should be returned. But if the
                // uploader returns a "Avaliable-Ranges" header regardless of
                // whether it is really busy or just does not have the requested
                // range, we cannot really distinguish between the two cases on
                // the client side.

                // For the most part clients send 416 when they have other
                // ranges
                // that may match the clients need. From LimeWire 4.0.6 onwards
                // LimeWire will treat 503s to mean either busy or queued BUT
                // NOT partial range available.

                // if( _rfd.isPartialSource() )
                // throw new RangeNotAvailableException();

                // no QueuedException or RangeNotAvailableException? not queued.
                // throw a generic busy exception.
                throw new TryAgainLaterException();

                // a general catch for 4xx and 5xx's
                // should maybe be a different exception?
                // else if ( (code >= 400) && (code < 600) )
            } else
                // unknown or unimportant
                throw new UnknownCodeException(code);
        }
    }

    /**
     * Does nothing except for throwing an exception if the
     * X-Gnutella-Content-URN header does not match.
     * 
     * @param value the header <tt>String</tt>
     * @param sha1 the <tt>URN</tt> we expect
     * @throws ContentUrnMismatchException
     */
    private void checkContentUrnHeader(String value, URN sha1) throws ContentUrnMismatchException {
        if (_root32 == null && value.indexOf("urn:bitprint:") > -1) {
            // If the root32 was not in the X-Thex-URI header
            // (the spec requires it be there), then steal it from
            // the content-urn if it was a bitprint.
            _root32 = value.substring(value.lastIndexOf(".") + 1).trim();
        }

        if (sha1 == null)
            return;

        URN contentUrn = null;
        try {
            contentUrn = URN.createSHA1Urn(value);
        } catch (IOException ioe) {
            // could be an URN type we don't know. So ignore all
            return;
        }
        if (!sha1.equals(contentUrn))
            throw new ContentUrnMismatchException();
        // else do nothing at all.
    }

    /**
     * Reads alternate location header. The header can contain only one
     * alternate location, or it can contain many in the same header. This
     * method adds them all to the <tt>FileDesc</tt> for this uploader. This
     * will not allow more than 20 alternate locations for a single file.
     * <p>
     * Since uploaders send only good alternate locations, we add merge proxies
     * to the existing sets.
     */
    private void readAlternateLocations(String altStr, boolean allowTLS) {
        AltLocUtils.parseAlternateLocations(_rfd.getSHA1Urn(), altStr, allowTLS,
                alternateLocationFactory, new Function<AlternateLocation, Void>() {
                    public Void apply(AlternateLocation location) {
                        RemoteFileDesc rfd = location.createRemoteFileDesc(_rfd.getSize(),
                                remoteFileDescFactory);
                        _locationsReceived.add(rfd);
                        return null;
                    }
                });
    }

    /**
     * Determines whether or not the partial file is valid for us to add
     * ourselves to the mesh.
     * <p>
     * Checks the following: - RFD has a SHA1. - We are allowing partial sharing
     * - We have successfully verified at least certain size of the file - Our
     * port and IP address are valid
     */
    private boolean isPartialFileValid() {
        return _rfd.getSHA1Urn() != null
                && _incompleteFile.getVerifiedBlockSize() > MIN_PARTIAL_FILE_BYTES
                && SharingSettings.ALLOW_PARTIAL_SHARING.getValue()
                && NetworkUtils.isValidPort(networkManager.getPort())
                && NetworkUtils.isValidAddress(networkManager.getAddress());
    }

    /**
     * Reads the Content-Length. Invalid Content-Lengths are set to 0.
     */
    public static long readContentLength(final String value) {
        if (value == null)
            return 0;
        else {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
    }

    /**
     * Returns the HTTP response code from the given string, throwing an
     * exception if it couldn't be parsed.
     * 
     * @param str an HTTP response string, e.g., "HTTP/1.1 200 OK \r\n"
     * @exception NoHTTPOKException str didn't contain "HTTP"
     * @exception ProblemReadingHeaderException some other problem extracting
     *            result code
     */
    private static int parseHTTPCode(String str, RemoteFileDesc rfd) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(str, " ");
        String token;

        // just a safety
        if (!tokenizer.hasMoreTokens())
            throw new NoHTTPOKException();

        token = tokenizer.nextToken();

        // the first token should contain HTTP
        if (token.toUpperCase(Locale.US).indexOf("HTTP") < 0)
            throw new NoHTTPOKException("got: " + str);
        // does the server support http 1.1?
        else
            rfd.setHTTP11(token.indexOf("1.1") > 0);

        // the next token should be a number
        // just a safety
        if (!tokenizer.hasMoreTokens())
            throw new NoHTTPOKException();

        token = tokenizer.nextToken();

        String num = token.trim();
        try {
            return java.lang.Integer.parseInt(num);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(e);
        }
    }

    /**
     * Reads the X-Queue headers from str, storing fields in refQueueInfo.
     * 
     * @param str a header value of form
     *        "X-Queue: position=2,length=5,limit=4,pollMin=45,pollMax=120"
     * @param refQueueInfo an array of 3 elements to store results.
     *        refQueueInfo[0] is set to the value of pollMin, or -1 if problems;
     *        refQueueInfo[1] is set to the value of pollMax, or -1 if problems;
     *        refQueueInfo[2] is set to the value of position, or -1 if
     *        problems;
     */
    private void parseQueueHeaders(String str, int[] refQueueInfo) {
        if (str == null)
            return;

        // Note: According to the specification there are 5 headers, LimeWire
        // ignores 2 of them - queue length, and maxUploadSlots.
        StringTokenizer tokenizer = new StringTokenizer(str, " ,:=");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String value;
            try {
                if (token.equalsIgnoreCase("pollMin")) {
                    value = tokenizer.nextToken();
                    refQueueInfo[0] = Integer.parseInt(value);
                } else if (token.equalsIgnoreCase("pollMax")) {
                    value = tokenizer.nextToken();
                    refQueueInfo[1] = Integer.parseInt(value);
                } else if (token.equalsIgnoreCase("position")) {
                    value = tokenizer.nextToken();
                    refQueueInfo[2] = Integer.parseInt(value);
                }
            } catch (NumberFormatException nfx) {
                Arrays.fill(refQueueInfo, -1);
                break;
            } catch (NoSuchElementException nsex) {
                Arrays.fill(refQueueInfo, -1);
                break;
            }
        }
    }

    private void validateContentRange(Range responseRange) throws IOException {
        long low = responseRange.getLow();
        long high = responseRange.getHigh() + 1;
        synchronized (this) {
            // were we stolen from in the meantime?
            if (_disconnect)
                throw new IOException("stolen from");

            // Make sure that the range they gave us is a subrange
            // of what we wanted in the first place.
            if (low < _initialReadingPoint || high > _initialReadingPoint + _amountToRead)
                throw new ProblemReadingHeaderException("invalid subrange given.  wanted low: "
                        + _initialReadingPoint + ", high: "
                        + (_initialReadingPoint + _amountToRead - 1) + "... given low: " + low
                        + ", high: " + high);
            _initialReadingPoint = low;
            _amountToRead = high - low;
        }
    }

    /**
     * Returns the interval of the responding content range. If the full content
     * range (start & stop interval) is not given, we assume it to be the
     * interval that we requested. The returned interval's low & high ranges are
     * both INCLUSIVE.
     * <p>
     * Does not strictly enforce HTTP; allows minor errors like replacing the
     * space after "bytes" with an equals. Also tries to interpret malformed
     * LimeWire 0.5 headers.
     * 
     * @param str a Content-range header line, e.g., <pre>
     *        "Content-range: bytes 0-9/10" or "Content-range:bytes 0-9/10" or
     *        "Content-range:bytes 0-9/X" (replacing X with "*") or
     *        "Content-range:bytes X/10" (replacing X with "*") or
     *        "Content-range:bytes X/X" (replacing X with "*") or Will also
     *        accept the incorrect but common "Content-range: bytes=0-9/10"
     *        </pre>
     * @exception ProblemReadingHeaderException some problem extracting the
     *            start offset.
     */
    private Range parseContentRange(String str) throws IOException {
        long numBeforeDash;
        long numBeforeSlash;
        long numAfterSlash;

        if (LOG.isDebugEnabled())
            LOG.debug("reading content range: " + str);

        // Try to parse all three numbers from header for verification.
        // Special case "*" before or after slash.
        try {
            int start = str.indexOf("bytes") + 6; // skip "bytes " or "bytes="
            int slash = str.indexOf('/');

            // "bytes */*" or "bytes */10"
            // We don't know what we're getting, but it'll start at 0.
            // Assume that we're going to get until the part we requested.
            // If we read more, good. If we read less, it'll work out just
            // fine.
            if (str.substring(start, slash).equals("*")) {
                if (LOG.isDebugEnabled())
                    LOG.debug(_rfd + " Content-Range like */?, " + str);
                synchronized (this) {
                    return Range.createRange(0, Math.max(_amountToRead - 1, 0));
                }
            }

            int dash = str.lastIndexOf("-"); // skip past "Content-range"
            numBeforeDash = Long.parseLong(str.substring(start, dash));
            numBeforeSlash = Long.parseLong(str.substring(dash + 1, slash));
            if (numBeforeDash < 0 || numBeforeSlash < 0) {
                throw new ProblemReadingHeaderException("Invalide range, low: " + numBeforeDash
                        + ", high: " + numBeforeSlash);
            }

            if (numBeforeSlash < numBeforeDash)
                throw new ProblemReadingHeaderException("invalid range, high (" + numBeforeSlash
                        + ") less than low (" + numBeforeDash + ")");

            // "bytes 0-9/*"
            if (str.substring(slash + 1).equals("*")) {
                if (LOG.isDebugEnabled())
                    LOG.debug(_rfd + " Content-Range like #-#/*, " + str);

                return Range.createRange(numBeforeDash, numBeforeSlash);
            }

            numAfterSlash = Long.parseLong(str.substring(slash + 1));
        } catch (IndexOutOfBoundsException e) {
            throw new ProblemReadingHeaderException(str);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(str);
        }

        // ignore invalid ranges
        if (numBeforeSlash >= numAfterSlash) {
            throw new ProblemReadingHeaderException(str);
        }

        if (LOG.isDebugEnabled())
            LOG.debug(_rfd + " Content-Range like #-#/#, " + str);
        try {
            return Range.createRange(numBeforeDash, numBeforeSlash);
        } catch (IllegalArgumentException iae) {
            // rethrow with tracking the input string, that caused the illegal
            // range offsets to be parsed, see LWC-1660
            IllegalArgumentException iaeWithReason = new IllegalArgumentException(
                    "invalid range for range header: " + str);
            iaeWithReason.initCause(iae);
            throw iaeWithReason;
        }
    }

    /**
     * Parses X-Available-Ranges header and stores the available ranges as a
     * list.
     * 
     * @param line the X-Available-Ranges header line which should look like:
     *        "X-Available-Ranges: bytes A-B, C-D, E-F"
     *        "X-Available-Ranges:bytes A-B"
     * @param rfd the RemoteFileDesc2 for the location we are trying to download
     *        from. We need this to store the available Ranges.
     * @exception ProblemReadingHeaderException when we could not parse the
     *            header line.
     */
    private void parseAvailableRangesHeader(String line, RemoteFileDesc rfd) throws IOException {
        IntervalSet availableRanges = new IntervalSet();

        line = line.toLowerCase(Locale.US);
        // start parsing after the word "bytes"
        int start = line.indexOf("bytes") + 6;
        // if start == -1 the word bytes has not been found
        // if start >= line.length we are at the end of the
        // header line
        while (start != -1 && start < line.length()) {
            // try to parse the number before the dash
            int stop = line.indexOf('-', start);
            // test if this is a valid interval
            if (stop == -1)
                break;

            // this is the interval to store the available
            // range we are parsing in.
            Range interval = null;

            try {
                // read number before dash
                // bytes A-B, C-D
                // ^
                long low = Long.parseLong(line.substring(start, stop).trim());

                // now moving the start index to the
                // character after the dash:
                // bytes A-B, C-D
                // ^
                start = stop + 1;
                // we are parsing the number before the comma
                stop = line.indexOf(',', start);

                // If we are at the end of the header line, there is no comma
                // following.
                if (stop == -1)
                    stop = line.length();

                // read number after dash
                // bytes A-B, C-D
                // ^
                long high = Long.parseLong(line.substring(start, stop).trim());

                // start parsing after the next comma. If we are at the
                // end of the header line start will be set to
                // line.length() +1
                start = stop + 1;

                if (high >= rfd.getSize())
                    high = rfd.getSize() - 1;

                if (low > high)// interval read off network is bad, try next one
                    continue;

                // this interval should be inclusive at both ends
                interval = Range.createRange(low, high);

            } catch (NumberFormatException e) {
                throw new ProblemReadingHeaderException(e);
            }
            availableRanges.add(interval);
        }
        rfdContext.setAvailableRanges(availableRanges);
    }

    /**
     * Parses the Retry-After header.
     * 
     * @param str expects a simple integer number specifying the number of
     *        seconds to wait before retrying the host.
     * @exception ProblemReadingHeaderException if we could not read the header
     */
    private static void parseRetryAfterHeader(String str, RemoteFileDescContext rfdContext)
            throws IOException {
        int seconds = 0;
        try {
            seconds = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(e);
        }
        // make sure the value is not smaller than MIN_RETRY_AFTER seconds
        seconds = Math.max(seconds, MIN_RETRY_AFTER);
        // make sure the value is not larger than MAX_RETRY_AFTER seconds
        seconds = Math.min(seconds, MAX_RETRY_AFTER);
        rfdContext.setRetryAfter(seconds);
    }

    /**
     * Parses the Creation Time header.
     * 
     * @param str expects a long number specifying the age in milliseconds of
     *        this file.
     * @exception ProblemReadingHeaderException if we could not read the header
     */
    private void parseCreationTimeHeader(String str, RemoteFileDesc rfd) throws IOException {
        long milliSeconds = 0;
        try {
            milliSeconds = Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(e);
        }
        if (rfd.getSHA1Urn() != null && milliSeconds > 0) {
            synchronized (creationTimeCache) {
                Long cTime = creationTimeCache.getCreationTime(rfd.getSHA1Urn());
                // prefer older times....
                if ((cTime == null) || (cTime.longValue() > milliSeconds))
                    creationTimeCache.addTime(rfd.getSHA1Urn(), milliSeconds);
            }
        }
    }

    /**
     * This method reads the "X-Features" header and looks for features we
     * understand.
     * 
     * @param str the header line.
     */
    private void parseFeatureHeader(String str) {
        StringTokenizer tok = new StringTokenizer(str, ",");

        while (tok.hasMoreTokens()) {
            String feature = tok.nextToken();
            String protocol = "";
            int slash = feature.indexOf("/");
            if (slash == -1) {
                protocol = feature.toLowerCase(Locale.US).trim();
            } else {
                protocol = feature.substring(0, slash).toLowerCase(Locale.US).trim();
            }
            // ignore the version for now.

            if (protocol.equals(HTTPConstants.BROWSE_PROTOCOL))
                _browseEnabled = true;
            else if (protocol.equals(HTTPConstants.PUSH_LOCS))
                _wantsFalts = true;
            else if (protocol.equals(HTTPConstants.FW_TRANSFER)) {
                // for this header we care about the version
                int FWTVersion = 0;
                try {
                    FWTVersion = (int) HTTPUtils.parseFeatureToken(feature);
                    _wantsFalts = true;
                } catch (ProblemReadingHeaderException prhe) {
                    // ignore this header
                    continue;
                }

                // try to update the FWT version and external address we know
                // for this host
                updatePEAddress();
                pushEndpointCache.get().setFWTVersionSupported(_rfd.getClientGUID(), FWTVersion);
            }
        }
    }

    /**
     * Method for reading the X-Thex-Uri header.
     */
    private void parseTHEXHeader(String str) {
        if (LOG.isDebugEnabled())
            LOG.debug("thex: " + getHostAddress() + ">" + str);

        if (str.indexOf(";") > 0) {
            StringTokenizer tok = new StringTokenizer(str, ";");
            _thexUri = tok.nextToken();
            _root32 = tok.nextToken();
        } else
            _thexUri = str;
    }

    /**
     * 
     * Method for parsing the header containing firewalled alternate locations.
     * The format is a modified version of the one described in the push proxy
     * spec at the_gdf
     * 
     */
    private void parseFALTHeader(String str) {
        // if we entered this method means the other side is interested
        // in receiving firewalled locations.
        _wantsFalts = true;

        // this just delegates to readAlternateLocationHeader
        readAlternateLocations(str, false);
    }

    /**
     * Parses the header containing the current set of push proxies for the
     * given host, and updates the rfd.
     */
    private void parseProxiesHeader(String str) {
        if (str == null || str.length() < 12)
            return;

        pushEndpointCache.get().overwriteProxies(_rfd.getClientGUID(), str);
        updatePEAddress();
    }

    /**
     * Parses port for firewalled-to-firewalled transfers and updates push
     * endpoint address with the socket's ip address and the port.
     */
    private void parseFWTPortHeader(String str) {
        try {
            int port = Integer.parseInt(str);
            if (NetworkUtils.isValidPort(port)) {
                IpPort newAddr = new IpPortImpl(_socket.getInetAddress(), port);
                pushEndpointCache.get().setAddr(_rfd.getClientGUID(), newAddr);
            }
        } catch (NumberFormatException nfe) {
            // do nothing, invalid network input
        }
    }

    private void updatePEAddress() {
        if (_socket instanceof RUDPSocket) {
            IpPort newAddr = new IpPortImpl(_socket.getInetAddress(), _socket.getPort());
            if (networkInstanceUtils.isValidExternalIpPort(newAddr))
                pushEndpointCache.get().setAddr(_rfd.getClientGUID(), newAddr);
        }
    }

    // ///////////////////////////// Download ////////////////////////////////

    /*
     * Downloads the content from the server and writes it to a temporary file.
     * Non-blocking. This MUST be initialized via connect() beforehand, and
     * doDownload MUST NOT have already been called.
     * 
     * @exception IOException download was interrupted, typically (but not
     * always) because the other end closed the connection.
     */
    public void doDownload(IOStateObserver observer) throws SocketException {
        _socket.setSoTimeout(60 * 1000); // downloading, can stall upto 1 minute
        observerHandler.setDelegate(observer);
        _stateMachine.addState(new DownloadState());
    }

    private class DownloadState extends ReadState {
        private long currPos = _initialReadingPoint;

        private volatile boolean doingWrite;

        void writeDone() {
            doingWrite = false;
            _stateMachine.handleRead();
        }

        @Override
        protected boolean processRead(ReadableByteChannel channel, ByteBuffer buffer)
                throws IOException {
            if (doingWrite)
                return true;

            boolean dataLeft = false;
            try {
                // LOG.debug("Doing read");
                dataLeft = readImpl(channel, buffer);
            } catch (IOException error) {
                LOG.debug("Error while reading", error);
                chunkCompleted();
                throw error;
            }

            if (!dataLeft) {
                chunkCompleted();
                if (!isHTTP11() || _disconnect)
                    throw new IOException("stolen from");
            }

            return dataLeft;
        }

        private void chunkCompleted() {
            _bodyConsumed = true;
            synchronized (HTTPDownloader.this) {
                _isActive = false;
            }
        }

        private boolean readImpl(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
            while (true) {
                long read = 0;

                // first see how much we have left to read, if any
                long left;
                synchronized (HTTPDownloader.this) {
                    if (_amountRead >= _amountToRead) {
                        LOG.debug("Read >= to needed, done.");
                        _isActive = false;
                        return false;
                    }
                    left = _amountToRead - _amountRead;
                }

                // Account for data already in the buffer.
                int preread = (int) Math.min(left, buffer.position());
                if (preread != 0 && LOG.isDebugEnabled())
                    LOG.debug("Using preread data of: " + preread);

                if (left - preread > 0) {
                    // ensure we don't read more into the buffer than we want.
                    if (buffer.limit() > left)
                        buffer.limit((int) left);

                    while (buffer.hasRemaining() && (read = rc.read(buffer)) > 0)
                        ;

                    // ensure the limit is set back to normal.
                    buffer.limit(buffer.capacity());
                }

                int totalRead = buffer.position();
                if (_inNetwork)
                    tcpBandwidthStatistics.getStatistic(
                            StatisticType.HTTP_BODY_INNETWORK_DOWNSTREAM).addData(totalRead);
                else
                    tcpBandwidthStatistics.getStatistic(StatisticType.HTTP_BODY_DOWNSTREAM)
                            .addData(totalRead);

                // If nothing could be read at all, leave.
                if (totalRead == 0) {
                    if (read == -1) {
                        LOG.debug("EOF while reading");
                        throw new IOException("EOF");
                    } else if (read == 0) {
                        return true;
                    }
                }

                long filePosition;
                int dataLength;
                int dataStart;
                synchronized (this) {
                    if (_isActive) {
                        // see if we were stolen from while reading
                        totalRead = (int) Math.min(totalRead, _amountToRead - _amountRead);
                        if (totalRead <= 0) {
                            LOG.debug("Someone stole completely from us while reading");
                            // if were told to not read anything more, finish
                            _isActive = false;
                            buffer.clear();
                            return false;
                        }

                        int skipped = (int) Math.min(totalRead, Math.max(0, _initialWritingPoint
                                - currPos));
                        if (skipped > 0)
                            LOG.debug("Amount we should skip: " + skipped);

                        // setup data for writing.
                        dataLength = totalRead - skipped;
                        dataStart = skipped;
                        filePosition = currPos + skipped;
                        // maintain data for next read.
                        _amountRead += totalRead;
                        currPos += totalRead;

                        if (skipped >= totalRead) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("skipped full read of: " + skipped + " bytes");
                            buffer.clear();
                            continue;
                        }
                    } else {
                        if (LOG.isDebugEnabled())
                            LOG.debug("WORKER:" + this + " stopping at "
                                    + (_initialReadingPoint + _amountRead));
                        buffer.clear();
                        return false;
                    }
                }

                // TODO: Write to disk only when buffer is full.
                try {
                    // write to disk outside of lock.
                    // LOG.debug("WORKER: " + this + ", left: " +
                    // (left-totalRead) +",  writing fp: " + filePosition
                    // +", ds: " + dataStart + ", dL: " + dataLength);
                    VerifyingFile.WriteRequest request = new VerifyingFile.WriteRequest(
                            filePosition, dataStart, dataLength, buffer.array());
                    if (!_incompleteFile.writeBlock(request)) {
                        LOG.debug("Scheduling callback for write.");
                        InterestReadableByteChannel irc = (InterestReadableByteChannel) rc;
                        irc.interestRead(false);
                        doingWrite = true;
                        _incompleteFile.registerWriteCallback(request, new DownloadRestarter(irc,
                                buffer, this));
                        return true;
                    }
                } catch (AssertFailure bad) {
                    createAssertionReport(bad);
                }
                buffer.clear();
            }
        }

        public long getAmountProcessed() {
            return -1;
        }
    }

    void createAssertionReport(AssertFailure bad) {
        String currentWorker = "current worker " + System.identityHashCode(this);
        String allWorkers = null;
        URN urn = _rfd.getSHA1Urn();
        if (urn != null) {
            ManagedDownloaderImpl myDownloader = (ManagedDownloaderImpl) downloadManager
                    .getDownloaderForURN(urn);
            if (myDownloader == null)
                allWorkers = "couldn't find my downloader???";
            else
                allWorkers = myDownloader.getWorkersInfo();
        } else
            allWorkers = " sha1 not available ";

        String errorReport = bad.getMessage() + "\n\n" + currentWorker + "\n\n" + allWorkers;
        AssertFailure failure = new AssertFailure(errorReport);
        failure.setStackTrace(bad.getStackTrace()); // so we see the VF dump
                                                    // only once.
        throw failure;

    }

    private static class DownloadRestarter implements VerifyingFile.WriteCallback, Runnable {
        private final DownloadState downloader;

        private final InterestReadableByteChannel irc;

        private final ByteBuffer buffer;

        DownloadRestarter(InterestReadableByteChannel irc, ByteBuffer buffer,
                DownloadState downloader) {
            this.irc = irc;
            this.buffer = buffer;
            this.downloader = downloader;
        }

        public void writeScheduled() {
            LOG.debug("Delayed write scheduled");
            NIODispatcher.instance().executeLaterAlways(this);
        }

        public void run() {
            buffer.clear();
            downloader.writeDone();
            irc.interestRead(true);
        }
    }

    /**
     * Stops this immediately. This method is always safe to call.
     * 
     * @modifies this
     */
    public void stop() {
        synchronized (this) {
            if (LOG.isDebugEnabled())
                LOG.debug("WORKER:" + this + " signaled to stop at "
                        + (_initialReadingPoint + _amountRead), new Exception());
            _isActive = false;
        }

        // Close in the NIO thread, so everything stays there.
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                IOUtils.close(_socket);
            }
        });
    }

    /**
     * Instructs this stop just before reading the given byte. This cannot be
     * used to increase the initial range.
     * 
     * @param stop the index just past the last byte to read; stop-1 is the
     *        index of the last byte to be downloaded
     */
    public synchronized void stopAt(long stop) {
        _disconnect = true;
        _amountToRead = Math.min(_amountToRead, stop - _initialReadingPoint);
    }

    public synchronized void startAt(long start) {
        _initialWritingPoint = start;
    }

    synchronized void forgetRanges() {
        _initialWritingPoint = 0;
        _initialReadingPoint = 0;
        _amountToRead = 0;
        _totalAmountRead += _amountRead;
        _amountRead = 0;
    }

    // /////////////////////////// Accessors ///////////////////////////////////

    public synchronized long getInitialReadingPoint() {
        return _initialReadingPoint;
    }

    public synchronized long getInitialWritingPoint() {
        return _initialWritingPoint;
    }

    public synchronized long getAmountRead() {
        return _amountRead;
    }

    public synchronized long getTotalAmountRead() {
        return _totalAmountRead + _amountRead;
    }

    public synchronized long getAmountToRead() {
        return _amountToRead;
    }

    public synchronized boolean isActive() {
        return _isActive;
    }

    synchronized boolean isVictim() {
        return _disconnect;
    }

    /**
     * Forces this to not write past the given byte of the file, if it has not
     * already done so. Typically this is called to reduce the download window;
     * doing otherwise will typically result in incomplete downloads.
     */
    public InetAddress getInetAddress() {
        return _socket.getInetAddress();
    }

    public boolean browseEnabled() {
        return _browseEnabled;
    }

    /**
     * @return whether the remote host is interested in receiving firewalled
     *         alternate locations.
     */
    public boolean wantsFalts() {
        return _wantsFalts;
    }

    public String getVendor() {
        return _server;
    }

    public long getIndex() {
        return _index;
    }

    public String getFileName() {
        return _filename;
    }

    public byte[] getGUID() {
        return _guid;
    }

    public int getPort() {
        return _socket.getPort();
    }

    /**
     * Returns the RemoteFileDesc passed to this' constructor.
     */
    public RemoteFileDesc getRemoteFileDesc() {
        return _rfd;
    }

    RemoteFileDescContext getContext() {
        return rfdContext;
    }

    /**
     * Returns true if we have think that the server supports HTTP1.1.
     */
    public boolean isHTTP11() {
        return _rfd.isHTTP11();
    }

    /**
     * Returns true if this downloader has a THEX tree that we have not yet
     * retrieved.
     */
    public boolean hasHashTree() {
        return _thexUri != null && _root32 != null && !rfdContext.hasTHEXFailed()
                && !_thexSucceeded;
    }

    // ///////////////////Bandwidth tracker interface methods//////////////
    public void measureBandwidth() {
        long totalAmountRead = 0;
        synchronized (this) {
            if (!_isActive)
                return;
            totalAmountRead = getTotalAmountRead();
        }

        bandwidthTracker.measureBandwidth(totalAmountRead);
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        return bandwidthTracker.getMeasuredBandwidth();
    }

    public float getAverageBandwidth() {
        return bandwidthTracker.getAverageBandwidth();
    }

    /**
     * Apply bandwidth limitation from settings.
     */
    // //////////////////////////// Unit Test ////////////////////////////////
    @Override
    public String toString() {
        return "<" + getHostAddress() + ", " + getFileName() + ">";
    }

    public static void setThrottleSwitching(boolean on) {
        // THROTTLE.setSwitching(on);
        // DO NOT PUT SWITCHING ON THE UDP SIDE.
    }

    private static class Observer implements IOStateObserver {
        private IOStateObserver delegate;

        private boolean handled = false;

        private boolean error = false;

        public void handleIOException(IOException iox) {
            IOStateObserver del;
            synchronized (this) {
                error = true;
                if (handled) {
                    LOG.warn("Ignoring iox", iox);
                    return;
                }

                handled = true;
                del = delegate;
            }
            if (del != null)
                del.handleIOException(iox);
        }

        public void handleStatesFinished() {
            IOStateObserver del;
            synchronized (this) {
                if (handled) {
                    if (LOG.isWarnEnabled())
                        LOG.warn("Ignoring states finished", new Exception());
                    return;
                }
                handled = true;
                del = delegate;
            }
            if (del != null)
                del.handleStatesFinished();
        }

        public void shutdown() {
            IOStateObserver del;
            synchronized (this) {
                error = true;
                if (handled) {
                    if (LOG.isWarnEnabled())
                        LOG.warn("Ignoring shutdown.");
                    return;
                }
                handled = true;
                del = delegate;
            }
            if (del != null)
                del.shutdown();
        }

        void setDelegate(IOStateObserver observer) {
            boolean hadError = false;
            synchronized (this) {
                handled = false;
                hadError = error;
                delegate = observer;
            }

            if (hadError) {
                observer.shutdown();
            }
        }
    }
}
