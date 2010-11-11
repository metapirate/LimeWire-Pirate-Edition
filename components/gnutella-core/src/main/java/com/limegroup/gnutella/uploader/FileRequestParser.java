package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

/**
 * Provides methods for parsing Gnutella request URIs.
 */
class FileRequestParser {

    /** The type of the requested resource. */
    enum RequestType {
        /** Indicates a request for a file transfer. */ 
        FILE, 
        /** Indicates a request for a THEX tree. */ 
        THEX 
    }; 

    /**
     * Parses a URN request.
     * @param uri the <tt>String</tt> instance containing the get request
     * 
     * @return information about the requested file, <code>null</code> if the
     *         request type is invalid or the URN does not map to a valid file
     * @throws IOException thrown if the request is malformed
     * @throws HttpException 
     */
    public static FileRequest parseRequest(HttpRequestFileViewProvider fileListProvider, final String uri,
            HttpContext context) throws IOException, com.limegroup.gnutella.uploader.HttpException {
        
        // Only parse URI requests.
        int index = uri.toLowerCase(Locale.US).indexOf("/uri-res/");
        if(index == -1) {
            throw new IOException("invalid request");
        }
        
        String uriRes = uri.substring(index);
        
        URN urn = URN.createSHA1UrnFromHttpRequest(uriRes + " HTTP/1.1");
    
        // Parse the service identifier, whether N2R, N2X or something
        // we cannot satisfy. URI scheme names are not case-sensitive.
        RequestType requestType;
        String requestUpper = uriRes.toUpperCase(Locale.US);
        if (requestUpper.indexOf(HTTPConstants.NAME_TO_THEX) > 0) {
            requestType = RequestType.THEX;
        } else if (requestUpper.indexOf(HTTPConstants.NAME_TO_RESOURCE) > 0) {
            requestType = RequestType.FILE;
        } else {
            return null;
        }
    
        FileDesc desc = null;
        String friendID = index == 0 ? null : parseFriendId(uri.substring(0, index));
        for (FileView fileList : fileListProvider.getFileViews(friendID, context)) {
            desc = fileList.getFileDesc(urn);
            if (desc != null) {
                break;
            }
        }
        if(desc == null) {
            return null;
        } else {
            return new FileRequest(desc, requestType, friendID);
        }
    }

    static String parseFriendId(String uriString) throws com.limegroup.gnutella.uploader.HttpException {
        try {
            URI uri = new URI(uriString);
            String path = uri.getPath();
            if (path == null) {
                throw new com.limegroup.gnutella.uploader.HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
            }
            if (path.endsWith("/")) {
                int previousSlash = path.lastIndexOf('/', path.length() - 2);
                if (previousSlash != -1) {
                    return path.substring(previousSlash + 1, path.length() - 1);
                }
            } else {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    return path.substring(lastSlash + 1);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        throw new com.limegroup.gnutella.uploader.HttpException("no friend id:", HttpStatus.SC_BAD_REQUEST);
    }

    /** Record for storing information about a file request. */
    static class FileRequest {
        
        private final FileDesc fileDesc;
        
        private final String friendID;
    
        /** Type of the requested resource. */ 
        private final RequestType requestType;
    
        public FileRequest(FileDesc fileDesc, RequestType requestType, String friendID) {
            this.fileDesc = fileDesc;
            this.requestType = requestType;
            this.friendID = friendID;
        }
    
        public boolean isThexRequest() {
            return this.requestType == RequestType.THEX;
        }
        
        public FileDesc getFileDesc() {
            return fileDesc;
        }
        
        public String getFriendID() {
            return friendID;
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
    }

    
}