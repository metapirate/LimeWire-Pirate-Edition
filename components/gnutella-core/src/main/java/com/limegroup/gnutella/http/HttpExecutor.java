package com.limegroup.gnutella.http;

import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.limewire.collection.Cancellable;
import org.limewire.nio.observer.Shutdownable;

/**
 * Something that executes HTTP requests using the http client library.
 */
public interface HttpExecutor {

    /**
     * Execute an http request asynchronously. A default
     * {@link org.apache.http.params.HttpParams} is used with a connection
     * timeout of 5s, socket timeout of 8s, and maximum redirects of 10.
     * 
     * @param request the request to execute
     * @return a {@link Shutdownable} to allow the request to be aborted.
     */
    Shutdownable execute(HttpUriRequest request);

    /**
     * Execute an http request asynchronously.
     * 
     * @param request the request to execute
     * @param params the {@link org.apache.http.params.HttpParams} to use
     * @return a {@link Shutdownable} to allow the request to be aborted.
     */
    Shutdownable execute(HttpUriRequest method, HttpParams params);

    /**
     * Execute an http request asynchronously.
     * 
     * @param request the request to execute
     * @param params the {@link org.apache.http.params.HttpParams} to use
     * @param listener the {@link HttpClientListener} to use
     * @return a {@link Shutdownable} to allow the request to be aborted.
     */
    public Shutdownable execute(HttpUriRequest method, HttpParams params,
            HttpClientListener listener);
    
    /**
     * Execute http request asynchronously.
     * @return a {@link Shutdownable} to allow the request to be aborted
     */
    public Shutdownable execute(HttpUriRequest request, HttpClientListener listener);

    /**
     * Tries to execute any of the methods until the HttpClientListener
     * instructs the executor to stop processing more, or the Cancellable
     * returns true for isCancelled.
     * <p>
     * This returns a Shutdownable that can be used to shutdown the execution of
     * requesting all methods, to stop the current processing.
     */
    public Shutdownable executeAny(HttpClientListener listener, ExecutorService executor,
            Iterable<? extends HttpUriRequest> methods, HttpParams params, Cancellable canceller);

    /**
     * Release any resources held by the provided method. The users of this
     * class must call this method once they're done processing their HttpMethod
     * object.
     */
    public void releaseResources(HttpResponse method);
}
