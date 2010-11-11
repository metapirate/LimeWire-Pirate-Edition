package org.limewire.core.impl.upload;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.impl.friend.BittorrentPresence;
import org.limewire.core.impl.friend.GnutellaPresence;
import org.limewire.core.settings.SharingSettings;
import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.ConnectableImpl;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.impl.ThreadSafeList;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTUploader;
import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;

@EagerSingleton
public class CoreUploadListManager implements UploadListener, UploadListManager {

    private final CoreUploadItem.Factory cuiFactory;
    private final UploadServices uploadServices;
    private final FriendManager friendManager;
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);

    private final EventList<UploadItem> uploadItems;

    private EventList<UploadItem> swingThreadUploadItems;
    private ThreadSafeList<UploadItem> threadSafeUploadItems;
    
    private static final int PERIOD = 1000;

    @Inject
    public CoreUploadListManager(UploadServices uploadServices, FriendManager friendManager, CoreUploadItem.Factory cuiFactory) {
        this.cuiFactory = cuiFactory;
        this.uploadServices = uploadServices;
        this.friendManager = friendManager;
        
        threadSafeUploadItems = GlazedListsFactory.threadSafeList(new BasicEventList<UploadItem>());
        
        ObservableElementList.Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);        
        uploadItems = GlazedListsFactory.observableElementList(threadSafeUploadItems, uploadConnector) ;
    }

    @Inject
    public void register(UploadListenerList uploadListenerList) {
        uploadListenerList.addUploadListener(this);       
    }
    
    /**
     * Prepare and install the (polling) monitor service.  This service will only be started when
     *  the stage keyed by *this* object is initiated.
     */
    // TODO: Come up with a reasonable strategy for ServiceRegistry custom stage keys (Strings vs Hashes?)
    @Inject
    public void register(ServiceScheduler scheduler, @Named("backgroundExecutor") ScheduledExecutorService executor) {
          Runnable command = new Runnable() {
              @Override
              public void run() {
                  update();
              }
          };
          
          scheduler.scheduleWithFixedDelay("UI Upload Status Monitor", 
                  command, 0, PERIOD,
                  TimeUnit.MILLISECONDS, executor).in(this);
    }
    
    @Override
    public List<UploadItem> getUploadItems() {
        return uploadItems;
    }

    @Override
    public EventList<UploadItem> getSwingThreadSafeUploads() {
        assert EventQueue.isDispatchThread();
        if (swingThreadUploadItems == null) {
            swingThreadUploadItems = GlazedListsFactory.swingThreadProxyEventList(uploadItems);
        }
        return swingThreadUploadItems;
    }
    
    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes.  Listeners added from the Swing UI thread will
     * always receive notification events on the Swing UI thread. 
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes. 
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Checks for uploads in progress, and fires a property change event if
     * all uploads are completed.
     */
    @Override
    public void updateUploadsCompleted() {
        if (uploadServices.getNumUploads() == 0) {
            uploadsCompleted();
        }
    }

    @Override
    public void uploadAdded(Uploader uploader) {
        if (!uploader.getUploadType().isInternal()) {

            CoreUploadItem currentItem = findMatchingUploader(uploader);
            CoreUploadItem newItem = cuiFactory.create(uploader, getFriendPresence(uploader));
            
            // if there's no matching uploader, add as a new upload
            if(currentItem == null) {
                threadSafeUploadItems.add(newItem);
            } else {
                threadSafeUploadItems.getReadWriteLock().writeLock().lock();
                try { 
                    threadSafeUploadItems.set(threadSafeUploadItems.indexOf(currentItem), newItem);
                } finally {
                    threadSafeUploadItems.getReadWriteLock().writeLock().unlock();
                }
            }
        }
    }
    
    /**
     * Attempts to match a newly added uploader with one that already exists in the list.
     * If an uploader already exists that matches this Uploader, it returns the pre-existing
     * CoreUploadItem, otherwise returns null.
     */
    private CoreUploadItem findMatchingUploader(Uploader uploader) {
    	CoreUploadItem matchingItem = null;
        threadSafeUploadItems.getReadWriteLock().readLock().lock(); 
        try { 
            for(UploadItem item : threadSafeUploadItems) {
                CoreUploadItem coreUploadItem = (CoreUploadItem) item;
                if(coreUploadItem.getUploader().getHost().equals(uploader.getHost())) {
                    // if its a browse host, there's no file to match on
                    if(uploader.getState() == UploadStatus.BROWSE_HOST && uploader.getUploadType() == coreUploadItem.getUploader().getUploadType()) {
                        matchingItem = coreUploadItem;
                        break;
                    } else if(uploader.getFile() != null && uploader.getFile().equals(coreUploadItem.getFile())) {       
                        matchingItem = coreUploadItem;
                        break;
                    }
                }
            }
        } finally {
            threadSafeUploadItems.getReadWriteLock().readLock().unlock();
        }
        
        return matchingItem;
    }

    @Override
    public void uploadComplete(Uploader uploader) {
        CoreUploadItem item = cuiFactory.create(uploader, getFriendPresence(uploader));
        //alert item that it really is finished so that getState() will be correct
        item.finish();
         
        UploadState state = item.getState();
        if (state == UploadState.CANCELED) {
            //cancelled items should be removed immediately
            remove(item);
        } else if (state == UploadState.DONE || state == UploadState.BROWSE_HOST_DONE || state.isError()) {
            if (SharingSettings.CLEAR_UPLOAD.getValue()) {
                //Remove if auto-clear is enabled.
                remove(item);
            } else {
                //make sure upload state is correct and UI is informed of state change
                int i = threadSafeUploadItems.indexOf(item);
                if (i>-1) {
                    ((CoreUploadItem)threadSafeUploadItems.get(i)).finish();
                } 
            }
        }
    }
    
    @Override
    public void uploadsCompleted() {
        changeSupport.firePropertyChange(UPLOADS_COMPLETED, false, true);
    }
    
    // forces refresh
    void update() {
        uploadItems.getReadWriteLock().writeLock().lock();
        try {
            // TODO use TransactionList for these for performance (requires using GlazedLists from head)
            for (UploadItem item : uploadItems) {
                if (item.getState() != UploadState.DONE && item.getState() != UploadState.BROWSE_HOST_DONE && item instanceof CoreUploadItem)
                    ((CoreUploadItem) item).refresh();
            }
        } finally {
            uploadItems.getReadWriteLock().writeLock().unlock();
        }
    }
    
    private FriendPresence getFriendPresence(Uploader uploader) {
        
        if(uploader instanceof BTUploader) {
            return new BittorrentPresence(uploader);
        }
        
        String id = uploader.getPresenceId();
        FriendPresence currentPresence = null;
        if (id != null) {
             currentPresence = friendManager.getMostRelevantFriendPresence(id);
        }
        if (currentPresence == null) {
            // copy construct connectable to give it full equals semantics
            currentPresence = new GnutellaPresence.GnutellaPresenceWithString(new ConnectableImpl(uploader), uploader.getHost());
        }
        return currentPresence;
    }

    /**
     * Thread safe method which removes any finished uploads from management.
     */
    @Override
    public void clearFinished() {
        List<UploadItem> finishedItems = new ArrayList<UploadItem>();
        threadSafeUploadItems.getReadWriteLock().writeLock().lock();
        try {
            for(UploadItem item : threadSafeUploadItems) {
                UploadState state = item.getState();
                if (state.isFinished() || state.isError()) {
                    finishedItems.add(item);
                }
            }
            threadSafeUploadItems.removeAll(finishedItems);
        } finally {
            threadSafeUploadItems.getReadWriteLock().writeLock().unlock();
        }
    }
    
    /** 
     * Thread safe method which force removes an upload item from management. 
     */
    @Override
    public void remove(UploadItem item) {
        threadSafeUploadItems.remove(item);
    }
}
