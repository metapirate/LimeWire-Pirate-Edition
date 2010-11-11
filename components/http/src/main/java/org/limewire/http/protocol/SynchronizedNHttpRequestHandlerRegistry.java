package org.limewire.http.protocol;

import java.util.Map;

import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpRequestHandlerRegistry;

public class SynchronizedNHttpRequestHandlerRegistry extends
        NHttpRequestHandlerRegistry {

    @Override
    public synchronized NHttpRequestHandler lookup(String requestURI) {
        return super.lookup(requestURI);
    }

    @Override
    public synchronized void register(String pattern, NHttpRequestHandler handler) {
        super.register(pattern, handler);
    }

    @Override
    public synchronized void setHandlers(Map<String, ? extends NHttpRequestHandler> map) {
        super.setHandlers(map);
    }

    @Override
    public synchronized void unregister(String pattern) {
        super.unregister(pattern);
    }

}
