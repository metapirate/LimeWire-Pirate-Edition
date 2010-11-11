package org.limewire.http.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.AuthScope;
import org.apache.http.impl.client.DefaultHttpClient;

public class SimpleLimeHttpClient extends DefaultHttpClient implements LimeHttpClient {
    private Credentials credentials;

    public void releaseConnection(HttpResponse response) {
        HttpClientUtils.releaseConnection(response);
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
                throw new UnsupportedOperationException();        
            }
        };
    }
}
