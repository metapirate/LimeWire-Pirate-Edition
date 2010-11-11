package com.limegroup.gnutella.uploader;

/**
 * An enumaration of the kinds of upload requests that
 * can be serviced.  Most of these are internal to the program
 * and shouldn't be shown to the user.
 */
public enum UploadType {    
    SHARED_FILE(false),
    BROWSE_HOST(false),
    PUSH_PROXY,
    UPDATE_FILE,
    MALFORMED_REQUEST,
    FILE_NOT_FOUND,
    FILE_VIEW,
    RESOURCE_FILE,
    BROWSER_CONTROL,
    HEAD_REQUEST,
    FORCED_SHARE;
    
    private final boolean internal;
    
    private UploadType() {
        this(true);
    }
    
    private UploadType(boolean internal) {
        this.internal = internal;
    }
    
    /** 
     * Determines if this kind of upload is an 'internal'
     * upload, ie: one that isn't from a user's shared file.
     */
    public boolean isInternal() {
        return internal;
    }

}
