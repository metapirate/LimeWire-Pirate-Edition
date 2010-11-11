package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.malware.VirusEngine;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.core.impl.RemoteHostRFD;
import org.limewire.core.impl.friend.GnutellaPresence;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.malware.VirusDefinitionDownloader;
import com.limegroup.gnutella.malware.VirusDefinitionDownloaderKeys;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class CoreDownloadItem implements DownloadItem, Downloader.ScanListener {
    
    static interface Factory {
        CoreDownloadItem create(Downloader downloader, QueueTimeCalculator calculator);
    }
   
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    private final Downloader downloader;
    private volatile int hashCode = 0;
    private volatile long cachedSize;
    private volatile boolean cancelled = false;
    private volatile boolean scanningFragment = false;

    private final QueueTimeCalculator queueTimeCalculator;
    private final FriendManager friendManager;
    private final DownloadItemType downloadItemType;
    private final CategoryManager categoryManager;
    
    @Inject
    public CoreDownloadItem(@Assisted Downloader downloader, @Assisted QueueTimeCalculator queueTimeCalculator, FriendManager friendManager, CategoryManager categoryManager) {
        this.downloader = downloader;
        this.queueTimeCalculator = queueTimeCalculator;
        this.friendManager = friendManager;
        this.categoryManager = categoryManager;
        if(downloader instanceof BTDownloader)
            downloadItemType = DownloadItemType.BITTORRENT;
        else if(downloader instanceof VirusDefinitionDownloader)
            downloadItemType = DownloadItemType.ANTIVIRUS;
        else
            downloadItemType = DownloadItemType.GNUTELLA;
        
        downloader.addListener(new EventListener<DownloadStateEvent>() {
            @Override
            public void handleEvent(DownloadStateEvent event) {
                // broadcast the status has changed
                fireDataChanged();
                if (event.getType() == com.limegroup.gnutella.Downloader.DownloadState.ABORTED) {
                    //attempt to delete ABORTED file
                    CoreDownloadItem.this.downloader.deleteIncompleteFiles();
                }
            }           
        });
    }

    @Override
    public DownloadItemType getDownloadItemType() {
        return downloadItemType;
    }
    
    void fireDataChanged() {
        cachedSize = downloader.getAmountRead();
        support.firePropertyChange("state", null, getState());
    }
    
    private Downloader getDownloader(){
        return downloader;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }

    @Override
    public void cancel() {
        cancelled = true;
        support.firePropertyChange("state", null, getState());
        new ManagedThread(new Runnable() {
            @Override
            public void run() {
                downloader.stop();
                downloader.deleteIncompleteFiles();
            }
        }, "CoreDownloadItem.cancel").start();
        // TODO there is a race condition with the delete action, the stop does
        // not happen right away. should revisit how this will be handled.
    }

    @Override
    public Category getCategory() {
        if(downloadItemType == DownloadItemType.BITTORRENT) {
            return Category.OTHER;
        } else {
            File file = downloader.getFile();
            if(file != null) {
                return categoryManager.getCategoryForFile(file);
            } else {
                // TODO: See if it's OK to always use save file.
                return categoryManager.getCategoryForFile(downloader.getSaveFile());
            }
        }
    }

    @Override
    public long getCurrentSize() {
        DownloadState state = getState();
        if (state == DownloadState.SCANNING || state.isFinished()) {
            return getTotalSize();
        } else {
            return cachedSize;
        }
    }
    
    @Override
    public long getAmountVerified() {
        return downloader.getAmountVerified();
    }
    
    @Override
    public long getAmountLost() {
        return downloader.getAmountLost();
    }

    @Override
    public List<Address> getSources() {
        return downloader.getSourcesAsAddresses();
    }
    
    @Override
    public List<SourceInfo> getSourcesDetails() {
        return downloader.getSourcesDetails();
    }
    
    @Override 
    public DownloadPiecesInfo getPiecesInfo() {
        return downloader.getPieceInfo();
    }

    @Override
    public Collection<RemoteHost> getRemoteHosts() {
        List<RemoteFileDesc> remoteFiles = downloader.getRemoteFileDescs();
        
        if(remoteFiles.size() > 0) {
            List<RemoteHost> remoteHosts = new ArrayList<RemoteHost>(remoteFiles.size());
            for(RemoteFileDesc rfd : remoteFiles) {
                remoteHosts.add(new RemoteHostRFD(rfd, getFriendPresence(rfd)));
                
            }
            return remoteHosts;
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
	 * Returns a FriendPresence for this RemoteFileDesc. If this is
	 * from a friend returns an associated LW FriendPresnce otherwise
     * returns a generic GnutellaPresence.
	 */
    private FriendPresence getFriendPresence(RemoteFileDesc rfd) {
        FriendPresence friendPresence = null;
        
        if(rfd.getAddress() instanceof FriendAddress) {
            friendPresence = friendManager.getMostRelevantFriendPresence(((FriendAddress)rfd.getAddress()).getId());
        } 
        if(friendPresence == null) {
            friendPresence = new GnutellaPresence.GnutellaPresenceWithGuid(rfd.getAddress(), rfd.getClientGUID());
        }
        return friendPresence;
    }

    @Override
    public int getDownloadSourceCount() {
        return downloader.getNumHosts();
    }

    @Override
    public int getPercentComplete() {
        DownloadState state = getState();
        if(state == DownloadState.FINISHING ||
                state == DownloadState.SCANNING ||
                state.isFinished()){
            return 100;
        }

        if(getTotalSize() == 0)
            return 0;
        else
            return (int) (100 * getCurrentSize() / getTotalSize());
    }

    @Override
    public long getRemainingDownloadTime() {
        double remaining = (getTotalSize() - getCurrentSize()) / 1024.0;
        float speed = getDownloadSpeed();
        if (speed > 0) {
            return (long) (remaining / speed);
        } else {
            return UNKNOWN_TIME;
        }

    }
    
    @Override
    public float getDownloadSpeed(){
        try {
            return downloader.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }  
    }

    @Override
    public DownloadState getState() {
        if(cancelled){
            return DownloadState.CANCELLED;
        }
        if(scanningFragment) {
            return DownloadState.SCANNING_FRAGMENT;
        }
        return convertState(downloader.getState());
    }

    @Override
    public String getTitle() {
        // TODO return title, not file name
        return downloader.getSaveFile().getName();
    }

    @Override
    public long getTotalSize() {
        return downloader.getContentLength();
    }

    @Override
    public void pause() {
        downloader.pause();
    }

    @Override
    public void resume() {
        downloader.resume();
    }
    
    @Override
    public int getRemoteQueuePosition(){
        if(downloader.getState() == com.limegroup.gnutella.Downloader.DownloadState.REMOTE_QUEUED) {
            return downloader.getQueuePosition();
        } else { 
            return -1;
        }
    }
    
    private DownloadState convertState(com.limegroup.gnutella.Downloader.DownloadState state) {
        switch (state) {
        case RESUMING:
            return DownloadState.RESUMING;

        case SAVING:
        case HASHING:
            if (getTotalSize() > 0) {
                return DownloadState.FINISHING;
            } else {
                return DownloadState.DONE;
            }

        case DOWNLOADING:
            return DownloadState.DOWNLOADING;

        case CONNECTING:
        case INITIALIZING:
        case WAITING_FOR_CONNECTIONS:
            return DownloadState.CONNECTING;

        case COMPLETE:
            return DownloadState.DONE;

        case REMOTE_QUEUED:
        case BUSY://BUSY should look like locally queued but acts like remotely
            return DownloadState.REMOTE_QUEUED;

        case QUEUED:
            return DownloadState.LOCAL_QUEUED;

        case PAUSED:
            return DownloadState.PAUSED;

        case WAITING_FOR_GNET_RESULTS:
        case ITERATIVE_GUESSING:
        case QUERYING_DHT:
            return DownloadState.TRYING_AGAIN;

        case WAITING_FOR_USER:
        case GAVE_UP:
            return DownloadState.STALLED;

        case ABORTED:
            return DownloadState.CANCELLED;

        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case INVALID:
        case UNABLE_TO_CONNECT:
            return DownloadState.ERROR;

        case DANGEROUS:
            return DownloadState.DANGEROUS;

        case SCANNING:
            return DownloadState.SCANNING;

        case THREAT_FOUND:
            return DownloadState.THREAT_FOUND;

        case SCAN_FAILED:
            return DownloadState.SCAN_FAILED;
            
        case APPLYING_ANTIVIRUS_DEFINITION_UPDATE:
            return DownloadState.APPLYING_DEFINITION_UPDATE;
            
        default:
            throw new IllegalStateException("Unknown State: " + state);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CoreDownloadItem)) {
            return false;
        }
        return getDownloader().equals(((CoreDownloadItem) o).getDownloader());
    }
    
    //TODO: better hashCode
    @Override
    public int hashCode(){
        if(hashCode == 0){
           hashCode =  37* getDownloader().hashCode();
        }
        return hashCode;
    }

    @Override
    public ErrorState getErrorState() {
        switch (downloader.getState()) {
        case CORRUPT_FILE:
            return ErrorState.CORRUPT_FILE;
        case DISK_PROBLEM:
            return ErrorState.DISK_PROBLEM;
        case INVALID:
            return ErrorState.INVALID;
        case UNABLE_TO_CONNECT:
            return ErrorState.UNABLE_TO_CONNECT;
        default:
            return ErrorState.NONE;
        }
    }
    
    @Override
    public boolean isTryAgainEnabled() {
        return DownloadItemType.BITTORRENT == downloadItemType || downloader.getState() == com.limegroup.gnutella.Downloader.DownloadState.WAITING_FOR_USER;
    }

    @Override
    public long getRemainingTimeInState() {
        long remaining = downloader.getRemainingStateTime();
        // Change a few state times explicitly.
        switch(downloader.getState()) {
        case QUEUED:
            remaining = queueTimeCalculator.getRemainingQueueTime(this);
            break;
        case QUERYING_DHT:
            remaining = UNKNOWN_TIME;
            break;
        }
        if(remaining == Integer.MAX_VALUE) {
            remaining = UNKNOWN_TIME;
        }
        return remaining;
    }

 
    @Override
    public int getLocalQueuePriority() {
        return downloader.getInactivePriority();
    }

    @Override
    public boolean isLaunchable() {
        return downloader.isLaunchable();
    }

    @Override
    public File getDownloadingFile() {
        return downloader.getFile();
    }
    
    @Override
    public File getLaunchableFile() {
        return downloader.getDownloadFragment(this);
    }
    
    @Override
    public void scanStarted() {
        scanningFragment = true;
    }
    
    @Override
    public void scanStopped() {
        scanningFragment = false;
    }
    
    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = downloader.getSha1Urn();
        return urn;
    }

    @Override
    public String getFileName() {
        return downloader.getSaveFile().getName();
    }
    
    @Override
    public void setSaveFile(File saveFile, boolean overwrite) throws DownloadException {
        File saveDir = null;
        String fileName = null;

        // Determine save directory and file name.
        if (saveFile != null) {
            if (saveFile.isDirectory()) {
                saveDir = saveFile;
                fileName = getFileName();
            } else {
                saveDir = saveFile.getParentFile();
                fileName = saveFile.getName();
            }
        }
        
        // Update save directory and file name.
        downloader.setSaveFile(saveDir, fileName, overwrite);
    }
    
    @Override
    public File getSaveFile(){
        return downloader.getSaveFile();
    }

    @Override
    public Object getProperty(FilePropertyKey property) {
        switch(property) {
        case NAME:
            return FileUtils.getFilenameNoExtension(getFileName());
        case DATE_CREATED:
            File file = downloader.getFile();
            long ct = -1;
            if(file != null) {
                ct = file.lastModified();
            }
            return ct == -1 ? null : ct;
        case FILE_SIZE:
            return getTotalSize();     
        case TORRENT:
            if(downloadItemType == DownloadItemType.BITTORRENT) {
                BTDownloader btDownloader = (BTDownloader)downloader;
                return btDownloader.getTorrent();
            } else {
                return null;
            }
        default:
            LimeXMLDocument doc = (LimeXMLDocument)downloader.getAttribute("LimeXMLDocument");
            if(doc != null) {
                Category category = categoryManager.getCategoryForFile(getSaveFile());
                return FilePropertyKeyPopulator.get(category, property, doc);
            } else {
                return null;
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
    public Date getStartDate() {
        return (Date)downloader.getAttribute(DownloadItem.DOWNLOAD_START_DATE);
    }

    @Override
    public boolean isRelocatable() {
        return downloader.isRelocatable();
    }

    @Override
    public Collection<File> getCompleteFiles() {
        List<File> files = new ArrayList<File>();
        if(downloader instanceof BTDownloader) {
            BTDownloader btDownloader = (BTDownloader)downloader;
            files.addAll(btDownloader.getCompleteFiles());
        } else {
            files.add(downloader.getSaveFile());
        }
        return files;
    }

    @Override
    public Object getDownloadProperty(DownloadPropertyKey key) {
        switch(key) {
        case ANTIVIRUS_FAIL_HINT:
            return downloader.getAttribute(VirusEngine.DOWNLOAD_FAILURE_HINT);
        case ANTIVIRUS_UPDATE_TYPE:
            return downloader.getAttribute(VirusDefinitionDownloaderKeys.TYPE); 
        case ANTIVIRUS_INCREMENT_COUNT:
            Object count = downloader.getAttribute(VirusDefinitionDownloaderKeys.COUNT);
            return count == null ? 0 : count;
        case ANTIVIRUS_INCREMENT_INDEX:
            Object index = downloader.getAttribute(VirusDefinitionDownloaderKeys.INDEX);
            return index == null ? 0 : index;
        }
        return null;
    }
}
