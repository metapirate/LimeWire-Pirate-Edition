package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IpPort;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Contains the necessary information to form a response to a connection
 * handshake. It contains a status code, a status message, and the headers to
 * use in the response.
 * <p>
 * There are only two ways to create a HandshakeResponse.
 * <ol>
 * <li>Create an instance which defaults the status code and status message to
 * be "200 OK". Only the headers used in the response need to be passed in.
 * <li>Create an instance with a custom status code, status message, and the
 * headers used in the response.
 * </ol>
 */
public class HandshakeResponse {

    /**
     * Version of LimeWire we consider old.
     */
    private static final Version OLD_LIME_VERSION;
    static {
        Version v = null;
        try {
            v = new Version("3.4.0");
        } catch (VersionFormatException impossible) {
        }
        OLD_LIME_VERSION = v;
    }

    /**
     * The "default" status code in a connection handshake indicating that the
     * handshake was successful and the connection can be established.
     */
    public static final int OK = 200;

    /**
     * The "default" status message in a connection handshake indicating that
     * the handshake was successful and the connection can be established.
     */
    public static final String OK_MESSAGE = "OK";

    /**
     * HTTP response code for the crawler.
     */
    public static final int CRAWLER_CODE = 593;

    /**
     * HTTP response message for the crawler.
     */
    public static final String CRAWLER_MESSAGE = "Hi";

    /**
     * The error code that a shielded leaf node should give to incoming
     * connections.
     */
    public static final int SHIELDED = 503;

    /**
     * The error code that a node with no slots should give to incoming
     * connections.
     */
    public static final int SLOTS_FULL = 503;

    /**
     * Default bad status code to be used while rejecting connections.
     */
    public static final int DEFAULT_BAD_STATUS_CODE = 503;

    /**
     * Default bad status message to be used while rejecting connections.
     */
    public static final String DEFAULT_BAD_STATUS_MESSAGE = "Service Not Available";

    /*
     * TODO: check about this error code...
     */
    public static final int LOCALE_NO_MATCH = 577;

    public static final String LOCALE_NO_MATCH_MESSAGE = "Service Not Available";

    /**
     * HTTP-like status code used when handshaking (e.g., 200, 401, 503).
     */
    private final int STATUS_CODE;

    /**
     * Message used with status code when handshaking (e.g., "OK, "Service Not
     * Available"). The status message together with the status code make up the
     * status line (i.e., first line) of an HTTP-like response to a connection
     * handshake.
     */
    private final String STATUS_MESSAGE;

    /**
     * Headers to use in the response to a connection handshake.
     */
    private final Properties HEADERS;

    /**
     * Is the GGEP header set?
     */
    private Boolean _supportsGGEP;

    /**
     * Cached boolean for whether or not this is considered a considered a
     * "good" leaf connection.
     */
    private final boolean GOOD_LEAF;

    /**
     * Cached boolean for whether or not this is considered a considered a
     * "good" ultrapeer connection.
     */
    private final boolean GOOD_ULTRAPEER;

    /**
     * Cached value for the number of Ultrapeers this Ultrapeer attempts to
     * connect to.
     */
    private final int DEGREE;

    /**
     * Cached value for whether or not this is a high degree connection.
     */
    private final boolean HIGH_DEGREE;

    /**
     * Cached value for whether or not this is an Ultrapeer connection that
     * supports Ultrapeer query routing.
     */
    private final boolean ULTRAPEER_QRP;

    /**
     * Cached value for the maximum TTL to use along this connection.
     */
    private final byte MAX_TTL;

    /**
     * Cached value for whether or not this connection supports dynamic
     * querying.
     */
    private final boolean DYNAMIC_QUERY;

    /**
     * Cached value for whether or not this connection reported X-Ultrapeer:
     * true in it's handshake headers.
     */
    private final boolean ULTRAPEER;

    /**
     * Cached value for whether or not this connection reported X-Ultrapeer:
     * false in it's handshake headers.
     */
    private final boolean LEAF;

    /**
     * Cached value for whether or not the connection reported Content-Encoding:
     * deflate
     */
    private final boolean DEFLATE_ENCODED;

    /**
     * Constant for whether or not this connection supports probe queries.
     */
    private final boolean PROBE_QUERIES;

