package org.limewire.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;

/**
 * Defines the requirements for that process {@link Header} objects. 
 */
public interface HeaderInterceptor {

    /**
     * Processes a header. 
     * 
     * @param header the header to process
     * @param context the context of the request or response header belongs to
     * @throws IOException thrown when a processing error occurs
     * @throws HttpException thrown when a processing error occurs
     */
    void process(Header header, HttpContext context) throws HttpException, IOException;
    
}
