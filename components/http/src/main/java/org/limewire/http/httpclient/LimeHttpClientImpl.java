package org.limewire.http.httpclient;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Provider;

/**
 * An <code>HttpClient</code> extension that supports utility methods defined
 * in <code>LimeHttpClient</code> and Socket "injection" as defined in
 * <code>SocketWrappingHttpClient</code> 
 */
class LimeHttpClientImpl extends DefaultHttpClient implements SocketWrappingHttpClient {
    private Credentials credentials;

    public void setSocket(Socket socket) {
        ((ReapingClientConnectionManager)getConnectionManager()).setSocket(socket);
    }

    public void releaseConnection(HttpResponse response) {
        close(response);
    }
    
    public LimeHttpClientImpl(ReapingClientConnectionManager manager, Provider<HttpParams> defaultParams) {
        super(manager, defaultParams.get());
    }

    /**
     * @return an <code>HttpRequestRetryHandler</code> that always returns
     * <code>false</code>
     */
    @Override
    protected HttpRequestRetryHandler createHttpRequestRetryHandler() {
        return new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                // when requests fail for unexpected reasons (eg., IOException), we don't 
                // want to blindly re-attempt 
                return false;
            }
        };
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    protected CredentialsProvider createCredentialsProvider() {
        return new CredentialsProvider() {
            public void setCredentials(AuthScope authscope, Credentials credentials) {
                throw new UnsupportedOperationException();
            }

            public Credentials getCredentials(AuthScope authscope) {
                return credentials;
            }

            public void clear() {
                credentials = null;
            }
        };
    }

    private void close(HttpResponse response) {
        HttpClientUtils.releaseConnection(response);
    }

        
}
