package org.limewire.http.entity;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpRequest;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HTTP;

/**
 * An entity that provides a 404 error page in response to a request for a
 * non-existing document.
 */
public class NotFoundEntity extends NStringEntity {

    private static final String HTML_TEXT_TYPE = "text/html";

    private static String HEAD = //   
            "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + //
            "<html>\n" + //
            "<head><title>404 Not Found</title></head>\n" + //
            "<body><h1>Not Found</h1><p>The requested URL ";

    private static String TAIL = //
            " was not found on this server.</p></body></html>";

    public NotFoundEntity(String uri) throws UnsupportedEncodingException {
        super(HEAD + uri + TAIL, HTTP.ISO_8859_1);
        
        setContentType(HTML_TEXT_TYPE + HTTP.CHARSET_PARAM + HTTP.ISO_8859_1);
    }

    public NotFoundEntity(HttpRequest request) throws UnsupportedEncodingException {
        this(request.getRequestLine().getUri());
    }

}