    /**
     * Constant for whether or not this node supports pong caching.
     */
    private final boolean PONG_CACHING;

    /**
     * Constant for whether or not this node supports GUESS.
     */
    private final boolean GUESS_CAPABLE;

    /**
     * Constant for whether or not this is a crawler.
     */
    private final boolean IS_CRAWLER;

    /**
     * Constant for whether or not this node is a LimeWire (or derivative)
     */
    private final boolean IS_LIMEWIRE;

    /**
     * Constant for whether or nor this node is an older limewire.
     */
    private final boolean IS_OLD_LIMEWIRE;

    /**
     * Constant for whether or not the client claims to do no requerying.
     */
    private final boolean NO_REQUERYING;

    /**
     * Locale
     */
    private final String LOCALE_PREF;

    /** The port the response says it's listening on. */
    private final int LISTEN_PORT;

    /** This connection's lime version */
    private final Version limeVersion;

    /**
     * Constant for the number of hosts to return in X-Try-Ultrapeer headers.
     */
    private static final int NUM_X_TRY_ULTRAPEER_HOSTS = 10;

    /**
     * Creates a <tt>HandshakeResponse</tt> which defaults the status code and
     * status message to be "200 Ok" and uses the desired headers in the
     * response.
     * 
     * @param headers the headers to use in the response.
     */
    // public for testing
    public HandshakeResponse(Properties headers) {
        this(OK, OK_MESSAGE, headers);
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance with the specified
     * response code and message and with no extra connection headers.
     * 
     * @param code the status code for the response
     * @param message the status message
     */
    protected HandshakeResponse(int code, String message) {
        this(code, message, new Properties());
    }

    /**
     * Creates a HandshakeResponse with the desired status code, status message,
     * and headers to respond with.
     * 
     * @param code the response code to use
     * @param message the response message to use
     * @param headers the headers to use in the response
     */
    HandshakeResponse(int code, String message, Properties headers) {
        STATUS_CODE = code;
        STATUS_MESSAGE = message;
        HEADERS = headers;
        DEGREE = extractIntHeaderValue(HEADERS, HeaderNames.X_DEGREE, 6);
        HIGH_DEGREE = getNumIntraUltrapeerConnections() >= 15;
        ULTRAPEER_QRP = isVersionOrHigher(HEADERS, HeaderNames.X_ULTRAPEER_QUERY_ROUTING, 0.1F);
        MAX_TTL = extractByteHeaderValue(HEADERS, HeaderNames.X_MAX_TTL, (byte) 4);
        DYNAMIC_QUERY = isVersionOrHigher(HEADERS, HeaderNames.X_DYNAMIC_QUERY, 0.1F);
        PROBE_QUERIES = isVersionOrHigher(HEADERS, HeaderNames.X_PROBE_QUERIES, 0.1F);
        NO_REQUERYING = isFalseValue(HEADERS, HeaderNames.X_REQUERIES);

        IS_LIMEWIRE = extractStringHeaderValue(headers, HeaderNames.USER_AGENT).toLowerCase(
                Locale.US).startsWith("limewire");

        GOOD_ULTRAPEER = isHighDegreeConnection() && isUltrapeerQueryRoutingConnection()
                && (getMaxTTL() < 5) && isDynamicQueryConnection();

        GOOD_LEAF = GOOD_ULTRAPEER && (IS_LIMEWIRE || NO_REQUERYING);

        ULTRAPEER = isTrueValue(HEADERS, HeaderNames.X_ULTRAPEER);
        LEAF = isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER);
        DEFLATE_ENCODED = isStringValue(HEADERS, HeaderNames.CONTENT_ENCODING,
                HeaderNames.DEFLATE_VALUE);
        PONG_CACHING = isVersionOrHigher(headers, HeaderNames.X_PONG_CACHING, 0.1F);
        GUESS_CAPABLE = isVersionOrHigher(headers, HeaderNames.X_GUESS, 0.1F);
        IS_CRAWLER = isVersionOrHigher(headers, HeaderNames.CRAWLER, 0.1F);
        Version version = null;
        if (IS_LIMEWIRE) {
            version = extractVersion(extractStringHeaderValue(headers, HeaderNames.USER_AGENT));
            IS_OLD_LIMEWIRE = version != null && version.compareTo(OLD_LIME_VERSION) < 0;
        } else
            IS_OLD_LIMEWIRE = false;
        limeVersion = version;

        String loc = extractStringHeaderValue(headers, HeaderNames.X_LOCALE_PREF);
        LOCALE_PREF = (loc.equals("")) ? ApplicationSettings.DEFAULT_LOCALE.get() : loc;

        LISTEN_PORT = extractIntHeaderValueAfter(headers, HeaderNames.LISTEN_IP, ":", -1);
    }

