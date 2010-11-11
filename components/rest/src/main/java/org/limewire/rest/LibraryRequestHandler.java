package org.limewire.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;

import com.google.inject.Inject;

/**
 * Request handler for Library services.
 */
class LibraryRequestHandler extends AbstractRestRequestHandler {

    private static final String METADATA = "";
    private static final String FILES = "/files";
    private static final int MAX_LIMIT = 50;
    
    private final LibraryManager libraryManager;
    
    @Inject
    public LibraryRequestHandler(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        String method = request.getRequestLine().getMethod();
        if (RestUtils.GET.equals(method)) {
            // Get uri target.
            String uriTarget = RestUtils.getUriTarget(request, RestPrefix.LIBRARY.pattern());
            
            // Get query parameters.
            Map<String, String> queryParams = RestUtils.getQueryParams(request);
            
            // Set response.
            process(uriTarget, queryParams, response);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    /**
     * Processes the specified uri target and query parameters.
     */
    private void process(String uriTarget, Map<String, String> queryParams, HttpResponse response) 
            throws IOException {
        
        if (METADATA.equals(uriTarget)) {
            // Get library files.
            LibraryFileList fileList = libraryManager.getLibraryManagedList();
            
            try {
                // Create JSON result.
                JSONObject jsonObj = RestUtils.createLibraryJson(fileList);

                // Set response entity and status.
                HttpEntity entity = RestUtils.createStringEntity(jsonObj.toString());
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                
            } catch (JSONException ex) {
                throw new IOException(ex);
            }
            
        } else if (FILES.equals(uriTarget)) {
            // Get library files.
            LibraryFileList fileList = libraryManager.getLibraryManagedList();
            List<LocalFileItem> fileItemList = new ArrayList<LocalFileItem>(fileList.getModel());
            
            // Get query parameters.
            String offsetStr = queryParams.get("offset");
            String limitStr = queryParams.get("limit");
            int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;
            int limit = (limitStr != null) ? Math.min(Integer.parseInt(limitStr), MAX_LIMIT) : MAX_LIMIT;
            
            try {
                // Create JSON result with requested files.
                JSONArray jsonArr = new JSONArray();
                for (int i = offset, max = Math.min(offset + limit, fileItemList.size()); i < max; i++) {
                    LocalFileItem fileItem = fileItemList.get(i);
                    jsonArr.put(RestUtils.createFileItemJson(fileItem));
                }

                // Set response entity and status.
                HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString());
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                
            } catch (JSONException ex) {
                throw new IOException(ex);
            }
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
}
