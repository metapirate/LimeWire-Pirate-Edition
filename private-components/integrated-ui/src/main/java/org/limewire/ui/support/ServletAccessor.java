package org.limewire.ui.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.SimpleLimeHttpClient;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class handles accessing the servlet, sending it data about the client
 * configuration, and obtaining information about the next time this or any
 * bug can be sent.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class ServletAccessor {
    
    private static final Log LOG = LogFactory.getLog(ServletAccessor.class);
    
    @Inject private static volatile Provider<LimeHttpClient> limeHttpClient;
    
    /**
	 * Constant number of milliseconds to wait before timing out the
	 * connection to the servlet.
	 */
	private static final int CONNECT_TIMEOUT = 10 * 1000; // 10 seconds.
    
	/**
	 * Constant for the servlet url.
	 */
	private static final String DEFAULT_SERVLET_URL =
		"http://bugreports.limewire.com/bugs/servlet/BugHandler";
	
	private final String SERVLET_URL;
    
    /** Whether or not HttpClient can use NIO */
    private final boolean ALLOW_NIO;

	/** Constructs an accessor that can use NIO */
	ServletAccessor() {
	    this(true);
    }
    
    /** Constructs an accessor that may or may not use NIO.  Use true if it can. */
    ServletAccessor(boolean allowNIO) {
        this(allowNIO, DEFAULT_SERVLET_URL);        
    }
    
    /** Constructs an accessor that may or may not use NIO.  Use true if it can. 
     *  Also takes a URL value as a parameter to be assigned to SERVLET_URL */
    ServletAccessor(boolean allowNIO, String servlet_url) {
        this.ALLOW_NIO = allowNIO;
        SERVLET_URL = servlet_url;
    }

	/**
	 * Contacts the application servlet and sends it the information 
	 * contained in the <tt>LocalClientInfo</tt> object.  This method 
	 * also builds a <tt>RemoteClientInfo</tt> object from the information 
	 * obtained from the servlet.
	 *
	 * @return a <tt>RemoteClientInfo</tt> object that encapsulates the 
	 *         data about when to next send a bug.
	 * @param localInfo is an object encapsulating information about the
	 *                  local machine to send to the remote server
	 */
	synchronized RemoteClientInfo getRemoteBugInfo(LocalClientInfo localInfo) {
        RemoteClientInfo remoteInfo = new RemoteClientInfo();
        HttpResponse response = null;
        LimeHttpClient client = ALLOW_NIO && limeHttpClient != null ? limeHttpClient.get() : new SimpleLimeHttpClient();
        try {
            NameValuePair[] params = getNameValuePairs(localInfo.getPostRequestParams());
            HttpPost post = new HttpPost(SERVLET_URL);
            post.addHeader("Cache-Control", "no-cache");
            post.addHeader("User-Agent", LimeWireUtils.getHttpServer());
            post.addHeader("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            post.setEntity(new UrlEncodedFormEntity(Arrays.asList(params), "UTF-8"));

            HttpConnectionParams.setConnectionTimeout(client.getParams(), CONNECT_TIMEOUT);
            HttpClientParams.setRedirecting(client.getParams(), false);

            response = client.execute(post);
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            String body = result;
            if (LOG.isDebugEnabled())
                LOG.debug("Got response: " + response);
            // process results if valid status code
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
                remoteInfo.addRemoteInfo(body);
                // otherwise mark as server down.
            else {
                if (LOG.isWarnEnabled())
                    LOG.warn("Servlet connect failed, code: " +
                            response.getStatusLine().getStatusCode());
                remoteInfo.connectFailed();
            }
		} catch(IOException e) {
            fail(remoteInfo, e);
        } finally {
            client.releaseConnection(response);
        }
        return remoteInfo;
    }

    private void fail(RemoteClientInfo remoteInfo, Throwable e) {
        LOG.error("Error connecting to bug servlet", e);
        remoteInfo.connectFailed();
    }
    
    /**
     * Converts the specified array of map entries into an array of 
     * NameValuePair objects.
     */
    private NameValuePair[] getNameValuePairs(Map.Entry[] entries) {
        // Create result.
        NameValuePair[] pairs = new NameValuePair[entries.length];
        
        // Convert each Map.Entry into a NameValuePair.
        for (int i = 0; i < entries.length; i++) {
            Map.Entry entry = entries[i];
            pairs[i] = new BasicNameValuePair(String.valueOf(entry.getKey()),
                    String.valueOf(entry.getValue()));
        }
        
        return pairs;
    }
}