    private Version extractVersion(String userAgent) {
        StringTokenizer tok = new StringTokenizer(userAgent, "/. ");
        if (tok.countTokens() < 3)
            return null;
        tok.nextToken(); // limewire
        String v = tok.nextToken() + "." + tok.nextToken() + ".";
        if (tok.hasMoreTokens())
            v += tok.nextToken();
        else
            v += "0";
        try {
            return new Version(v);
        } catch (VersionFormatException vfe) {
            return null;
        }
    }

    private static final HandshakeResponse EMPTY_RESPONSE = new HandshakeResponse(new Properties());

    /**
     * Creates an empty response with no headers. This is useful, for example,
     * during connection handshaking when we haven't yet read any headers.
     * 
     * @return a new, empty <tt>HandshakeResponse</tt> instance
     */
    public static HandshakeResponse createEmptyResponse() {
        return EMPTY_RESPONSE;
    }

    /**
     * Constructs the response from the other host during connection
     * handshaking.
     * 
     * @return a new <tt>HandshakeResponse</tt> instance with the headers sent
     *         by the other host
     */
    public static HandshakeResponse createResponse(Properties headers) {
        return new HandshakeResponse(headers);
    }

    /**
     * Constructs the response from the other host during connection
     * handshaking. The returned response contains the connection headers sent
     * by the remote host.
     * 
     * @param line the status line received from the connecting host
     * @param headers the headers received from the other host
     * @return a new <tt>HandshakeResponse</tt> instance with the headers sent
     *         by the other host
     * @throws <tt>IOException</tt> if the status line could not be parsed
     */
    public static HandshakeResponse createRemoteResponse(String line, Properties headers)
            throws IOException {
        int code = extractCode(line);
        if (code == -1)
            throw new IOException("could not parse status code: " + line);

        String message = extractMessage(line);
        if (message == null)
            throw new IOException("could not parse status message: " + line);

        return new HandshakeResponse(code, message, headers);
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that accepts the
     * potential connection.
     * 
     * @param headers the <tt>Properties</tt> instance containing the headers to
     *        send to the node we're accepting
     */
    static HandshakeResponse createAcceptIncomingResponse(HandshakeResponse response,
            Properties headers, HandshakeServices handshakeServices) {
        return new HandshakeResponse(addXTryHeader(response, headers, handshakeServices));
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that accepts the
     * outgoing connection -- the final third step in the handshake. This passes
     * no headers, as all necessary headers have already been exchanged. The
     * only possible exception is the potential inclusion of X-Ultrapeer: false.
     * 
     * @param headers the <tt>Properties</tt> instance containing the headers to
     *        send to the node we're accepting
     */
    static HandshakeResponse createAcceptOutgoingResponse(Properties headers) {
        return new HandshakeResponse(headers);
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that responds to a
     * special crawler connection with connected leaves and Ultrapeers. See the
     * Files>>Development section on the GDF.
     */
    static HandshakeResponse createCrawlerResponse(HandshakeServices handshakeServices) {
        Properties headers = new Properties();

        // add our user agent
        headers.put(HeaderNames.USER_AGENT, LimeWireUtils.getHttpServer());
        headers.put(HeaderNames.X_ULTRAPEER, Boolean.toString(handshakeServices.isUltrapeer()));

        // add any leaves
        List<? extends IpPort> leaves = handshakeServices.getLeafNodes();
        headers.put(HeaderNames.LEAVES, createEndpointString(leaves, leaves.size()));

        // add any Ultrapeers
        List<? extends IpPort> ultrapeers = handshakeServices.getUltrapeerNodes();
        headers.put(HeaderNames.PEERS, createEndpointString(ultrapeers, ultrapeers.size()));

        return new HandshakeResponse(HandshakeResponse.CRAWLER_CODE,
                HandshakeResponse.CRAWLER_MESSAGE, headers);
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that rejects the
     * potential connection. This includes the X-Try-Ultrapeers header to tell
     * the remote host about other nodes to connect to. We return the hosts we
     * most recently knew to have free leaf or ultrapeer connection slots.
     * 
     * @param hr the <tt>HandshakeResponse</tt> containing the connection
     *        headers of the connecting host
     * @return a <tt>HandshakeResponse</tt> with the appropriate response
     *         headers
     */
    static HandshakeResponse createUltrapeerRejectIncomingResponse(HandshakeResponse hr,
            HandshakeStatus status, HandshakeServices handshakeServices) {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, status.getMessage(),
                addXTryHeader(hr, new Properties(), handshakeServices));
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that rejects the
     * potential connection. The returned <tt>HandshakeResponse</tt> DOES NOT
     * include the X-Try-Ultrapeers header because this is an outgoing
     * connection, and we should not send host data that the remote client does
     * not request.
     */
    static HandshakeResponse createRejectOutgoingResponse(HandshakeStatus status) {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, status.getMessage(),
                new Properties());
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that rejects the
     * potential connection to a leaf. We add hosts that we know about with free
     * connection slots to the X-Try-Ultrapeers header.
     * 
     * @param hr the <tt>HandshakeResponse</tt> containing the headers of the
     *        remote host
     * @return a new <tt>HandshakeResponse</tt> instance rejecting the
     *         connection and with the specified connection headers
     */
    static HandshakeResponse createLeafRejectIncomingResponse(HandshakeResponse hr,
            HandshakeStatus status, HandshakeServices handshakeServices) {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, status.getMessage(),
                addXTryHeader(hr, new Properties(), handshakeServices));
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that rejects an
     * outgoing leaf connection. This occurs when we, as a leaf, reject a
     * connection on the third stage of the handshake.
     * 
     * @return a new <tt>HandshakeResponse</tt> instance rejecting the
     *         connection and with no extra headers
     */
    static HandshakeResponse createLeafRejectOutgoingResponse(HandshakeStatus status) {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, status.getMessage());
    }

    static HandshakeResponse createLeafRejectLocaleOutgoingResponse() {
        return new HandshakeResponse(HandshakeResponse.LOCALE_NO_MATCH,
                HandshakeResponse.LOCALE_NO_MATCH_MESSAGE);
    }

    /**
     * Creates a new String of hosts, limiting the number of hosts to add to the
     * default value of 10. This is particularly used for the X-Try-Ultrapeers
     * header.
     * 
     * @param hosts a <tt>Collection</tt> of <tt>IpPort</tt> instances
     * @return a string of the form IP:port,IP:port,... from the given list of
     *         hosts
     * 
     *         Default access for testing.
     */
    static String createEndpointString(Collection<? extends IpPort> hosts) {
        return createEndpointString(hosts, NUM_X_TRY_ULTRAPEER_HOSTS);
    }

    /**
     * Utility method that takes the specified list of hosts and returns a
     * string of the form:
     * <p>
     * 
     * IP:port,IP:port,IP:port
     * 
     * @param hosts a <tt>Collection</tt> of <tt>IpPort</tt> instances
     * @return a string of the form IP:port,IP:port,... from the given list of
     *         hosts Default access for testing.
     */
    static String createEndpointString(Collection<? extends IpPort> hosts, int limit) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        Iterator<? extends IpPort> iter = hosts.iterator();
        while (iter.hasNext() && i < limit) {
            IpPort host = iter.next();
            sb.append(host.getAddress());
            sb.append(":");
            sb.append(host.getPort());
            if (iter.hasNext()) {
                sb.append(",");
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * Utility method to extract the connection code from the connect string,
     * such as "200" in a "200 OK" message.
     * <p>
     * Default access for testing.
     * 
     * @param line the full connection string, such as "200 OK."
     * @return the status code for the connection string, or -1 if the code
     *         could not be parsed
     */
    static int extractCode(String line) {
        // get the status code and message out of the status line
        int statusMessageIndex = line.indexOf(" ");
        if (statusMessageIndex == -1)
            return -1;
        try {
            return Integer.parseInt(line.substring(0, statusMessageIndex).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Utility method to extract the connection message from the connect string,
     * such as "OK" in a "200 OK" message.
     * <p>
     * Default access for testing.
     * 
     * @param line the full connection string, such as "200 OK."
     * @return the status message for the connection string
     */
    static String extractMessage(String line) {
        // get the status code and message out of the status line
        int statusMessageIndex = line.indexOf(" ");
        if (statusMessageIndex == -1)
            return null;
        return line.substring(statusMessageIndex).trim();
    }

    /**
     * Utility method for creating a set of headers with the X-Try-Ultrapeers
     * header set according to the headers from the remote host.
     * <p>
     * Default access for testing.
     * @param hr the <tt>HandshakeResponse</tt> of the incoming request
     * @return a new <tt>Properties</tt> instance with the X-Try-Ultrapeers
     *         header set according to the incoming headers from the remote host
     */
    static Properties addXTryHeader(HandshakeResponse hr, Properties headers,
            HandshakeServices handshakeServices) {
        Collection<IpPort> hosts = handshakeServices.getAvailableHosts(hr.isUltrapeer(), hr
                .getLocalePref(), 10);
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, createEndpointString(hosts));
        return headers;
    }

    /**
     * Returns the response code.
     */
    public int getStatusCode() {
        return STATUS_CODE;
    }

    /**
     * Returns the status message.
     * 
     * @return the status message (e.g. "OK" , "Service Not Available" etc.)
     */
    public String getStatusMessage() {
        return STATUS_MESSAGE;
    }

    /**
     * Tells if the status returned was OK or not.
     * 
     * @return true, if the status returned was not the OK status, false
     *         otherwise
     */
    public boolean notOKStatusCode() {
        if (STATUS_CODE != OK)
            return true;
        else
            return false;
    }

    /**
     * Returns whether or not this connection was accepted -- whether or not the
     * connection returned Gnutella/0.6 200 OK.
     * 
     * @return <tt>true</tt> if the server returned Gnutella/0.6 200 OK,
     *         otherwise <tt>false</tt>
     */
    public boolean isAccepted() {
        return STATUS_CODE == OK;
    }

    /**
     * Returns the status code and status message together used in a status
     * line. (e.g., "200 OK", "503 Service Not Available")
     */
    public String getStatusLine() {
        return new String(STATUS_CODE + " " + STATUS_MESSAGE);
    }

    /**
     * Returns the headers as a <tt>Properties</tt> instance.
     */
    public Properties props() {
        return HEADERS;
    }

    /**
     * Accessor for an individual property.
     */
    public String getProperty(String prop) {
        return HEADERS.getProperty(prop);
    }

    /**
     * Returns the vendor string reported by this connection, i.e., the
     * USER_AGENT property, or null if it wasn't set.
     * 
     * @return the vendor string, or null if unknown
     */
    public String getUserAgent() {
        return HEADERS.getProperty(HeaderNames.USER_AGENT);
    }

    /**
     * Returns the maximum TTL that queries originating from us and sent from
     * this connection should have. If the max TTL header is not present, the
     * default TTL is assumed.
     * 
     * @return the maximum TTL that queries sent to this connection should have
     *         -- this will always be 5 or less
     */
    public byte getMaxTTL() {
        return MAX_TTL;
    }

    /**
     * Accessor for the X-Try-Ultrapeers header. If the header does not exist or
     * is empty, this returns the empty string.
     * 
     * @return the string of X-Try-Ultrapeer hosts, or the empty string if they
     *         do not exist
     */
    public String getXTryUltrapeers() {
        return extractStringHeaderValue(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * This is a convenience method to see if the connection passed the
     * X-Try-Ultrapeer header. This simply checks the existence of the header --
     * if the header was sent but is empty, this still returns <tt>true</tt>.
     * 
     * @return <tt>true</tt> if this connection sent the X-Try-Ultrapeer header,
     *         otherwise <tt>false</tt>
     */
    public boolean hasXTryUltrapeers() {
        return headerExists(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * Returns whether or not this host included leaf guidance, i.e., whether or
     * not the host wrote:
     * <p>
     * X-Ultrapeer-Needed: false
     * 
     * @return <tt>true</tt> if the other host returned X-Ultrapeer-Needed:
     *         false, otherwise <tt>false</tt>
     */
    public boolean hasLeafGuidance() {
        return isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER_NEEDED);
    }

    /**
     * Returns the number of intra-Ultrapeer connections this node maintains.
     * 
     * @return the number of intra-Ultrapeer connections this node maintains
     */
    public int getNumIntraUltrapeerConnections() {
        return DEGREE;
    }

    // implements ReplyHandler interface -- inherit doc comment
    public boolean isHighDegreeConnection() {
        return HIGH_DEGREE;
    }

    /**
     * Returns whether or not we think this connection is from a LimeWire or a
     * derivative of LimeWire.
     */
    public boolean isLimeWire() {
        return IS_LIMEWIRE;
    }

    /**
     * @return true if we consider this an older version of LimeWire, false
     *         otherwise
     */
    public boolean isOldLimeWire() {
        return IS_OLD_LIMEWIRE;
    }

    /**
     * @return the version of this connection if its LimeWire. null if not
     *         LimeWire or could not be parsed.
     */
    public Version getLimeVersion() {
        return limeVersion;
    }

    /**
     * Returns whether or not this is connection passed the headers to be
     * considered a "good" leaf.
     * 
     * @return <tt>true</tt> if this is considered a "good" leaf, otherwise
     *         <tt>false</tt>
     */
    public boolean isGoodLeaf() {
        return GOOD_LEAF;
    }

    /**
     * Returns whether or not this connection is encoded in deflate.
     */
    public boolean isDeflateEnabled() {
        // this does NOT check the setting because we have already told the
        // outgoing side we support encoding, and they're expecting us to use it
        return DEFLATE_ENCODED;
    }

    /**
     * Returns whether or not this connection accepts deflate as an encoding.
     */
    public boolean isDeflateAccepted() {
        // Note that we check the ENCODE_DEFLATE setting, and NOT the
        // ACCEPT_DEFLATE setting. This is a trick to prevent the
        // HandshakeResponders from thinking they can encode
        // the via deflate if we do not want to encode in deflate.
        return ConnectionSettings.ENCODE_DEFLATE.getValue() && containsStringValue(HEADERS, // the
                                                                                            // headers
                                                                                            // to
                                                                                            // look
                                                                                            // through
                HeaderNames.ACCEPT_ENCODING,// the header to look for
                HeaderNames.DEFLATE_VALUE); // the value to look for
    }

    /**
     * Returns whether or not this is connection passed the headers to be
     * considered a "good" ultrapeer.
     * 
     * @return <tt>true</tt> if this is considered a "good" ultrapeer, otherwise
     *         <tt>false</tt>
     */
    public boolean isGoodUltrapeer() {
        return GOOD_ULTRAPEER;
    }

    /**
     * Returns whether or not this connection supports query routing between
     * Ultrapeers at 1 hop.
     * 
     * @return <tt>true</tt> if this is an Ultrapeer connection that exchanges
     *         query routing tables with other Ultrapeers at 1 hop, otherwise
     *         <tt>false</tt>
     */
    public boolean isUltrapeerQueryRoutingConnection() {
        return ULTRAPEER_QRP;
    }

    /**
     * Returns true iff this connection wrote "X-Ultrapeer: false". This does
     * NOT necessarily mean the connection is shielded.
     */
    public boolean isLeaf() {
        return LEAF;
    }

    /** Returns true iff this connection wrote "X-Ultrapeer: true". */
    public boolean isUltrapeer() {
        return ULTRAPEER;
    }

    /**
     * Returns whether or not this connection is to a client supporting GUESS.
     * 
     * @return <tt>true</tt> if the node on the other end of this connection
     *         supports GUESS, <tt>false</tt> otherwise
     */
    public boolean isGUESSCapable() {
        return GUESS_CAPABLE;
    }

    /**
     * Returns whether or not this connection is to a ultrapeer supporting
     * GUESS.
     * 
     * @return <tt>true</tt> if the node on the other end of this Ultrapeer
     *         connection supports GUESS, <tt>false</tt> otherwise
     */
    public boolean isGUESSUltrapeer() {
        return isGUESSCapable() && isUltrapeer();
    }

    /**
     * Returns true iff this connection is a temporary connection as per the
     * headers.
     */
    public boolean isTempConnection() {
        // get the X-Temp-Connection from either the headers received
        String value = HEADERS.getProperty(HeaderNames.X_TEMP_CONNECTION);
        // if X-Temp-Connection header is not received, return false, else
        // return the value received
        if (value == null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Returns true if this supports GGEP'ed messages. GGEP'ed messages (e.g.,
     * big pongs) should only be sent along connections for which
     * supportsGGEP()==true.
     */
    public boolean supportsGGEP() {
        if (_supportsGGEP == null) {
            String value = HEADERS.getProperty(HeaderNames.GGEP);

            // Currently we don't care about the version number.
            _supportsGGEP = new Boolean(value != null);
        }
        return _supportsGGEP.booleanValue();
    }

    /**
     * Determines whether or not this node supports vendor messages.
     * 
     * @return <tt>true</tt> if this node supports vendor messages, otherwise
     *         <tt>false</tt>
     */
    public float supportsVendorMessages() {
        String value = HEADERS.getProperty(HeaderNames.X_VENDOR_MESSAGE);
        if ((value != null) && !value.equals("")) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Returns whether or not this node supports pong caching.
     * 
     * @return <tt>true</tt> if this node supports pong caching, otherwise
     *         <tt>false</tt>
     */
    public boolean supportsPongCaching() {
        return PONG_CACHING;
    }

    public String getVersion() {
        return HEADERS.getProperty(HeaderNames.X_VERSION);
    }

    /**
     * True if the remote host supports query routing (QRP). This is only
     * meaningful in the context of leaf-supernode relationships.
     */
    public boolean isQueryRoutingEnabled() {
        return isVersionOrHigher(HEADERS, HeaderNames.X_QUERY_ROUTING, 0.1F);
    }

    /**
     * Returns whether or not the node on the other end of this connection uses
     * dynamic querying.
     * 
     * @return <tt>true</tt> if this node uses dynamic querying, otherwise
     *         <tt>false</tt>
     */
    public boolean isDynamicQueryConnection() {
        return DYNAMIC_QUERY;
    }

    /**
     * Accessor for whether or not this connection supports TTL=1 probe queries.
     * These queries are treated separately from other queries. In particular,
     * if a second query with the same GUID is received, it is not considered a
     * duplicate.
     * 
     * @return <tt>true</tt> if this connection supports probe queries,
     *         otherwise <tt>false</tt>
     */
    public boolean supportsProbeQueries() {
        return PROBE_QUERIES;
    }

    /**
     * Determines whether or not this handshake is from the crawler.
     * 
     * @return <tt>true</tt> if this handshake is from the crawler, otherwise
     *         <tt>false</tt>
     */
    public boolean isCrawler() {
        return IS_CRAWLER;
    }

    /**
     * Access the locale preferences advertised by the client.
     */
    public String getLocalePref() {
        return LOCALE_PREF;
    }

    /** Accessor for the listening port. */
    public int getListeningPort() {
        return LISTEN_PORT;
    }

    /**
     * Convenience method that returns whether or not the given header exists.
     * 
     * @return <tt>true</tt> if the header exists, otherwise <tt>false</tt>
     */
    private static boolean headerExists(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        return value != null;
    }

    /**
     * Utility method for checking whether or not a given header value is true.
     * 
     * @param headers the headers to check
     * @param headerName the header name to look for
     */
    private static boolean isTrueValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        if (value == null)
            return false;

        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Utility method for checking whether or not a given header value is false.
     * 
     * @param headers the headers to check
     * @param headerName the header name to look for
     */
    private static boolean isFalseValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        if (value == null)
            return false;
        return value.equalsIgnoreCase("false");
    }

    /**
     * Utility method for determining whether or not a given header is a given
     * string value. Case-insensitive.
     * 
     * @param headers the headers to check
     * @param headerName the headerName to look for
     * @param headerValue the headerValue to check against
     */
    private static boolean isStringValue(Properties headers, String headerName, String headerValue) {
        String value = headers.getProperty(headerName);
        if (value == null)
            return false;
        return value.equalsIgnoreCase(headerValue);
    }

    /**
     * Utility method for determining whether or not a given header contains a
     * given string value within a comma-delimited list. Case-insensitive.
     * 
     * @param headers the headers to check
     * @param headerName the headerName to look for
     * @param headerValue the headerValue to check against
     */
    private static boolean containsStringValue(Properties headers, String headerName,
            String headerValue) {
        String value = headers.getProperty(headerName);
        if (value == null)
            return false;

        // As a small optimization, we first check to see if the value
        // by itself is what we want, so we don't have to create the
        // StringTokenizer.
        if (value.equalsIgnoreCase(headerValue))
            return true;

        StringTokenizer st = new StringTokenizer(value, ",");
        while (st.hasMoreTokens()) {
            if (st.nextToken().equalsIgnoreCase(headerValue))
                return true;
        }
        return false;
    }

    /**
     * Utility method that checks the headers to see if the advertised version
     * for a specified feature is greater than or equal to the version we
     * require (<tt>minVersion</tt>.
     * 
     * @param headers the connection headers to evaluate
     * @param headerName the header name for the feature to check
     * @param minVersion the minimum version that we require for this feature
     * 
     * @return <tt>true</tt> if the version number for the specified feature is
     *         greater than or equal to <tt>minVersion</tt>, otherwise
     *         <tt>false</tt>.
     */
    private static boolean isVersionOrHigher(Properties headers, String headerName, float minVersion) {
        String value = headers.getProperty(headerName);
        if (value == null)
            return false;
        try {
            Float f = new Float(value);
            return f.floatValue() >= minVersion;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Helper method for returning an int header value. If the header name is
     * not found, or if the header value cannot be parsed, the default value is
     * returned.
     * 
     * @param headers the connection headers to search through
     * @param headerName the header name to look for
     * @param defaultValue the default value to return if the header value could
     *        not be properly parsed
     * @return the int value for the header
     */
    private static int extractIntHeaderValue(Properties headers, String headerName, int defaultValue) {
        String value = headers.getProperty(headerName);

        if (value == null)
            return defaultValue;
        try {
            return Integer.valueOf(value).intValue();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Helper method for returning an int header value in a header after a
     * certain character. If the header name is not found, or if the header
     * value cannot be parsed, the default value is returned.
     * 
     * @param headers the connection headers to search through
     * @param headerName the header name to look for
     * @param token the token after which the int is looked for
     * @param defaultValue the default value to return if the header value could
     *        not be properly parsed
     * @return the int value for the header
     */
    private static int extractIntHeaderValueAfter(Properties headers, String headerName,
            String token, int defaultValue) {
        String value = headers.getProperty(headerName);

        if (value == null)
            return defaultValue;

        int idx = value.indexOf(token) + 1;
        if (idx == 0 || idx == value.length())
            return defaultValue;

        try {
            return Integer.valueOf(value.substring(idx)).intValue();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Helper method for returning a byte header value. If the header name is
     * not found, or if the header value cannot be parsed, the default value is
     * returned.
     * 
     * @param headers the connection headers to search through
     * @param headerName the header name to look for
     * @param defaultValue the default value to return if the header value could
     *        not be properly parsed
     * @return the byte value for the header
     */
    private static byte extractByteHeaderValue(Properties headers, String headerName,
            byte defaultValue) {
        String value = headers.getProperty(headerName);

        if (value == null)
            return defaultValue;
        try {
            return Byte.valueOf(value).byteValue();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Helper method for returning a string header value. If the header name is
     * not found, or if the header value cannot be parsed, the default value is
     * returned.
     * 
     * @param headers the connection headers to search through
     * @param headerName the header name to look for
     * @return the string value for the header, or the empty string if the
     *         header could not be found
     */
    private static String extractStringHeaderValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);

        if (value == null)
            return "";
        return value;
    }

    @Override
    public String toString() {
        return "<" + STATUS_CODE + ", " + STATUS_MESSAGE + ">" + HEADERS;
    }
}
