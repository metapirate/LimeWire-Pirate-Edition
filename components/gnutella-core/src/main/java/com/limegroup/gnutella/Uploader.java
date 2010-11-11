package com.limegroup.gnutella;

import java.io.File;
import java.util.List;

import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.io.Connectable;

import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;

/**
 * This interface outlines the basic functionality for a class that performs
 * uploads.
 * <p>
 * A single instance should be reused for multiple chunks of a single file in an
 * HTTP/1.1 session. However, multiple HTTPUploaders should be used for multiple
 * files in a single HTTP/1.1 session.
 */
public interface Uploader extends BandwidthTracker, Connectable {
    
    public static enum UploadStatus {
        CANCELLED,
        CONNECTING,
        FREELOADER,
        LIMIT_REACHED,
        UPLOADING,
        COMPLETE,
        INTERRUPTED,
        FILE_NOT_FOUND,
        BROWSE_HOST,
        QUEUED,
        UPDATE_FILE,
        MALFORMED_REQUEST,
        PUSH_PROXY,
        UNAVAILABLE_RANGE,
        BANNED_GREEDY,
        THEX_REQUEST, 
        PAUSED 
    }
    
    /**
     * Marker string for bt-specific values.
     */
    public static final String BITTORRENT_UPLOAD = "";

    /**
	 * Stops this upload.  If the download is already 
	 * stopped, it does nothing.
	 */ 
	public void stop();
    
	/**
	 * returns the name of the file being uploaded.
	 */
	public String getFileName();
	
	/**
	 * returns the length of the file being uploaded.
	 */ 
	public long getFileSize();
	
	/**
	 * Returns the <tt>FileDesc</tt> of the file being uploaded.
	 *
	 * @return <tt>null</tt> if the file can not be found
	 */
	public FileDesc getFileDesc();

	/**
	 * returns the index of the file being uploaded.
	 */ 
	public int getIndex();

	/**
     * Returns the amount that of data that has been uploaded. For HTTP/1.1
     * transfers, this number is the amount uploaded for this specific chunk
     * only. Uses {@link #getTotalAmountUploaded()} for the entire amount
     * uploaded.
     * <p>
     * Note: This method was previously called "amountRead", but the name was
     * changed to make more sense.
	 */ 
	public long amountUploaded();
	
	/**
	 * Returns the amount of data that this uploader and all previous
	 * uploaders exchanging this file have uploaded.
	 */
	public long getTotalAmountUploaded();

	/**
	 * returns the string representation of the IP Address
	 * of the host being uploaded to.
	 */
	public String getHost();
	
	/**
	 * Returns the PresenceID for this Uploader. If this is
	 * not an Uploader for a Friend, this will return null.
	 */
	public String getPresenceId();
	
    /**
     * Returns the current state of this uploader.
     */
    public UploadStatus getState();
    
    /**
     * Returns the last transfer state of this uploader.
     * Transfers states are all states except INTERRUPTED, COMPLETE,
     * and CONNECTING.
     */
    public UploadStatus getLastTransferState();

	/**
	 * returns true if browse host is enabled, false if it is not.
	 */
	public boolean isBrowseHostEnabled();
	
	/**
	 * return the port of the gnutella-client host (not the HTTP port)
	 */
	public int getGnutellaPort();
	
	/** 
	 * return the userAgent
	 */
	public String getUserAgent();
	
    /**
     * Returns the current queue position if queued.
     */
    public int getQueuePosition();
    
    /**
     * Returns whether or not the uploader is in an inactive state.
     */
    public boolean isInactive();
    
    /**
     * @return a custom icon descriptor, null if the file icon should be
     * used.
     */
    public String getCustomIconDescriptor();
    
    /**
     * Returns the kind of upload this is
     * (shared file, browse host, malformed request, etc..)
     */
    public UploadType getUploadType();

    /**
     * Returns the file backing this uploader.
     * 
     * @return null if an entity different from a file is uploaded
     */
    public File getFile();
    
    /**
     * Returns the URN for the file backing this uploader. 
     */
    public URN getUrn();
    
    /**
     * Returns the number of connections we are currently uplaoding to. 
     */
    public int getNumUploadConnections();
    
    /**
     * Returns the seed ratio for torrent uploaders. Other uploaders will return -1 indicating the seed ratio is not supported. 
     */
    public float getSeedRatio();
    
    /**
     * Pauses the Uploader if possible.
     */
    public void pause();
    
    /**
     * Resumes the Uploader if possible.
     */
    public void resume();

    /**
     * Gets the download/upload sources for this upload.
     */
    public List<SourceInfo> getTransferDetails();
    
}

