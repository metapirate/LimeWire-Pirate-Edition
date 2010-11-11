package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;

/**
 * Provides names for the headers used in the gnutella connection handshake.
 * 
 * @author Anurag Singla
 */
public final class HeaderNames {

    /**
     * Private constructor to ensure that other classes cannot mistakenly
     * construct an instance of this class.
     */
    private HeaderNames() {
    }

    /** Obsolete; use X_LISTEN_IP instead. */
    public static final String X_MY_ADDRESS = "X-My-Address";

    /** The replacement for X_MY_ADDRESS. */
    public static final String LISTEN_IP = "Listen-IP";

    public static final String X_ULTRAPEER = "X-Ultrapeer";

    public static final String X_TRY_ULTRAPEERS = "X-Try-Ultrapeers";

    public static final String X_QUERY_ROUTING = "X-Query-Routing";

    /**
     * Constant string for the header indicating support for pong caching.
     */
    public static final String X_PONG_CACHING = "Pong-Caching";

    public static final String X_ULTRAPEER_NEEDED = "X-Ultrapeer-Needed";

    public static final String USER_AGENT = "User-Agent";

    public static final String X_TEMP_CONNECTION = "X-Temp-Connection";

    public static final String REMOTE_IP = "Remote-IP";

    public static final String GGEP = "GGEP";

    /** Requerying exclamation. */
    public static final String X_REQUERIES = "X-Requeries";

    /**
     * Header name for the GUESS version.
     */
    public static final String X_GUESS = "X-Guess";

    public static final String X_VERSION = "X-Version";

    /**
     * Header name for the degree of intra-Ultrapeer connections the host tries
     * to maintain.
     */
    public static final String X_DEGREE = "X-Degree";

    /**
     * Header for the version of query routing supported at the Ultrapeer level.
     */
    public static final String X_ULTRAPEER_QUERY_ROUTING = "X-Ultrapeer-Query-Routing";

    /**
     * Constant for the header advertising support for vendor messages.
     */
    public static final String X_VENDOR_MESSAGE = "Vendor-Message";

    /**
     * Constant for the header advertising support of extendible probe queries.
     */
    public static final String X_PROBE_QUERIES = "X-Ext-Probes";

    /**
     * Send by new hosts using dynamic-query style searching to denote the
     * maximum TTL that should be sent to them. This is only for queries coming
     * directly from this host, and is affected by degree.
     */
    public static final String X_MAX_TTL = "X-Max-TTL";

    /**
     * Header to indicate the version of dynamic querying in use.
     */
    public static final String X_DYNAMIC_QUERY = "X-Dynamic-Querying";

    /**
     * Header to indicate the locale that the client is running.
     */
    public static final String X_LOCALE_PREF = "X-Locale-Pref";

    /**
     * Header for Content-Encoding. Useful because typing
     * HTTPHeaderName.CONTENT_ENCODING.httpStringValue() in all handshaking
     * classes is cumbersome.
     */
    public static final String CONTENT_ENCODING = HTTPHeaderName.CONTENT_ENCODING.httpStringValue();

    /**
     * Header for Accept-Encoding. Useful because typing
     * HTTPHeaderName.ACCEPT_ENCODING.httpStringValue() in all handshaking
     * classes is cumbersome.
     */
    public static final String ACCEPT_ENCODING = HTTPHeaderName.ACCEPT_ENCODING.httpStringValue();

    /**
     * The value for deflate -- the type of encoding we can read & write. Useful
     * because typing ConstantHTTPHeaderValue.DEFLATE_VALUE.httpStringValue() in
     * all handshaking classes is cumbersome.
     */
    public static final String DEFLATE_VALUE = ConstantHTTPHeaderValue.DEFLATE_VALUE
            .httpStringValue();

    /**
     * Constant for the header indicating that the crawler is connecting.
     */
    public static final String CRAWLER = "Crawler";

    /**
     * Constant for the header indicating the number of leaf connections to the
     * crawler.
     */
    public static final String LEAVES = "Leaves";

    /**
     * Constant for the header indicating the number of ultrapeer connections to
     * the crawler.
     */
    public static final String PEERS = "Peers";
}
