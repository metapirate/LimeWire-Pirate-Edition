package org.limewire.core.api.upload;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.transfer.SourceInfo;

/**
 * A single upload.
 */
public interface UploadItem extends PropertiableFile {
    
    public enum UploadItemType {
        GNUTELLA,
        BITTORRENT
    }
    
    public enum BrowseType {FRIEND, P2P, NONE}

    /**
     * Cancels the upload.
     */
    public void cancel();

    /**
     * @return the {@link UploadState} of the upload
     */
    public UploadState getState();
    
    /**
     * Returns the amount of data that this uploader and all previous
     * uploaders exchanging this file have uploaded. 
     */
    public long getTotalAmountUploaded();
    
    /**
     * @return the name of the file being uploaded.
     */
    public String getFileName();
    
    /**
     * @return the length of the file being uploaded.
     */ 
    public long getFileSize();
    
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * @return the <code>Category</code> of the File being uploaded
     */
    public Category getCategory();
    
    /**
     * Returns the source of this Upload.
     */
    public RemoteHost getRemoteHost();
    
    /**
     * Returns a renderer name of the RemoteHost.
     */
    public String getRenderName();
    
    /**
     * Returns the current queue position if queued.
     */
    public int getQueuePosition();

    public long getRemainingUploadTime();

    float getUploadSpeed();
    
    /**
     * Returns a collection of completed files for this upload item.
     */
    Collection<File> getCompleteFiles();

    /**
     * Returns the file backing this upload item. 
     */
    public File getFile();
    
    /**
     * Returns the type of this upload item.
     */
    public UploadItemType getUploadItemType();

    /**
     * Returns the number of connections we are currently uploading to.
     */
    public int getNumUploadConnections();

    /**
     * @return the {@link BrowseType} of the upload
     */
    BrowseType getBrowseType();
    
    /**
     * Returns the time the upload was added to the upload list.
     */
    long getStartTime();
    
    /**
     * Returns the seed ratio for torrent uploaders. Other uploaders will return -1 indicating the seed ratio is not supported. 
     */
    public float getSeedRatio();
    
    /**
     * Returns true if the upload is finished.
     */
    boolean isFinished();
    
    /**
     * Returns true if the upload has started.
     */
    boolean isStarted();
    
    /**
     * Pauses the UploadeItem if possible.
     */
    public void pause();
    
    /**
     * Resumes the UploadItem if possible.
     */
    public void resume();
    
    /**
     * @return A list of details pertaining to the sources associated with the download.
     * 
     * <p>NOTE: May include upload only sources.
     */
    public List<SourceInfo> getTransferDetails();
}
