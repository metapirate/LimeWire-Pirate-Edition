package org.limewire.http.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpParams;

/**
 * An extension to the <code>org.apache.http.client.HttpClient</code> interface to provide
 * helper methods.
 */
public interface LimeHttpClient extends HttpClient {
    /**
     * Sets <code>HttpParams<code>
     * @param params the params to use
     */
    public void setParams(HttpParams params);
    
    /**
     * Set credentials that are used by the credentials provider.
     */
    public void setCredentials(Credentials credentials);

    /**
     * Does any necessary cleanup to allow 
     * all underlying connections to be able to be reused or closed.
     * @param response the response to cleanup
     */
    public void releaseConnection(HttpResponse response);
}
