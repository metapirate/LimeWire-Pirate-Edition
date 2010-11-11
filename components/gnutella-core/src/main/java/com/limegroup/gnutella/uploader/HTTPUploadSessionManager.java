package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Defines the callback interface for response handlers to the upload manager
 * which provides methods for managing uploaders. 
 */
public interface HTTPUploadSessionManager {
    
    enum QueueStatus { UNKNOWN, BYPASS, REJECTED, QUEUED, ACCEPTED, BANNED };

    /**
     * Returns an uploader for <code>request</code>. If the previous request
     * was for <code>filename</code> as well an existing uploader is returned;
     * otherwise a new uploader is created.
     */
    HTTPUploader getOrCreateUploader(HttpRequest request, HttpContext context,
            UploadType type, String filename);
    
    /**
     * Returns an uploader for <code>request</code>. If the previous request
     * was for <code>filename</code> as well an existing uploader is returned;
     * otherwise a new uploader is created. FriendID may be null if this uploader
     * is associated with a gnutella Upload.
     */
    HTTPUploader getOrCreateUploader(HttpRequest request, HttpContext context,
            UploadType type, String filename, String friendID);

    /**
     * Add <code>request</code> to the queue of uploaders.
     * 
     * @return the queue status
     * @see UploadSlotManager
     */
    QueueStatus enqueue(HttpContext context, HttpRequest request);

    /**
     * Adds an accepted HTTPUploader to the internal list of active downloads.
     */
    void addAcceptedUploader(HTTPUploader uploader, HttpContext context);

    /**
     * Adds <code>uploader</code> to the GUI if it is not visible, yet, and
     * increments the attempted uploads.
     */
    void sendResponse(HTTPUploader uploader, HttpResponse response);

    /**
     * Sets a response code and entity on <code>response</code> for handling
     * requests from unsupported clients.
     */
    void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException, IOException;
    
}
