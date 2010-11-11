package com.limegroup.gnutella.http;

import org.apache.http.protocol.HttpContext;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;
import org.limewire.http.reactor.HttpIOSession;

/**
 * Provides methods to access or modify objects stored in an {@link HttpContext}.
 */
public class HttpContextParams {

    /** Key for the connection flags. */
    public final static String CONNECTION_DATA = "org.limewire.connectionData";

    /** Indicates a connection from the local network. */
    public static boolean isLocal(final HttpContext context) {
        Object o = context.getAttribute(CONNECTION_DATA);
        return (o != null) ? ((HTTPConnectionData) o).isLocal() : false;
    }

    /** Indicates a transfer that was pushed. */
    public static boolean isPush(final HttpContext context) {
        Object o = context.getAttribute(CONNECTION_DATA);
        return (o != null) ? ((HTTPConnectionData) o).isPush() : false;
    }

    /** Indicates a transfer from a firewalled peer. */
    public static boolean isFirewalled(final HttpContext context) {
        Object o = context.getAttribute(CONNECTION_DATA);
        return (o != null) ? ((HTTPConnectionData) o).isFirewalled() : false;
    }
    
    public static void setConnectionData(final HttpContext context, final HTTPConnectionData data) {
        context.setAttribute(CONNECTION_DATA, data);
    }

    public static void setIOSession(HttpContext context, HttpIOSession session) {
        context.setAttribute(DefaultDispatchedIOReactor.IO_SESSION_KEY, session);
    }
    
    public static HttpIOSession getIOSession(HttpContext context) {
        return (HttpIOSession) context.getAttribute(DefaultDispatchedIOReactor.IO_SESSION_KEY);
    }

}
