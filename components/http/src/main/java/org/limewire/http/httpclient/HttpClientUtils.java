package org.limewire.http.httpclient;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

public class HttpClientUtils {
    
    private static final Log LOG = LogFactory.getLog(HttpClientUtils.class);
    
    /** Ensures the response is consumed, so the connection can be recycled. */
    public static void releaseConnection(HttpResponse response) {
        if(response != null && response.getEntity() != null) {
            try {
                response.getEntity().consumeContent();
            } catch (IOException e) {
                LOG.error(e);
            }            
        }
    }

}
