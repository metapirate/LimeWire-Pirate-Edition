package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.uploader.HTTPUploader;

/**
 * Collects the push endpoint information from an FWT-Node header and sets it on
 * an {@link HTTPUploader}.
 */
public class FWNodeInfoInterceptor implements HeaderInterceptor {

    private final HTTPUploader uploader;
    private final PushEndpointFactory pushEndpointFactory;

    public FWNodeInfoInterceptor(HTTPUploader uploader, PushEndpointFactory pushEndpointFactory) {
        this.uploader = uploader;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    public void process(Header header, HttpContext context) throws HttpException, IOException {
        readPushEndPoint(header);
    }

    void readPushEndPoint(Header header) {
        if (HTTPHeaderName.FW_NODE_INFO.matches(header)) {
           try {
               PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(header.getValue());
               uploader.setPushEndpoint(pushEndpoint);
               uploader.setBrowseHostEnabled(true);
           } catch (IOException e) { }            
        }
    }
    
}
