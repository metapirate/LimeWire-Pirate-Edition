package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.limewire.collection.Cancellable;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


/**
 * Default implementation of <tt>HttpExecutor</tt>.
 */
@Singleton
public class DefaultHttpExecutor implements HttpExecutor {
    
    private static final Log LOG = LogFactory.getLog(DefaultHttpExecutor.class);

	private static final ExecutorService POOL = 
        ExecutorsHelper.newThreadPool("HttpClient pool");
    private final Provider<LimeHttpClient> clientProvider;
    private final HttpParams httpParams;

    @Inject
    public DefaultHttpExecutor(Provider<LimeHttpClient> clientProvider,
                               @Named("defaults") HttpParams httpParams) {
        this.clientProvider = clientProvider;
        this.httpParams = httpParams;
    }
    
    @Override
    public Shutdownable execute(HttpUriRequest method) {
        return execute(method, httpParams);
    }
    
    @Override
    public Shutdownable execute(HttpUriRequest method, HttpParams params) {
        return execute(method, params, new DefaultHttpClientListener());
    }
    
    @Override
    public Shutdownable execute(HttpUriRequest request, HttpClientListener listener) {
        return execute(request, httpParams, listener);
    }
	
    @Override
    public Shutdownable execute(HttpUriRequest method, HttpParams params, HttpClientListener listener) {
		return execute(method, params, listener, POOL);
	}

	private Shutdownable execute(final HttpUriRequest method, final HttpParams params, final HttpClientListener listener,
			ExecutorService executor) {
		
		Runnable r = new Runnable() {
            @Override
			public void run() {
				performRequest(method, params, listener);		
			}
		};
		executor.execute(r);
		return new Aborter(method);
	}
	
	private static class Aborter implements Shutdownable {
		private final AbortableHttpRequest toAbort;
		Aborter(HttpUriRequest toAbort) {
            if(toAbort instanceof AbortableHttpRequest) {
                this.toAbort = (AbortableHttpRequest)toAbort;
            } else {
                this.toAbort = null;
            }
        }
		
        @Override
        public void shutdown() {
            if(toAbort != null) {
                 toAbort.abort();
            }
        }
    }
	
    @Override
	public void releaseResources(HttpResponse response) {
        HttpClientUtils.releaseConnection(response);
	}

    @Override
	public Shutdownable executeAny(HttpClientListener listener, 
                        		   ExecutorService executor, 
                        		   Iterable<? extends HttpUriRequest> methods,
                                   HttpParams params,
                                   Cancellable canceller) {
		MultiRequestor r = new MultiRequestor(listener, methods, params, canceller);
		executor.execute(r);
		return r;
	}
	
	/**
     * Performs a single request.
     * Returns true if no more requests should be processed,
     * false if another request should be processed.
     */
	private boolean performRequest(HttpUriRequest method, HttpParams params, HttpClientListener listener) {
	    LOG.debugf("performing request, method: {0}, params: {1}", method, params); 
	    // If we aren't allowed to do this request, skip to the next.
	    if(!listener.allowRequest(method)) {
	        return false;
	    }
	    
		LimeHttpClient client = clientProvider.get();
        if(params != null) {
            client.setParams(params);
        }

        HttpResponse response;
        try {
			response = client.execute(method);
		} catch (IOException failed) {
		    LOG.debug("iox", failed);
			return !listener.requestFailed(method, null, failed);
		} catch (Throwable t) {
		    LOG.debug("throwable", t);
		    return !listener.requestFailed(method, null, new IOException(t));
		}
        return !listener.requestComplete(method, response);
	}
	
    /** Runs all requests until the listener told it to not do anymore. */
	private class MultiRequestor implements Runnable, Shutdownable {
		private boolean shutdown;
		private HttpUriRequest currentMethod;
		private final Iterable<? extends HttpUriRequest> methods;
		private final HttpClientListener listener;
        private HttpParams params;
        private final Cancellable canceller;
		
		MultiRequestor(HttpClientListener listener, 
				Iterable<? extends HttpUriRequest> methods, HttpParams params, Cancellable canceller) {
			this.methods = methods;
			this.listener = listener;
            this.params = params;
            this.canceller = canceller;
		}
		
        @Override
		public void run() {
			for (HttpUriRequest m : methods) {
				synchronized(this) {
					if (shutdown) {
					    LOG.debug("shut down in run");
						return;
					}
					currentMethod = m;
				}
				if (canceller.isCancelled()) {
				    LOG.debug("cancelled");
					return;
				}
				if (performRequest(m, params, listener))
					return;
			}
		}
		
		public void shutdown() {
			HttpUriRequest m;
			LOG.debug("shutting down");
			synchronized (this) {
				shutdown = true;
				m = currentMethod;
			}
            if(m instanceof AbortableHttpRequest) {
                ((AbortableHttpRequest)m).abort();
            }
        }
	}
    
    private class DefaultHttpClientListener implements HttpClientListener {
        @Override
        public boolean allowRequest(HttpUriRequest request) {
            return true;
        }

        @Override
        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            releaseResources(response);
            return false; 
        }

        @Override
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            releaseResources(response);
            return false;
        }
    }

}
