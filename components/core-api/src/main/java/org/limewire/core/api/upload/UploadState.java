package org.limewire.core.api.upload;

/**
 * The states related to managed uploads encapsulated in {@link UploadItem}.  
 */
public enum UploadState {
    
    /**
     * Upload is being queued.
     */
    QUEUED(false, false), 
    
    /**
     * Upload completed.
     */
    DONE(true, false), 
    
    /**
     * Upload in progress.
     */
    UPLOADING(false, false), 
    
    /**
     * Upload was stopped due to an error with the request.
     */
    REQUEST_ERROR(false, true), 
    
    /**
     * Upload was stopped because a limit was reached.
     */
    LIMIT_REACHED(false, true), 
    
    /**
     * Upload cancelled by user.
     */
    CANCELED(true, false), 
    
    /**
     * Only related to bittorrent?
     * <p>{@link UploadStatus.WAITING_REQUESTS}
     */
    PAUSED(false, false), 
    
    /**
     * This upload corresponds to a a live browse host.
     */
    BROWSE_HOST(false, false), 
    
    /**
     * The upload was a browse host, but is now complete.
     */
    BROWSE_HOST_DONE(true, false);
    
    private final boolean finished;
    private final boolean error;
    
    /**
     * Constructs an UploadState with the specified values.
     */
    UploadState(boolean finished, boolean error) {
        this.finished = finished;
        this.error = error;
    }
    
    /**
     * Returns true if the state represents a finished condition.
     */
    public boolean isFinished() {
        return finished;
    }
    
    /**
     * Returns true if the state represents an error.
     */
    public boolean isError() {
        return error;
    }
}
