package org.limewire.http.protocol;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.AsyncNHttpServiceHandler;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;
import org.limewire.http.reactor.HttpIOSession;

/**
 * An extension to {@link AsyncNHttpServiceHandler} to allow more fine-grained
 * control.
 */
public class ExtendedAsyncNHttpServiceHandler extends AsyncNHttpServiceHandler {
    
    public ExtendedAsyncNHttpServiceHandler(
            final HttpProcessor httpProcessor, 
            final HttpResponseFactory responseFactory,
            final ConnectionReuseStrategy connStrategy,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(httpProcessor, responseFactory, connStrategy, allocator, params);
    }

    public ExtendedAsyncNHttpServiceHandler(
            final HttpProcessor httpProcessor, 
            final HttpResponseFactory responseFactory,
            final ConnectionReuseStrategy connStrategy,
            final HttpParams params) {
        this(httpProcessor, responseFactory, connStrategy, 
                new HeapByteBufferAllocator(), params);
    }

    public void setEventListener(final HttpServiceEventListener eventListener) {
        this.eventListener = eventListener;
    }
    
    @Override
    protected void responseComplete(HttpResponse response, HttpContext context) {
        NHttpServerConnection conn = (NHttpServerConnection) context
                .getAttribute(ExecutionContext.HTTP_CONNECTION);
        
        if(eventListener instanceof HttpServiceEventListener) {
            ((HttpServiceEventListener)eventListener).responseSent(conn, response);
        }
        
        if(conn.isOpen()) {
            final DefaultNHttpServerConnection c = (DefaultNHttpServerConnection)conn;
            // make sure any buffered requests are processed
            if (c.hasBufferedInput()) {
                HttpIOSession session = (HttpIOSession)c.getContext().getAttribute(DefaultDispatchedIOReactor.IO_SESSION_KEY);
                Executor executor = session.getIoExecutor();
                Runnable runner = new Runnable() {
                    public void run() {
                        c.consumeInput(ExtendedAsyncNHttpServiceHandler.this);
                    }
                };
                // By scheduling, we can force it to execute _after_ this.
                // Otherwise, things running after this could erase state this sets.
                if(executor instanceof ScheduledExecutorService) {
                    ((ScheduledExecutorService)executor).schedule(runner, 0, TimeUnit.MILLISECONDS);
                } else {
                    executor.execute(runner);
                }
            } 
        }        
    }
    
    @Override
    protected void closeConnection(NHttpConnection conn, Throwable cause) {
        // Make sure outgoing connections are cleaned up.
        ServerConnState state = (ServerConnState)conn.getContext().getAttribute(CONN_STATE);
        try {
            state.finishOutput();
        } catch(IOException ignored) {}
        
        super.closeConnection(conn, cause);
    }
    
    @Override
    protected void shutdownConnection(NHttpConnection conn, Throwable cause) {
        ServerConnState state = (ServerConnState)conn.getContext().getAttribute(CONN_STATE);
        try {
            state.finishOutput();
        } catch(IOException ignored) {}
        
        super.shutdownConnection(conn, cause);
    }
    
}
