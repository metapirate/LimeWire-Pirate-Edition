package org.limewire.http;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpRequestHandlerRegistry;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.auth.AuthenticationInterceptor;
import org.limewire.http.protocol.ExtendedAsyncNHttpServiceHandler;
import org.limewire.http.protocol.HttpServiceEventListener;
import org.limewire.http.protocol.LimeResponseConnControl;
import org.limewire.http.protocol.SynchronizedHttpProcessor;
import org.limewire.http.protocol.SynchronizedNHttpRequestHandlerRegistry;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;
import org.limewire.http.reactor.DispatchedIOReactor;
import org.limewire.lifecycle.Service;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.NIODispatcher;
import org.limewire.service.ErrorService;

/**
 * Processes HTTP requests which are forwarded to {@link HttpRequestHandler}
 * objects that can be registered for a URL pattern.
 * <p>
 * The acceptor uses HttpCore and LimeWire's HTTP component for connection
 * handling. <code>BasicHttpAcceptor</code> needs to be started by invoking
 * {@link #start(ConnectionDispatcher)} in order to accept connection.
 */
public abstract class BasicHttpAcceptor implements ConnectionAcceptor, Service {

    private static final Log LOG = LogFactory.getLog(BasicHttpAcceptor.class);

    public static final String[] DEFAULT_METHODS = new String[] { "GET",
            "HEAD", "POST", };

    private final AuthenticationInterceptor authenticationInterceptor;
    
    private final String[] supportedMethods;

    private final NHttpRequestHandlerRegistry registry;

    private final SynchronizedHttpProcessor processor;

    private final List<HttpAcceptorListener> acceptorListeners = new CopyOnWriteArrayList<HttpAcceptorListener>();

    private final HttpParams params; 
    
    private DispatchedIOReactor reactor;

    private ConnectionEventListener connectionListener;

    private DefaultHttpResponseFactory responseFactory;

    private AtomicBoolean started = new AtomicBoolean();

    public BasicHttpAcceptor(HttpParams params,
                             AuthenticationInterceptor authenticationInterceptor,
                             String... supportedMethods) {
        this.params = params;
        this.authenticationInterceptor = authenticationInterceptor;
        this.supportedMethods = supportedMethods;
        
        this.registry = new SynchronizedNHttpRequestHandlerRegistry();
        this.processor = new SynchronizedHttpProcessor();
        
        initializeDefaultInterceptors();
    }
    
    private void initializeDefaultInterceptors() {
        // order doesn't play a role
        addRequestInterceptor(authenticationInterceptor);
        addResponseInterceptor(new ResponseDate());
        addResponseInterceptor(new ResponseServer());
        addResponseInterceptor(new ResponseContent());
        addResponseInterceptor(new LimeResponseConnControl());
    }

