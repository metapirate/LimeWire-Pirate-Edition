package org.limewire.rest;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.URNFactory;
import org.limewire.http.handler.MimeTypeProvider;

import com.google.inject.Inject;

/**
 * Request handler for streaming services.
 */
class StreamRequestHandler extends AbstractRestRequestHandler {
    
    private final LibraryManager libraryManager;
    private final MimeTypeProvider mimeTypeProvider;
    private final URNFactory urnFactory;
    
    @Inject
    public StreamRequestHandler(LibraryManager libraryManager, MimeTypeProvider mimeTypeProvider, URNFactory urnFactory) {
        this.libraryManager = libraryManager;
        this.mimeTypeProvider = mimeTypeProvider;
        this.urnFactory = urnFactory;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        String method = request.getRequestLine().getMethod();
        if (RestUtils.GET.equals(method)) {
            // Get uri target.
            String uriTarget = RestUtils.getUriTarget(request, RestPrefix.STREAM.pattern());
            
            // Get query parameters.
            Map<String, String> queryParams = RestUtils.getQueryParams(request);
            
            // Set response.
            process(uriTarget, queryParams, response);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    private URN getSha1UrnStringFromUri(String uri) throws IOException {
        String sha1 = uri.substring(1);  // remove "/..."  
        return urnFactory.createSHA1Urn(sha1);
    }
    
    /**
     * Processes the specified uri target and query parameters.
     */
    private void process(String uriTarget, Map<String, String> queryParams, HttpResponse response) 
            throws IOException {
        URN urn;
        try {
            urn = this.getSha1UrnStringFromUri(uriTarget);
        } catch(IOException e) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            return;
        }
        
        // Get library file.
        LibraryFileList fileList = libraryManager.getLibraryManagedList();
        LocalFileItem fi = fileList.getFileItem(urn);
        
        if(fi == null || fi.getFile() == null) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        } else {
            File f = fi.getFile();

            String mimeType = mimeTypeProvider.getMimeType(f);

            HttpEntity entity = new NFileEntity(f, mimeType);
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }
}
