package org.limewire.http.protocol;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.ResponseConnControl;

/**
 * An extended connection control that does nothing if a response contains a
 * <tt>Connection</tt> header.
 */
public class LimeResponseConnControl extends ResponseConnControl {

    @Override
    public void process(HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        Header header = response.getFirstHeader(HTTP.CONN_DIRECTIVE);
        if (header == null || !HTTP.CONN_KEEP_ALIVE.equals(header.getValue())) {
            super.process(response, context);
        }
    }
    
}
