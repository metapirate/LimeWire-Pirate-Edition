package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Responds with an HTML page showing information. 
 */
public class FreeLoaderRequestHandler implements HttpRequestHandler {

    public static final String REDIRECT_URL = "http://www2.limewire.com/browser.htm";
        
    public static final String FREELOADER_RESPONSE_PAGE = "<html>\r\n"
            + "<head>\r\n"
            + "  <title>" + LimeWireUtils.getHttpServer() + "</title>\r\n"
            + "  <meta http-equiv=\"refresh\" content=\"0; URL=" + REDIRECT_URL + "\">\r\n"
            + "</head>\r\n"
            + "<body>\r\n"
            + "  <a href=\"" + REDIRECT_URL + "\">Please Share</a>\r\n"
            + "</body>\r\n" // 
            + "</html>\r\n";
    
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_OK);
        NStringEntity entity = new NStringEntity(FREELOADER_RESPONSE_PAGE);
        entity.setContentType(Constants.HTML_MIME_TYPE);
        response.setEntity(entity);
    }

}
