package com.limegroup.gnutella.http;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class adds type safety for constant HTTP header values.  If there's
 * an HTTP header value that is constant, simply add it to this enumeration.
 */
public class ConstantHTTPHeaderValue {
		
    /** Accepting or encoding in deflate, in the Accept-Encoding or Content-Encoding fields. */
    public static final HTTPHeaderValue DEFLATE_VALUE = new SimpleHTTPHeaderValue("deflate");
        
    /** The 'close' value sent the server expects to close the connection. */
    public static final HTTPHeaderValue CLOSE_VALUE = new SimpleHTTPHeaderValue("close");
        
    /** The 'browse/version' value sent. */
    public static final HTTPHeaderValue BROWSE_FEATURE =
        new SimpleHTTPHeaderValue(HTTPConstants.BROWSE_PROTOCOL + "/" + HTTPConstants.BROWSE_VERSION);
        
    /** The 'chat/version' value sent. */
    public static final HTTPHeaderValue CHAT_FEATURE =
        new SimpleHTTPHeaderValue(HTTPConstants.CHAT_PROTOCOL + "/" + HTTPConstants.CHAT_VERSION);        
       
    /** The 'queue/version' value sent. */
    public static final HTTPHeaderValue QUEUE_FEATURE =
        new SimpleHTTPHeaderValue(HTTPConstants.QUEUE_PROTOCOL + "/" + HTTPConstants.QUEUE_VERSION);
    
    /** The queue version. */
    public static final HTTPHeaderValue QUEUE_VERSION =
        new SimpleHTTPHeaderValue("" + HTTPConstants.QUEUE_VERSION);
        
    /** The g2/version' value sent. */
    public static final HTTPHeaderValue G2_FEATURE =
        new SimpleHTTPHeaderValue(HTTPConstants.G2_PROTOCOL + "/" + HTTPConstants.G2_VERSION);
    
    /** The host sending this header would like to receive alternate locations behind firewalls. */
    public static final HTTPHeaderValue PUSH_LOCS_FEATURE =
    	new SimpleHTTPHeaderValue(HTTPConstants.PUSH_LOCS + "/" + HTTPConstants.PUSH_LOCS_VERSION);
    
    /**
     * The host sending this header supports
     * the designated version of Firewall to Firewall transfer, and is 
     * most likely firewalled.
     */
    public static final HTTPHeaderValue FWT_PUSH_LOCS_FEATURE =
    	new SimpleHTTPHeaderValue(HTTPConstants.FW_TRANSFER + "/" + HTTPConstants.FWT_TRANSFER_VERSION);
    
    /** The current User Agent. */
    public static final HTTPHeaderValue USER_AGENT =
        new HTTPHeaderValue() {
            public String httpStringValue() {
                return LimeWireUtils.getHttpServer();
            }
            
            @Override
            public String toString() {
                return httpStringValue();
            }
    };
    
    /** The current HTTP Server, as given in the "Server: " header. */
    public static final HTTPHeaderValue SERVER_VALUE = 
        new HTTPHeaderValue() {
            public String httpStringValue() {
                return LimeWireUtils.getHttpServer();
            }
            
            @Override
            public String toString() {
                return httpStringValue();
            }
    };
    
}