    public static HttpParams createDefaultParams(String userAgent, int timeout) {
        BasicHttpParams params = new BasicHttpParams();
        params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, timeout);
        params.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, timeout);
        // size of the per connection buffers used for headers and by the
        // decoder/encoder
        params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                8 * 1024);
        params.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true);
        params.setParameter(HttpProtocolParams.ORIGIN_SERVER, userAgent);
        params.setIntParameter(HttpConnectionParams.MAX_LINE_LENGTH, 4096);
        params.setIntParameter(HttpConnectionParams.MAX_HEADER_COUNT, 50);
        params.setParameter(HttpProtocolParams.HTTP_ELEMENT_CHARSET,
                HTTP.ISO_8859_1);

        return params;
    }
    
    /**
     * Note: Needs to be called from the NIODispatcher thread.
     */
    private void initializeReactor() {
        assert NIODispatcher.instance().isDispatchThread();
        
        this.connectionListener = new ConnectionEventListener();

        responseFactory = new DefaultHttpResponseFactory();

        ExtendedAsyncNHttpServiceHandler serviceHandler = new ExtendedAsyncNHttpServiceHandler(processor,
                responseFactory, new DefaultConnectionReuseStrategy(), params);
        serviceHandler.setEventListener(connectionListener);
        serviceHandler.setHandlerResolver(this.registry);

        this.reactor = new DefaultDispatchedIOReactor(params, NIODispatcher.instance().getScheduledExecutorService());
        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, params);
        try {
            this.reactor.execute(ioEventDispatch);
        } catch (IOException e) {
            // can not happen
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public void acceptConnection(String word, Socket socket) {
        reactor.acceptConnection(word + " ", socket);
    }

    public boolean isBlocking() {
        return false;
    }

    /**
     * Returns the supported HTTP methods, e.g. "GET" or "HEAD".
     */
    public String[] getHttpMethods() {
        return supportedMethods;
    }
    
    /**
     * Adds a listener for acceptor events.
     */
    public void addAcceptorListener(HttpAcceptorListener listener) {
        acceptorListeners.add(listener);
    }
    
    /**
     * Adds an interceptor for incoming requests. 
     * 
     * @see HttpProcessor
     */
    public void addRequestInterceptor(HttpRequestInterceptor interceptor) {
        processor.addInterceptor(interceptor);
    }
    
    /**
     * Adds an interceptor for outgoing responses. 
     * 
     * @see HttpProcessor
     */
    public void addResponseInterceptor(HttpResponseInterceptor interceptor) {
        processor.addInterceptor(interceptor);
    }    

    /**
     * Returns the reactor.
     * 
     * <p>Note: Needs to be called from the NIODispatcher thread.
     * 
     * @return null, if the acceptor has not been started, yet.
     */
    protected DispatchedIOReactor getReactor() {
        assert NIODispatcher.instance().isDispatchThread();
        
        return reactor;
    }

    /**
     * Removes <code>listener</code> from the list of acceptor listeners.
     * 
     * @see #addAcceptorListener(HttpAcceptorListener)
     */
    public void removeAcceptorListener(HttpAcceptorListener listener) {
        acceptorListeners.remove(listener);
    }

    /**
     * Removes an interceptor for incoming requests. 
     * 
     * @see #addRequestInterceptor(HttpRequestInterceptor)
     */
    public void removeRequestInterceptor(HttpRequestInterceptor interceptor) {
        processor.removeInterceptor(interceptor);
    }
    
    /**
     * Adds an interceptor for outgoing responses. 
     * 
     * @see #addResponseInterceptor(HttpResponseInterceptor)
     */
    public void removeResponseInterceptor(HttpResponseInterceptor interceptor) {
        processor.removeInterceptor(interceptor);
    }

    /**
     * Registers a request handler for a request pattern. See
     * {@link HttpRequestHandlerRegistry} for a description of valid patterns.
     * <p>
     * If a request matches multiple handlers, the handler with the longer
     * pattern is preferred.
     * <p>
     * Only a single handler may be registered per pattern.
     * 
     * @param pattern the URI pattern to handle requests for
     * @param handler the handler that processes the request
     */
    public void registerHandler(final String pattern,
            final NHttpRequestHandler handler) {
        registry.register(pattern, authenticationInterceptor.getGuardedHandler(pattern, handler));       
    }

    /**
     * Unregisters the handlers for <code>pattern</code>.
     * 
     * @see #registerHandler(String, HttpRequestHandler)
     */
    public void unregisterHandler(final String pattern) {
        authenticationInterceptor.unregisterHandler(pattern);
        registry.unregister(pattern);
    }

    /**
     * Initializes the reactor.
     * 
     * @see #stop()
     */
    public void start() {
        if (started.getAndSet(true)) {
            throw new IllegalStateException();
        }
        
        final AtomicBoolean inited = new AtomicBoolean(false);
        try {
            Future<?> result = NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
                public void run() {
                    initializeReactor();
                    inited.set(true);
                }
            });
            
            // wait for reactor to finish initialization
            result.get();
        } catch (InterruptedException e) {
            if (inited.get())
                LOG.warn("Interrupted while waiting for reactor initialization", e);
            else
                ErrorService.error(e); // this is a problem.
        } catch (ExecutionException e) {
            ErrorService.error(e);
        }
    }

    /**
     * @see #start()
     */
    public void stop() {
        if (!started.getAndSet(false)) {
            throw new IllegalStateException();
        }
    }
    
    public void initialize() {}
    
    public String getServiceName() {
        return null;
    }

    /**
     * Forwards events from the underlying protocol layer to acceptor event
     * listeners.
     */
    private class ConnectionEventListener implements HttpServiceEventListener {

        public void connectionOpen(NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionOpen(conn);
            }
        }

        public void connectionClosed(NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionClosed(conn);
            }
        }

        public void connectionTimeout(NHttpConnection conn) {
            // should never happen since LimeWire will close the socket on
            // timeouts which will trigger a connectionClosed() event
            throw new RuntimeException();
        }

        public void fatalIOException(IOException e, NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            LOG.debug("HTTP connection error", e);
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionClosed(conn);
            }
        }

        public void fatalProtocolException(HttpException e, NHttpConnection conn) {
            assert NIODispatcher.instance().isDispatchThread();
            
            LOG.debug("HTTP protocol error", e);
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.connectionClosed(conn);
            }
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            assert NIODispatcher.instance().isDispatchThread();
            
            for (HttpAcceptorListener listener : acceptorListeners) {
                listener.responseSent(conn, response);
            }
        }

    }

}
