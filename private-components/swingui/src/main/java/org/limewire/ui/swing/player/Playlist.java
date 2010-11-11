package org.limewire.ui.swing.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.SwingEDTEvent;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Contains a reference to an EventList that is used for playing
 * a list of audio file automatically. Since the backing of the
 * playlist is an EventList, any changes on the list will be reflected
 * within the playlist automatically. 
 */
class Playlist {
    
    /** File item for the last opened song. */
    private LocalFileItem currentFileItem = null;
    
    /** Indicator for shuffle mode. */
    private boolean shuffle = false;
    
    /** Current list of songs. Null if no list is selected. */
    private EventList<LocalFileItem> playlist = null;
    private List<LocalFileItem> shuffleList = Collections.emptyList();
    private ListChanges listChanges = null;
    
    /**
     * Sets the current FileItem that is playing. If no FileItem exists for
     * the item being played, can be set to null.
     */
    public void setCurrentItem(LocalFileItem fileItem) {
        this.currentFileItem = fileItem;
    }

    /**
     * Sets the current EventList that is being used as the playlist. If
     * no playlist exists, this can be set to null.
     */
    public void setActivePlaylist(EventList<LocalFileItem> fileList) {
        if(playlist != null) {
            removeListener();
            playlist.dispose();
        }
        this.playlist = fileList;
        
        if(shuffle) {
            createShuffleList();
            addListener();
        }
    }
    
    /**
     * Returns true if shuffle moad is set, false otherwise.
     */
    public boolean isShuffle() {
        return shuffle;
    }
    
    /**
     * Returns the next file item in the current playlist. If no next FileItem
     * exists, returns null.
     */
    public LocalFileItem getNextFileItem() {        
        if(!isValidPlaylist())
            return null;
        
        if(shuffle) {
            currentFileItem = getNextItem(shuffleList, true);
        } else {
            currentFileItem = getNextItem(playlist, true);
        }
        return currentFileItem;
    }
    
    /**
     * Returns the previous file item in the current playlist. If no previous item
     * exists, returns null.
     */
    public LocalFileItem getPrevFileItem() {
        if(!isValidPlaylist())
            return null;
        
        if(shuffle) {
            currentFileItem = getNextItem(shuffleList, false);
        } else {
            currentFileItem = getNextItem(playlist, false);
        }
        return currentFileItem;
    }
    
    private boolean isValidPlaylist() {
        return playlist != null && currentFileItem != null && playlist.size() > 0;
    }
    
    /**
     * Returns the size of the current playlist. If no playlist
     * is set returns 0.
     */
    public int size() {
        if(playlist == null)
            return 0;
        else
            return playlist.size();
    }

    /**
     * Returns the next item in the given list. If isForward is true, will 
     * return the next item that is incremented. If isForward is false, will
     * return the next item decremented by the counter.
     */
    private LocalFileItem getNextItem(List<LocalFileItem> list, boolean isForward) {
        if(list == null)
            return null;

        int index = list.indexOf(currentFileItem);
        if(isForward) {
            if (index >= 0 && index < (list.size() - 1)) {
                return list.get(index + 1);
            } else {
                return null;
            }
        } else {
            if (index > 0 && index < (list.size())) {
                return list.get(index - 1);
            } else {
                return null;
            }
        }
    }
    
    /**
     * Updates the shuffle state.
     */
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        
        // Update shuffle list.
        if (shuffle) {
            createShuffleList();
            addListener();
        } else {
            shuffleList = Collections.emptyList();
            removeListener();
        }
    }
    
    /**
     * Creates and adds a listener to the playlist if it doesn't
     * already exist.
     */
    private void addListener() {
        if(playlist != null && listChanges == null) {
            listChanges = new ListChanges();
            playlist.addListEventListener(listChanges);
        }
    }
    
    /**
     * Removes the listener from the playlist if one exists.
     */
    private void removeListener() {
        if(playlist != null && listChanges != null) {
            playlist.removeListEventListener(listChanges);
            listChanges = null;
        }
    }
    
    /**
     * Creates a ShuffleList based on the current files within the 
     * playlist. If the playlist is changed, this list must be recreated.
     */
    private void createShuffleList() {
        shuffleList = new ArrayList<LocalFileItem>();
        
        if(playlist != null && playlist.size() > 0 && currentFileItem != null) {
            playlist.getReadWriteLock().readLock().lock();
            try {
                for (Iterator<LocalFileItem> iter = playlist.iterator(); iter.hasNext();) {
                    shuffleList.add(iter.next());
                }
            } finally {
                playlist.getReadWriteLock().readLock().unlock();
            }
            
            Collections.shuffle(shuffleList);
            
            int index = shuffleList.indexOf(currentFileItem);
            if (index > 0) {
                shuffleList.remove(index);
                shuffleList.add(0, currentFileItem);
            }
        }
    }

    /**
     * Listens to changes in the playlist. Recreates the shuffleList
     * if this list changes.
     */
    private class ListChanges implements ListEventListener<LocalFileItem> {
        @Override
        @SwingEDTEvent
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            createShuffleList();
        }        
    }
}
