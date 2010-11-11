package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.Address;

/**
 * The interface for a single download.  Observed by all downloads: gnutella, bittorrent, or other.
 */
public interface DownloadItem extends PropertiableFile {	

    public static final String DOWNLOAD_ITEM = "limewire.download.glueItem";
    public static final String DOWNLOAD_START_DATE = "limewire.download.startDate";

    /**
     * The type of downloader attached to this download.
     */
    public enum DownloadItemType {
        GNUTELLA,
        BITTORRENT,
        ANTIVIRUS
    }
    
    /**
     * Amalgamates the various specific error messages that a downloader could encounter
     *  into a general error applicable to any type of download and user under understandable.
     */
	public static enum ErrorState {
	    DISK_PROBLEM(I18nMarker.marktr("There is a disk problem")),
	    CORRUPT_FILE(I18nMarker.marktr("The file is corrupted")),
	    INVALID(I18nMarker.marktr("The copyright holder has not authorized this for sharing")),
	    UNABLE_TO_CONNECT(I18nMarker.marktr("Trouble connecting to people")),
	    
	    /**
         * The DownloadItem is not in an error state.
         */
	    NONE(I18nMarker.marktr("No problems."));
	    
	    private final String message;

        private ErrorState(String message) {
            this.message = message;
        }
        
        /**
         * Returns error state message marked for translation.
         */
        public String getMessage() {
            return message;
        }
    };
    
    /**
     * Returns the DownloadItemType for this downloader. Currently only broken up between gnutella and bittorrent,
     * All non bittorrent downloaders are still considered gnutella for consistency currently. 
     */
    public DownloadItemType getDownloadItemType();
    
    /**
     * Used to signify a "remainder" estimate that can not be calculated for 
     *  whatever reason.
     */
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;

    /**
     * Adds a listener for state changes affecting getState().
     */
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	/**
     * Removes an existing listener.
     */
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * @return the current state of the download.
	 */
	public DownloadState getState();

	public String getTitle();

	/**
	 * 
	 * @return the percentage between 0 and 100.
	 */
	public int getPercentComplete();

	/**
	 * @return size in bytes.
	 */
	public long getCurrentSize();
	
	/**
	 * @return number of bytes in the download that have been currently verified.
	 */
	public long getAmountVerified();
	
	/**
     * @return size in bytes.
     */
	public long getTotalSize();

	/**
	 * @return the number of bytes from the download lost to corruption.
	 */
	public long getAmountLost();
	
    /**
     * @return seconds remaining until the download will complete or <code>UNKNOWN</code> if unknown.
     */
	public long getRemainingDownloadTime();
	
    /**
     * @return seconds remaining until the state will change or <code>UNKNOWN</code> if unknown.
     */
    public long getRemainingTimeInState();

    /**
     * Initiates the cancel of this download item.
     */
	public void cancel();

	/**
	 * Pauses the download item if possible.
	 */
	public void pause();

	/**
	 * Resumes downloading of a paused item.
	 */
	public void resume();

	/**
	 * @return The number of sources (usually peers) associated with the download.
	 * 
	 * <p>NOTE: May include upload only sources.
	 */
	public int getDownloadSourceCount();

	/**
	 * @return A list of ip addresses of the peers associated with the download.
	 * 
	 * <p>NOTE: May include upload only sources.
	 */
	public List<Address> getSources();
	
	/**
	 * @return A list of details pertaining to the sources associated with the download.
	 * 
     * <p>NOTE: May include upload only sources.
	 */
	public List<SourceInfo> getSourcesDetails();
	
	/**
	 * @return A structure that holds the state of each download piece.
	 */
	DownloadPiecesInfo getPiecesInfo();
	
	/**
	 * @return The media type of the file being downloaded.
	 */
	public Category getCategory();

    /**
     * @return speed in kb/s or 0 if speed could not be measured.
     */
	public float getDownloadSpeed();
	
	/** 
	 * Returns a Collection of sources that support this download item. Each
	 * soure is represented by a RemoteHost object.
	 */
	public Collection<RemoteHost> getRemoteHosts();

	/**
     * Returns the position of the download on the uploader, relevant only if
     * the downloader is remote queued.
     */
    public int getRemoteQueuePosition();
  
    /**
     * @return The current {@link ErrorState}, {@link ErrorState#NONE} if no error.
     */
    public ErrorState getErrorState();
    
    
    /**
     * TODO: javadoc?
     */
    public int getLocalQueuePriority();
    
    /**
     * @return if this downloader can be launched or previewed.
     */
    public boolean isLaunchable();
    
    /**
     * @return the File that is being downloaded to.
     * This call never blocks, but may return a file
     * that is locked and cannot be used by other programs.
     */
    File getDownloadingFile();
    
    /**
     * Returns a file suitable for launching.
     * This call may block while the file is created.
     */
    File getLaunchableFile();
    
    /**
     * Sets the destination path and file name for the download.
     */
    void setSaveFile(File saveFile, boolean overwrite) throws DownloadException;
    
    /** Returns the file under which the download will be saved when complete.  
     * Counterpart to setSaveFile. */
    public File getSaveFile();

    /** @return true if {@link #resume()} will search for more sources. */
    boolean isTryAgainEnabled(); 
    
    /**@return The date the download started*/
    Date getStartDate();
    
    /** @return true if this can have its saveLocation changed. */
    public boolean isRelocatable();

    /**
     * Returns a list of the completed download files for this DownloadItem. 
     */
    public Collection<File> getCompleteFiles();
    
    /**
     * Returns the given Download property that matches the DownloadPropertyKey, Null is returned if the 
     * property does not exist for this DownloadItem. 
     */
    public Object getDownloadProperty(DownloadPropertyKey key);
}