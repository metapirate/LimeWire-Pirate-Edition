package com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.limewire.collection.Cancellable;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;

/**
 * Last-ditch bootstrapping method: HTTP. 
 */
class TcpBootstrapImpl implements TcpBootstrap {

    private static final Log LOG = LogFactory.getLog(TcpBootstrapImpl.class);

    private final ExecutorService bootstrapQueue =
        ExecutorsHelper.newProcessingQueue("TCP Bootstrap");    
    private final HttpExecutor httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final ConnectionServices connectionServices;
    private final List<URI> hosts = new ArrayList<URI>();

    @Inject
    TcpBootstrapImpl(HttpExecutor httpExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            ConnectionServices connectionServices) {
        this.httpExecutor = httpExecutor;
        this.defaultParams = defaultParams;
        this.connectionServices = connectionServices;
        String[] servers = ConnectionSettings.BOOTSTRAP_SERVERS.get();
        for(String server : servers) {
            add(URI.create(server));
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Loaded " + servers.length + " bootstrap servers");
    }

    boolean add(URI uri) {
        return hosts.add(uri);
    }

    @Override
    public synchronized boolean fetchHosts(Bootstrapper.Listener listener) {
        List<HttpUriRequest> requests = new ArrayList<HttpUriRequest>();
        Map<HttpUriRequest, URI> requestToHost = new HashMap<HttpUriRequest, URI>();
        for(URI host : hosts) {
            HttpUriRequest request = newRequest(host);
            requests.add(request);
            requestToHost.put(request, host);
        }

        if(requests.isEmpty()) {
            LOG.debug("No TCP host caches to try");
            return false;
        }

        HttpParams params = new BasicHttpParams();
        params = new DefaultedHttpParams(params, defaultParams.get());

        if(LOG.isDebugEnabled())
            LOG.debug("Trying 1 of " + requests.size() + " TCP host caches");

        httpExecutor.executeAny(new Listener(requestToHost, listener),
                bootstrapQueue, requests, params,
                new Cancellable() {
            public boolean isCancelled() {
                return connectionServices.isConnected();
            }
        });
        return true;
    }

    private HttpUriRequest newRequest(URI host) {
        HttpGet get = new HttpGet(host);
        get.addHeader("Cache-Control", "no-cache");
        return get;
    }

    private int parseResponse(HttpResponse response, Bootstrapper.Listener listener) {
        if(response.getEntity() == null) {
            LOG.warn("No response entity!");
            return 0;
        }

        String line = null;
        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        try {
            InputStream in = response.getEntity().getContent();
            String charset = EntityUtils.getContentCharSet(response.getEntity());
            if(charset == null)
                charset = HTTP.DEFAULT_CONTENT_CHARSET;
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, charset));
            while((line = reader.readLine()) != null && line.length() > 0) {
                String[] words = line.split(",");
                if(words != null && words.length > 0) {
                    try {
                        Endpoint host = new Endpoint(words[0], true);
                        if(LOG.isDebugEnabled())
                            LOG.debug("Received " + host);
                        endpoints.add(host);
                    } catch(IllegalArgumentException e) {
                        LOG.error("Malformed line: " + line);
                    }
                }
            }
        } catch(IOException e) {
            LOG.error("IOX", e);
        }

        if(!endpoints.isEmpty()) {
            return listener.handleHosts(endpoints);
        } else {
            LOG.debug("No endpoints received");
            return 0;
        }
    }

    private class Listener implements HttpClientListener {
        private final Map<HttpUriRequest, URI> hosts;
        private final Bootstrapper.Listener listener;

        Listener(Map<HttpUriRequest, URI> hosts, Bootstrapper.Listener listener) {
            this.hosts = hosts;
            this.listener = listener;
        }

        @Override
        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            if(LOG.isDebugEnabled())
                LOG.debug("Completed request: " + request.getRequestLine());
            int received = parseResponse(response, listener);
            httpExecutor.releaseResources(response);
            return received < 10;
        }

        @Override
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Failed request: " + request.getRequestLine());
                if(response != null)
                    LOG.debug("Response " + response);
                if(exc != null)
                    LOG.debug(exc);
            }
            httpExecutor.releaseResources(response);
            return true;
        }

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            // Do not allow the request if we don't know about it
            synchronized(TcpBootstrapImpl.this) {
                return hosts.containsKey(request);
            }
        }
    }
}
