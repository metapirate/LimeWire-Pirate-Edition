package com.limegroup.gnutella.filters;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.core.settings.FilterSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;
import org.limewire.util.Visitor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;

/**
 * Manages a file containing blacklisted URNs, which is updated periodically
 * via HTTP. The manager's <code>iterator()</code> method can be used to read
 * the URNs from disk as base32-encoded strings.
 */
@EagerSingleton
class URNBlacklistManagerImpl implements URNBlacklistManager, Service {

    private static final Log LOG =
        LogFactory.getLog(URNBlacklistManagerImpl.class);

    private final Provider<HttpExecutor> httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final Provider<SpamServices> spamServices;
    private final AtomicBoolean updatedThisSession = new AtomicBoolean(false);

    @Inject
    URNBlacklistManagerImpl(Provider<HttpExecutor> httpExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            Provider<SpamServices> spamServices) {
        this.httpExecutor = httpExecutor;
        this.defaultParams = defaultParams;
        this.spamServices = spamServices;
    }

    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void start() {
        LOG.debug("Starting");
        long now = System.currentTimeMillis();
        if(now > FilterSettings.NEXT_URN_BLACKLIST_UPDATE.getValue())
            checkForUpdate();
        else
            LOG.debug("Too soon to check for an update");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getServiceName() {
        return "URNBlacklistManager";
    }

    /**
     * Loads and verifies the URN blacklist, then passes each successfully
     * loaded URN to the given visitor as a base32-encoded string. This method
     * blocks.
     */
    @Override
    public void loadURNs(Visitor<String> visitor) {
        byte[] buf = new byte[20];
        byte[] sig = new byte[SIG_LENGTH];
        RandomAccessFile in = null;

        // Fail fast if the file is fundamentally fubar
        File file = getFile();
        long length = file.length() - SIG_LENGTH;
        // The data excluding the signature should be a multiple of 20 bytes
        if(length <= 0 || length > MAX_LENGTH || length % 20 != 0) {
            LOG.debug("File is missing, empty, or an invalid size");
            invalidFile();
            return;
        }
        try {
            LOG.debug("Opening file");
            in = new RandomAccessFile(file, "r");
            // Initialise the signature verifier
            Signature signature = Signature.getInstance(SIG_ALGORITHM);
            byte[] keyBytes = Base32.decode(PUBLIC_KEY);
            KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey key = factory.generatePublic(keySpec);
            signature.initVerify(key);
            // Feed the data to the signature verifier
            for(long read = 0; read < length; read += buf.length) {
                // Unexpected EOF will throw an exception
                in.readFully(buf);
                signature.update(buf);
            }
            // Read the signature
            in.readFully(sig);
            // Verify the signature
            if(signature.verify(sig)) {
                LOG.debug("Valid signature");
            } else {
                LOG.debug("Invalid signature");
                invalidFile();
                return;
            }
            // Rewind and read the file again, passing URNs to the visitor
            in.seek(0);
            for(long read = 0; read < length; read += buf.length) {
                // Unexpected EOF will throw an exception
                in.readFully(buf);
                if(!visitor.visit(Base32.encode(buf)))
                    break; // The visitor's had enough
            }
        } catch(IOException e) {
            LOG.debug("Error loading URNs", e);
            invalidFile();
        } catch(GeneralSecurityException e) {
            LOG.debug("Error verifying URNs", e);
            invalidFile();
        } finally {
            IOUtils.close(in);
        }
    }
    
    private void invalidFile() {
        // The file is invalid - replace it with any available version
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.setValue(0);
        checkForUpdate();   
    }

    /**
     * Returns the file where the URN blacklist should be stored.
     * Package access for testing.
     */
    File getFile() {
        return new File(CommonUtils.getUserSettingsDir(), "urns.dat");
    }

    /**
     * Selects one of the update URLs at random, checks for an updated version
     * of the blacklist, and downloads it if available. No more than one check
     * will be performed per session.
     */
    private void checkForUpdate() {
        final String[] urls = FilterSettings.URN_BLACKLIST_UPDATE_URLS.get();
        if(urls.length == 0) {
            LOG.debug("No request URLs");
            // Pick a new update time, otherwise when the list of URLs is
            // updated everyone will hit the servers at once
            setNextUpdateTime();
            return;
        }
        if(updatedThisSession.getAndSet(true)) {
            LOG.debug("Already updated this session");
            return;
        }
        int random = (int)(Math.random() * urls.length);
        String url = urls[random];
        if(LOG.isDebugEnabled())
            LOG.debug("Sending request to " + url);
        sendRequest(new HttpHead(url));
    }

    /**
     * Sends an HTTP request.
     */
    private void sendRequest(HttpRequestBase request) {
        request.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        params = new DefaultedHttpParams(params, defaultParams.get());
        httpExecutor.get().execute(request, params, new RequestHandler());
    }

    /**
     * Updates the settings recording the last time an update check was
     * performed and the time of the next check.
     */
    private void setNextUpdateTime() {
        long now = System.currentTimeMillis();
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.setValue(now);
        // Choose a random interval between zero and the maximum
        long max = FilterSettings.MAX_URN_BLACKLIST_UPDATE_INTERVAL.getValue();
        long min = FilterSettings.MIN_URN_BLACKLIST_UPDATE_INTERVAL.getValue();
        long next = now + Math.max(min, (long)(Math.random() * max));
        if(LOG.isDebugEnabled())
            LOG.debug("Setting next update time to " + next);
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.setValue(next);
    }

    private class RequestHandler implements HttpClientListener {

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            return true;
        }

        @Override
        public boolean requestComplete(HttpUriRequest request,
                HttpResponse response) {
            String method = request.getMethod();
            if("HEAD".equals(method)) {
                LOG.debug("HEAD request completed");
                long modified = 0;
                Header header = response.getFirstHeader("Last-Modified");
                if(header == null || header.getValue() == null) {
                    LOG.debug("Response has no Last-Modified header");
                } else {
                    try {
                        String date = header.getValue();
                        modified = DateUtils.parseDate(date).getTime();
                    } catch(DateParseException e) {
                        LOG.debug("Error parsing date", e);
                    }
                }
                long last = FilterSettings.LAST_URN_BLACKLIST_UPDATE.getValue();
                // If the blacklist has been modified since the last check,
                // send a GET request to download the new blacklist
                if(modified > last) {
                    String url = request.getURI().toString();
                    sendRequest(new HttpGet(url));
                } else {
                    setNextUpdateTime();
                }
            } else if("GET".equals(method)) {
                LOG.debug("GET request completed");
                HttpEntity body = response.getEntity();
                if(body == null) {
                    LOG.debug("Response has no body");
                } else {
                    BufferedOutputStream out = null;
                    try {
                        out = new BufferedOutputStream(
                                new FileOutputStream(getFile()));
                        body.writeTo(out);
                        out.flush();
                        out.close();
                        spamServices.get().reloadSpamFilters();
                    } catch(IOException e) {
                        LOG.debug("Error saving URNs", e);
                    } finally {
                        IOUtils.close(out);
                    }
                }
                setNextUpdateTime();
            }
            return false; // Do not attempt any further requests.
        }

        @Override
        public boolean requestFailed(HttpUriRequest request,
                HttpResponse response, IOException e) {
            if(LOG.isDebugEnabled()) {
                String method = request.getMethod();
                String status = null;
                if(response != null)
                    status = response.getStatusLine().toString();
                LOG.debug(method + " request failed with status " + status, e);
            }
            setNextUpdateTime();
            return false; // Do not attempt any further requests.
        }
    }
}
