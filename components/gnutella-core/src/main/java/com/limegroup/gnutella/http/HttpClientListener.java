package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Callback used by an HttpExecutor in order to inform about the status
 * of arbitrary HttpMethod requests.
 */
public interface HttpClientListener {
    
    /** Returns true if HttpExecutor is allowed to execute this request, false if it should skip to the next. */
    public boolean allowRequest(HttpUriRequest request);
    
    /**
     * Notification that the HttpMethod completed.
     * Returns true if more requests should be processed, false otherwise.
     * (The return value only makes sense in the case that multiple methods
     *  are being handled by a single HttpClientListener.)
     * @param request the request that completed
     * @param response the response that completed
     * @return true if additional requests should be attempted. @see com.limegroup.gnutella.http.HttpExecutor.executeAny()
     */
	public boolean requestComplete(HttpUriRequest request, HttpResponse response);
    
    /**
     * Notification that the HttpMethod failed.
     * Returns true if more requests should be processed, false otherwise.
     * (The return value only makes sense in the case that multiple methods
     *  are being handled by a single HttpClientListener.)
     * @param request the request that failed
     * @param response the response that failed; may be null
     * @param exc the exception that occurs
     * @return true if additional requests should be attempted. @see com.limegroup.gnutella.http.HttpExecutor.executeAny()
     */
	public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc);
}
