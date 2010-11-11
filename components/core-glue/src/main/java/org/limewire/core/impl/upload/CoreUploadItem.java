package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.bittorrent.BTUploader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;

class CoreUploadItem implements UploadItem {
    
    static interface Factory {
        CoreUploadItem create(@Assisted Uploader uploader, @Assisted FriendPresence friendPresence);
    }

    private final Uploader uploader;    
    private final FriendPresence friendPresence;
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    private final CategoryManager categoryManager;
    
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;
    private final UploadItemType uploadItemType;
    private boolean isFinished = false;
    private UploadRemoteHost uploadRemoteHost;
    private final long startTime;
    
    private volatile long totalAmountUploaded;
    private volatile float uploadSpeed;
    
    @Inject
    CoreUploadItem(@Assisted Uploader uploader, @Assisted FriendPresence friendPresence, CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
        this.uploader = uploader;
        this.friendPresence = friendPresence;
        uploadItemType = uploader instanceof BTUploader ? UploadItemType.BITTORRENT : UploadItemType.GNUTELLA;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        uploader.stop();
        fireDataChanged();
    }

    @Override
    public String getFileName() {
        return uploader.getFileName();
    }

    @Override
    public long getFileSize() {
        return uploader.getFileSize();
    }
    
    Uploader getUploader() {
        return uploader;
    }

    @Override
    public UploadState getState() {
        switch (getUploaderStatus()) {
        case CANCELLED:
            return UploadState.CANCELED;
            
        case COMPLETE:
            if(uploader.getUploadType() == UploadType.BROWSE_HOST){
                return UploadState.BROWSE_HOST_DONE;
            }
            return UploadState.DONE;

        case CONNECTING:
        case UPLOADING:
        case THEX_REQUEST:
        case PUSH_PROXY:
        case UPDATE_FILE:
            return UploadState.UPLOADING;

        case QUEUED:
            return UploadState.QUEUED;

        case PAUSED:
            return UploadState.PAUSED;
            
        case INTERRUPTED:
        case FILE_NOT_FOUND:
        case UNAVAILABLE_RANGE:
        case MALFORMED_REQUEST:
            return UploadState.REQUEST_ERROR;
            
        case LIMIT_REACHED:
        case BANNED_GREEDY:
        case FREELOADER:
            return UploadState.LIMIT_REACHED;
            
        case BROWSE_HOST:
            return UploadState.BROWSE_HOST;
        }

        throw new IllegalStateException("Unknown Upload status : " + uploader.getState());
    }
    
    private UploadStatus getUploaderStatus() {
        // Revert to the prior state if we are at an intermediate "complete"
        // state.  (This means that a particular chunk finished, but more 
        // will come.)
        // We use isFinished to tell us when it's finished, because that is
        // set when finish() is called, which is only called when the entire
        // upload has finished.
        // The intermediate connecting state is okay even if bytes have been
        // read because getState() reports it as an uploading state.
        UploadStatus state = uploader.getState();
        UploadStatus lastState = uploader.getLastTransferState();
        if (state == UploadStatus.COMPLETE && !isFinished) {
            state = lastState;
        }
        
        // Reset the current state to be the lastState if we're complete now,
        // but our last transfer wasn't uploading, queued, or thex.
        if(uploader.getUploadType() != UploadType.BROWSE_HOST &&
          state == UploadStatus.COMPLETE && 
          lastState != UploadStatus.UPLOADING &&
          lastState != UploadStatus.QUEUED &&
          lastState != UploadStatus.THEX_REQUEST) {
            state = lastState;
        }
        
        return state;    
    }

