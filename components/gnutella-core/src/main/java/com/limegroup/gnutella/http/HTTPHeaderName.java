package com.limegroup.gnutella.http;

import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

/** All HTTPHeaderNames. */
public enum HTTPHeaderName {

    /** Header for new alternate file locations, as per new spec. */
    ALT_LOCATION("X-Alt"),

    /** Header for alternate locations behind firewalls. */
    FALT_LOCATION("X-Falt"),

    /** Header for bad alternate locations behind firewalls. */
    BFALT_LOCATION("X-NFalt"),

    /** Header that used to be used for alternate locations, as per HUGE v0.94. */
    OLD_ALT_LOCS("X-Gnutella-Alternate-Location"),

    /** Header for failed Alternate locations to be removed from the mesh. */
    NALTS("X-NAlt"),

    /** Header for specifying the URN of the file, as per HUGE v0.94. */
    GNUTELLA_CONTENT_URN("X-Gnutella-Content-URN"),

    /**
     * Header for specifying the URN of the file, as per the CAW spec at
     * http://www.open-content.net/specs/draft-jchapweske-caw-03.html .
     */
    CONTENT_URN("X-Content-URN"),

    /** Header for specifying the byte range of the content. */
    CONTENT_RANGE("Content-Range"),

    /** Header for specifying the type of content. */
    CONTENT_TYPE("Content-Type"),

    /** Header for specifying the length of the content, in bytes. */
    CONTENT_LENGTH("Content-Length"),

    /** Header for specifying the media types we'll accept. */
    ACCEPT("Accept"),

    /** Header for specifying the type of encoding we'll accept. */
    ACCEPT_ENCODING("Accept-Encoding"),

    /** Header for specifying the type of encoding we'll send. */
    CONTENT_ENCODING("Content-Encoding"),

    /** Response header for specifying the server name and version. */
    SERVER("Server"),

    /**
     * Header for specifying whether the connection should be kept alive or
     * closed when using HTTP 1.1.
     */
    CONNECTION("Connection"),

    /**
     * Header for specifying a THEX URI. THEX URIs are of the form:
     * <xmp>
     * 
     * X-Thex-URI: <URI> ; <ROOT>.
     * </xmp>
     * This informs the client where the full Tiger tree hash can be retrieved.
     */
    THEX_URI("X-Thex-URI"),

    /** Constant header for the date. */
    DATE("Date"),

    /**
     * Header for the available ranges of a file currently available, as
     * specified in the Partial File Sharing Protocol. This takes the save form
     * as the Content-Range header, as in:
     * <p>
     * 
     * X-Available-Ranges: bytes 0-10,20-30
     */
    AVAILABLE_RANGES("X-Available-Ranges"),

    /** Header for queued downloads. */
    QUEUE("X-Queue"),

    /**
     * Header for retry after. Useful for two things: 1) LimeWire can now be
     * queued in gtk-gnutella's PARQ 2) It's possible to tune the number of http
     * requests down when LimeWire is busy
     */
    RETRY_AFTER("Retry-After"),

    /**
     * Header for creation time. Allows the creation time of the file to
     * propagate throughout the network.
     */
    CREATION_TIME("X-Create-Time"),

    /**
     * Header for submitting supported features. Introduced by BearShare.
     * <p>
     * Example: X-Features: chat/0.1, browse/1.0, queue/0.1
     */
    FEATURES("X-Features"),

    /**
     * Header for updating the set of push proxies for a host. Defined in
     * section 4.2 of the Push Proxy proposal, v. 0.7.
     */
    PROXIES("X-Push-Proxy"),

    /** 
     * <xmp>
     * Header for sending your own "<ip>: <listening port>" 
     * </xmp>
     */
    NODE("X-Node"),

    /** Header for informing uploader about amount of already downloaded bytes. */
    DOWNLOADED("X-Downloaded"),

    /** Header for the content disposition. */
    CONTENT_DISPOSITION("Content-Disposition"),

    /** The current host. */
    HOST("Host"),

    /** The user agent. */
    USER_AGENT("User-Agent"),

    /** Request ranges. */
    RANGE("Range"),

    /** The chat status. */
    CHAT("Chat"),

    /** The port for firewalled transfers. */
    FWTPORT("X-FWTP"),

    /** The transfer encoding. */
    TRANSFER_ENCODING("Transfer-Encoding"),

    /** The firewalled push proxy info of this client. */
    FW_NODE_INFO("X-FW-Node-Info"),
    
    /** Header to signal interest in and support of non-metadata urns. **/
    NMS1("X-NMS1");
    /**
     * Constant for the HTTP header name as a string.
     */
    private final String NAME;

    /**
     * Constant for the lower-case representation of the header name.
     */
    private final String LOWER_CASE_NAME;

    /**
     * Private constructor for creating the "enum" of header names. Making the
     * constructor private also ensures that this class cannot be subclassed.
     * 
     * @param name the string header as it is written out to the network
     */
    private HTTPHeaderName(final String name) {
        NAME = name;
        LOWER_CASE_NAME = name.toLowerCase(Locale.US);
    }

    /**
     * Returns whether or not the start of the passed in string matches the
     * string representation of this HTTP header, ignoring case.
     * 
     * @param str the string to check for a match
     * @return <tt>true</tt> if the passed in string matches the string
     *         representation of this HTTP header (ignoring case), otherwise
     *         returns <tt>false</tt>
     */
    public boolean is(String str) {
        return str.toLowerCase(Locale.US).equals(LOWER_CASE_NAME);
    }

    /**
     * Returns whether or not the start of the passed in string matches the
     * string representation of this HTTP header, ignoring case.
     * 
     * @param str the string to check for a match
     * @return <tt>true</tt> if the passed in string matches the string
     *         representation of this HTTP header (ignoring case), otherwise
     *         returns <tt>false</tt>
     */
    public boolean matchesStartOfString(String str) {
        return str.toLowerCase(Locale.US).startsWith(LOWER_CASE_NAME);
    }

    /**
     * Accessor to obtain the string representation of the header as it should
     * be written out to the network.
     * 
     * @return the HTTP header name as a string
     */
    public String httpStringValue() {
        return NAME;
    }

    /**
     * Overrides Object.toString to give a more informative description of the
     * header.
     * 
     * @return the string description of this instance
     */
    @Override
    public String toString() {
        return NAME;
    }

    public boolean matches(Header header) {
        return header.getName().equalsIgnoreCase(NAME);
    }

    public Header create(String value) {
        return new BasicHeader(httpStringValue(), value);
    }

    public Header create(HTTPHeaderValue value) {
        return new BasicHeader(httpStringValue(), value.httpStringValue());
    }

}
