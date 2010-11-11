package com.limegroup.gnutella;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.io.Address;
import org.limewire.listener.ListenerSupport;

import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Defines an interface for downloading a file. The user interface maintains a 
 * list of <code>Downloader</code>'s and uses its methods to stop and 
 * resume downloads. Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
public interface Downloader extends BandwidthTracker,
                                    ListenerSupport<DownloadStateEvent> {
    
    /**
     * Key for custom description of the inactivity state.
     */
    public static final String CUSTOM_INACTIVITY_KEY = "CIK";
    
    
    /**
     * Stops this download if it is not already stopped. 
     * @modifies this
     */
    public void stop();
    
    /**
     * Pauses this download.  If the download is already paused or stopped, does nothing.
     */
    public void pause();
    
    /**
     * Returns the amount read by this so far, in bytes.
     */
    public long getAmountRead();
    
    /**
     * Returns the size of this file in bytes, i.e., the total amount to
     * download or -1 if content length is unknown.
     */
    public long getContentLength();

    
    /**
     * Returns the state of the downloader.
     */
    public DownloadState getState();
    
    /**
     * Determines if the download is completed.
     */
    public boolean isCompleted();

    /**
     * Determines if this download is paused or not.
     */
    public boolean isPaused();
	
	/**
     * Determines if this can have its saveLocation changed.
     */
    public boolean isRelocatable();
    
    /**
     * Returns the inactive priority of this download.
     */
    public int getInactivePriority();

    /**
     * Resumes this.  If the download is GAVE_UP, tries all locations again and
     * @return true If WAITING_FOR_RETRY, forces the retry immediately and
     * returns true if some other downloader is currently downloading the
     * file, throws AlreadyDowloadingException.  If WAITING_FOR_USER, then
     * launches another query.  Otherwise does nothing and returns false. 
     * @modifies this 
     */
    public boolean resume();
    
    /**
     * Returns the file that this downloader is using.
     * This is useful for retrieving information from the file,
     * such as the icon.
     *
     * This should NOT be used for playing the file; instead,
     * use {@link #getDownloadFragment()} for the reasons described in that
     * method.
     */
    public File getFile();

    /**
     * If this download is not yet complete, returns a copy of the first
     * contiguous fragment of the incomplete file.  (The copying helps prevent
     * file locking problems.)  Returns null if the download hasn't started or
     * the copy failed.  If the download is complete, returns the saved file.
     *
     * @param listener a listener to be notified when the download fragment is
     * being scanned for viruses - may be null
     * @return the copied file fragment, saved file, or null 
     */
    public File getDownloadFragment(ScanListener listener);

    /**
     * Sets the directory where the file will be saved. If <code>saveDirectory</code> 
     * is null, the default save directory will be used.
     *
     * @param saveDirectory the directory where the file should be saved. null 
     * indicates the default.
     * @param fileName the name of the file to be saved in <code>saveDirectory</code>. 
     * null indicates the default.
     * @param overwrite is true if saving should be allowed to overwrite existing files
     * @throws DownloadException when the new file location could not be set
     */
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite) throws DownloadException;
    
    /** Returns the file under which the download will be saved when complete.  
     * Counterpart to setSaveFile. */
    public File getSaveFile();

    /**
     * Returns an upper bound on the amount of time (in seconds) this will stay 
     * in the current state.  Returns <code>Integer.MAX_VALUE</code> if unknown.
     */
    public int getRemainingStateTime();

    /**
     * @return the amount of data pending to be written on disk (i.e. in cache, queue)
     */
    public int getAmountPending();
    
    /**
     * @return the number locations from which this is currently downloading.
     * The number of hosts returned is only meaningful in the 
     * {@link DownloadState#DOWNLOADING} state.
     */
    public int getNumHosts();
	
	/**
	 * @return if this downloader can be launched.
	 */
	public boolean isLaunchable();

    /**
     * Handles the user's response to
     * DownloadCallback.promptAboutUnscannedPreview().
     * @param delete whether to discard the unscanned preview
     */
    public void discardUnscannedPreview(boolean delete);

    /**
     * Returns a list of all RemoteFileDescs currently
     * associated with this Download.
     */
	public List<RemoteFileDesc> getRemoteFileDescs();

    /**
     * Returns the position of the download on the uploader, relevant only if
     * the downloader is queued.
     */
    public int getQueuePosition();

    /**
     * @return the number of hosts we are remotely queued on. 
     */
    public int getQueuedHostCount();
	
	/**
	 * @return the amount of data that has been verified
	 */
	public long getAmountVerified();
	
	/**
	 * @return the chunk size for the given download
	 */
	public int getChunkSize();
	
	/**
	 * @return the amount of data lost due to corruption
	 */
	public long getAmountLost();

	/**
	 * Returns the sha1 urn associated with the file being downloaded, or
	 * <code>null</code> if there is none.
	 * @return
	 */
	public URN getSha1Urn();
    
    /**
     * Sets a new attribute associated with the download.
     * The attributes are used, for example, by GUI to store some extra
     * information about the download.
     * @param key A key used to identify the attribute.
     * @param value The value of the key.
     * @param serialize Whether the attribute should be serialized to disk.
     * @return A previous value of the attribute, or <code>null</code>
     *         if the attribute wasn't set.
     */
    public Object setAttribute( String key, Object value, boolean serialize );

    /**
     * Gets a value of attribute associated with the download.
     * The attributes are used, for example, by GUI to store some extra
     * information about the download.
     * @param key A key which identifies the attribute.
     * @return The value of the specified attribute,
     *         or <code>null</code> if value was not specified.
     */
    public Object getAttribute( String key );

    /**
     * Removes an attribute associated with this download.
     * @param key A key which identifies the attribute do remove.
     * @return A value of the attribute or <code>null</code> if
     *         attribute was not set.
     */
    public Object removeAttribute( String key );
    
    /** @return All sources as addresses. */
    public List<Address> getSourcesAsAddresses();
    
    /** @return All the sources and their details. */
    public List<SourceInfo> getSourcesDetails();
    
    /** @return A structure containing the state of each piece in the download. */
    public DownloadPiecesInfo getPieceInfo();
    
    /**
     * Deletes the incomplete files for this downloader.
     * The downloader should handle the possibility of this method being called multiple times.
     */
    public void deleteIncompleteFiles();

    /** Enumerates the various states of a download. */
    public static enum DownloadState {
        INITIALIZING,
        QUEUED,
        CONNECTING,
        DOWNLOADING,
        BUSY,
        COMPLETE,
        ABORTED,
        GAVE_UP,
        UNABLE_TO_CONNECT,
        DISK_PROBLEM,
        WAITING_FOR_GNET_RESULTS,
        CORRUPT_FILE,
        REMOTE_QUEUED,
        HASHING,
        SAVING,
        WAITING_FOR_USER,
        WAITING_FOR_CONNECTIONS,
        ITERATIVE_GUESSING,
        QUERYING_DHT,
        PAUSED,
        INVALID,
        RESUMING,
        DANGEROUS,
        SCANNING,
        THREAT_FOUND,
        SCAN_FAILED,
        APPLYING_ANTIVIRUS_DEFINITION_UPDATE
    }
    
    // FIXME: this seems like a clumsy way to track preview state
    interface ScanListener {
        public void scanStarted();
        public void scanStopped();
    }
}