    @Override
    public long getTotalAmountUploaded() {
        return totalAmountUploaded;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uploader == null) ? 0 : uploader.hashCode());
        return result;
    }

    /**
     * Tests if the Uploaders from construction are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoreUploadItem other = (CoreUploadItem) obj;
        if (uploader == null) {
            if (other.uploader != null)
                return false;
        } else if (!uploader.equals(other.uploader))
            return false;
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    void fireDataChanged() {
        support.firePropertyChange("state", null, getState());
    }
    
    /**
     * Updates amount uploaded and upload speed.
     */
    void refresh() {
        // Refresh amount uploaded.
        totalAmountUploaded = uploader.getTotalAmountUploaded();
        
        // Refresh upload speed.
        try {
            uploadSpeed = uploader.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            uploadSpeed = 0;
        }
        
        // Fire event to update UI.
        fireDataChanged();
    }

    @Override
    public Category getCategory() {
        if(uploadItemType == UploadItemType.BITTORRENT)
            return Category.TORRENT;
        else
            return categoryManager.getCategoryForFilename(getFileName());
    }

    @Override
    public RemoteHost getRemoteHost() {
        if(uploadRemoteHost == null)
            uploadRemoteHost = new UploadRemoteHost();
        return uploadRemoteHost;
    }
    
    @Override
    public BrowseType getBrowseType(){
        if (getState() != UploadState.BROWSE_HOST && getState() != UploadState.BROWSE_HOST_DONE){
            return BrowseType.NONE;
        }
        
        if ("".equals(getFileName())){
            return BrowseType.P2P;
        }
        
        return BrowseType.FRIEND;
    }
    
    @Override
    public String toString(){
        return "CoreUploadItem: " + getFileName() + ", " + getState();
    }

    @Override
    public int getQueuePosition() {
        return uploader.getQueuePosition();
    }
    
    @Override
    public float getUploadSpeed() {
        return uploadSpeed;
    }
    
    @Override
    public long getRemainingUploadTime() {
        float speed = getUploadSpeed();
        if (speed > 0) {
            double remaining = (getFileSize() - getTotalAmountUploaded()) / 1024.0;
            return (long) (remaining / speed);
        } else {
            return UNKNOWN_TIME;
        }
    }
    
    @Override
    public Object getProperty(FilePropertyKey property) {
        FileDesc fd = uploader.getFileDesc();
        switch(property) {
        case NAME:
            return (fd == null) ? null : FileUtils.getFilenameNoExtension(fd.getFileName());
        case DATE_CREATED:
            if(fd == null) {
                return null;
            } else {
                long ct = fd.lastModified();
                return ct == -1 ? null : ct;
            }
        case FILE_SIZE:
            return (fd == null) ? null : fd.getFileSize();            
        case TORRENT:
            if(UploadItemType.BITTORRENT == uploadItemType) {
                BTUploader btUploader = (BTUploader)uploader;
                return btUploader.getTorrent();
            } else {
                return null;
            }
        case USERAGENT:
            return uploader.getUserAgent();
        default:
            if(fd == null) {
                return null;
            } else {
                Category category = categoryManager.getCategoryForFilename(fd.getFileName());
                return FilePropertyKeyPopulator.get(category, property, fd.getXMLDocument());
            }
        }
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }

    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = uploader.getUrn();
        if(urn != null) {
            return urn;
        }
        return null;
    }
    
    @Override
    public Collection<File> getCompleteFiles() {
        List<File> files = new ArrayList<File>();
        
        if (uploader instanceof BTUploader) {
            BTUploader btUploader = (BTUploader) uploader;
            files.addAll(btUploader.getCompleteFiles());
        } else {
            files.add(uploader.getFile());
        }
        
        return files;
    }

    @Override
    public File getFile() {
        return uploader.getFile();
    }
    
    @Override
    public String getRenderName() {
        return friendPresence.getFriend().getRenderName();
    }

    @Override
    public UploadItemType getUploadItemType() {
        return uploadItemType;
    }

    @Override
    public int getNumUploadConnections() {
        return uploader.getNumUploadConnections();
    }
    
    @Override
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Called when upload is finished. This enables the DONE state. This method
     * is necessary to present false DONE states.
     */
    void finish(){
        isFinished = true;
        fireDataChanged();
    }

    @Override
    public float getSeedRatio() {
        return uploader.getSeedRatio();
    }

    @Override
    public boolean isFinished() {
        if (uploader instanceof BTUploader) {
            // Return torrent indicator.
            return ((BTUploader) uploader).getTorrent().isFinished();
            
        } else {
            // Determine using state for non-torrents.
            UploadState state = getState();
            return (state == UploadState.DONE || state == UploadState.BROWSE_HOST_DONE);
        }
    }
    
    @Override
    public boolean isStarted() {
        if (uploader instanceof BTUploader) {
            // Return torrent indicator.
            return ((BTUploader) uploader).getTorrent().isStarted();
            
        } else {
            // Always true for non-torrents.
            return true;
        }
    }
    
    @Override
    public void pause() {
        uploader.pause();
    }

    @Override
    public void resume() {
        uploader.resume();        
    }
   
    /**
     * Creates a RemoteHost for this uploader. This allows browses on the 
     * person uploading this file.
     */
    private class UploadRemoteHost implements RemoteHost {
        
        @Override
        public FriendPresence getFriendPresence() {
            return friendPresence;
        }

        @Override
        public boolean isBrowseHostEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return uploader.isBrowseHostEnabled();
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }

        @Override
        public boolean isChatEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return false;
            }else { //TODO: this isn't entirely correct. Friend could have signed
                // ouf of LW but still be logged in through other service allowing chat
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }

        @Override
        public boolean isSharingEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return false;
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }
    }

    @Override
    public List<SourceInfo> getTransferDetails() {
        return uploader.getTransferDetails();
    }
}
