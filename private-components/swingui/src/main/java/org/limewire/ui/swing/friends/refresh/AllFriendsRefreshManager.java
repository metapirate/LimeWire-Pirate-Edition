package org.limewire.ui.swing.friends.refresh;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.search.DefaultSearchRepeater;
import org.limewire.ui.swing.search.SearchRepeater;
import org.limewire.ui.swing.search.model.SearchResultsModel;

import com.google.inject.Inject;

@EagerSingleton
public class AllFriendsRefreshManager implements SearchRepeater{
    
    /**
     * The amount of time before a second refresh status update is fired  
     */
    private static final long DELAY = 10 * 1000;
    
    private final RemoteLibraryManager remoteLibraryManager;
    /**The earliest time a status change can be fired.  Initialized to January 1, 1970.*/
    private long earliestPossibleFiringTime = 0;
    
    private boolean hasInsert = false;
    private boolean hasDelete = false;
    private BrowseSearch allFriendsBrowse;
    private SearchResultsModel allFriendsModel;
    private final Timer delayTimer = new DelayTimer();
    
    private ArrayList<BrowseRefreshStatusListener> statusListeners = new ArrayList<BrowseRefreshStatusListener>();
    
    @Inject
    public AllFriendsRefreshManager(RemoteLibraryManager remoteLibraryManager){
        this.remoteLibraryManager = remoteLibraryManager;
   
    }
    
    @Inject
    public void register(){
        remoteLibraryManager.getAllFriendsLibrary().addListener(new EventListener<RemoteLibraryEvent>() {
            public void handleEvent(RemoteLibraryEvent event) {
                switch (event.getType()) {
                case RESULTS_ADDED:
                    hasInsert = true;
                    break;
                case RESULTS_CLEARED:
                case RESULTS_REMOVED:
                    hasDelete = true;
                    break;
                }
                fireCurrentStatus();               
            }; 
         });
    }
    
    public void addBrowseRefreshStatusListener(BrowseRefreshStatusListener listener){
        statusListeners.add(listener);
    }
    
    public void removeBrowseRefreshStatusListener(BrowseRefreshStatusListener listener){
        statusListeners.remove(listener);
    }
    
    /**
     * Fires the current status immediately if more time than DELAY has elapsed since the previous status change.
     * Fires with a delay otherwise.
     */
    private void fireCurrentStatus(){
        if (!delayTimer.isRunning()) {
            long delayRemaining = earliestPossibleFiringTime - System.currentTimeMillis();
            if (delayRemaining <= 0) {
                fireCurrentStatusNow();
            } else {
                delayTimer.setInitialDelay((int)(delayRemaining));
                delayTimer.start();
            }
        }
    }
        
        private void fireCurrentStatusNow() {
        // we only have an insert if there is currently something in the library
        hasInsert = hasInsert && hasSharedFiles();

        if (hasInsert && hasDelete) {
            fireRefreshStatusChange(BrowseRefreshStatus.CHANGED);
        } else if (hasInsert) {
            fireRefreshStatusChange(BrowseRefreshStatus.ADDED);
        } else if (hasDelete) {
            fireRefreshStatusChange(BrowseRefreshStatus.REMOVED);
        }
    }
    
    public void registerBrowseSearch(BrowseSearch allFriendsBrowse, SearchResultsModel allFriendsModel){
        this.allFriendsBrowse = allFriendsBrowse;
        this.allFriendsModel = allFriendsModel;
        //We have a new BrowseSearch because a new one just started.  This means we are refreshed.
        fireRefreshed();
    }
    
    public void clearBrowseSearch(){
        this.allFriendsBrowse = null;
        this.allFriendsModel = null;
    }
    
    @Override
    public void refresh() {
        if (needsRefresh()) {
            new DefaultSearchRepeater(allFriendsBrowse, allFriendsModel).refresh();
            fireRefreshed();
        }
    }
  
    private void fireRefreshed(){
        hasInsert = false;    
        hasDelete = false;  
        delayTimer.stop();
        fireRefreshStatusChange(BrowseRefreshStatus.REFRESHED);
    }
    
    private void fireRefreshStatusChange(final BrowseRefreshStatus status){
        earliestPossibleFiringTime = System.currentTimeMillis() + DELAY;
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                for(BrowseRefreshStatusListener listener : statusListeners){
                    listener.statusChanged(status);                    
                }
            }
        });
    }

    public boolean needsRefresh() {
        return hasInsert || hasDelete;
    }
    
    public boolean hasSharedFiles(){
        return remoteLibraryManager.getAllFriendsLibrary().size() > 0;
    }
    
    
    private class DelayTimer extends Timer {

        public DelayTimer() {
            super(0, new FireStatusChangeAction());
            setRepeats(false);
        }
        
    }
    
    private class FireStatusChangeAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            fireCurrentStatusNow();
        }
        
    }
    
}
