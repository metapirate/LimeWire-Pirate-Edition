package com.limegroup.gnutella.security;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class HttpCertificateReaderImpl implements HttpCertificateReader {

    private final Provider<LimeHttpClient> httpClient;
    private final CertificateParser certificateParser;

    @Inject
    public HttpCertificateReaderImpl(Provider<LimeHttpClient> httpClient, 
            CertificateParser certificateParser) {
        this.httpClient = httpClient;
        this.certificateParser = certificateParser;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.security.HttpCertificateReader#read(java.net.URI, org.limewire.io.IpPort)
     */
    public Certificate read(URI uri, IpPort messageSource) throws IOException {
        HttpGet get = new HttpGet(uri.toASCIIString());
        if (messageSource != null) {
            get.addHeader("X-Message-Source", messageSource.getAddress() + ":" + messageSource.getPort());
        }
        LimeHttpClient limeHttpClient = httpClient.get();
        HttpResponse response = null;
        try {
            response = limeHttpClient.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("could not get content from: " + uri);
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new IOException("no entity from: " + uri);
            }
            String contents = EntityUtils.toString(entity);
            return certificateParser.parseCertificate(contents);
        } finally {
            limeHttpClient.releaseConnection(response);
        }
    }
    
}
