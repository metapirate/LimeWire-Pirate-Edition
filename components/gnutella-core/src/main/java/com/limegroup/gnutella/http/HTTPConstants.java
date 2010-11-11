package com.limegroup.gnutella.http;

import org.limewire.rudp.RUDPUtils;

/**
 * This class defines a set of constants for use in HTTP messages.
 */
public final class HTTPConstants {

    /**
     * Private constructor to ensure that this class cannot be instantiated.
     */
    private HTTPConstants() {
    }

    /**
     * Constant for the beginning "GET" of an HTTP URN get request.
     */
    public static final String GET = "GET";

    /**
     * Constant for the HTTP 1.0 specifier at the end of an HTTP URN get
     * request.
     */
    public static final String HTTP10 = "HTTP/1.0";

    /**
     * Constant for the HTTP 1.1 specifier at the end of an HTTP URN get
     * request.
     */
    public static final String HTTP11 = "HTTP/1.1";

    /**
     * Constant for the "uri-res" specifier for an HTTP URN get request.
     */
    public static final String URI_RES = "uri-res";

    /**
     * Constant for the "Name to Resource", or "N2R?" resolution service
     * identifier, as specified in RFC 2169.
     */
    public static final String NAME_TO_RESOURCE = "N2R?";

    /**
     * Constant for the "Name to THEX", or "N2X?" resolution service identifier,
     * as specified in the PFSP proposal.
     */
    public static final String NAME_TO_THEX = "N2X?";

    /**
     * Constant for the "uri-res" uri resolution specifier, followed by the
     * standard "/" and the resolution service id, in our case "N2R?".
     */
    public static final String URI_RES_N2R = "/" + URI_RES + "/" + NAME_TO_RESOURCE;

    /**
     * Constant for the "uri-res" uri resolution specifier, followed by the
     * standard "/" and the resolution service id, in this case "N2X?".
     */
    public static final String URI_RES_N2X = "/" + URI_RES + "/" + NAME_TO_THEX;

    /**
     * constant strings for the X-Feature header.
     */
    public static final String CHAT_PROTOCOL = "chat";

    public static final String BROWSE_PROTOCOL = "browse";

    public static final String QUEUE_PROTOCOL = "queue";

    public static final String G2_PROTOCOL = "g2";

    public static final String PUSH_LOCS = "fwalt";

    public static final String FW_TRANSFER = "fwt";

    public static final double CHAT_VERSION = 0.1;

    public static final double BROWSE_VERSION = 1.0;

    public static final double QUEUE_VERSION = 0.1;

    public static final double G2_VERSION = 1.0;

    public static final double PUSH_LOCS_VERSION = 0.1;

    // this is the same as the version of the Firewall-to-Firewall transfer
    public static final double FWT_TRANSFER_VERSION = RUDPUtils.VERSION;
}
