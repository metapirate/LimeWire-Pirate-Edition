package org.limewire.core.api.upload;

import java.beans.PropertyChangeListener;
import java.util.List;

import ca.odell.glazedlists.EventList;

/**
 * Defines the manager API for the list of uploads. 
 */
public interface UploadListManager {
    /** Property name for uploads completed event. */
    public static final String UPLOADS_COMPLETED = "uploadsCompleted";

    /**
     * Returns a list of all items being uploaded.
     */
    List<UploadItem> getUploadItems();

    /**
     * Returns a Swing-thread safe version of the uploads event list.
     */
    EventList<UploadItem> getSwingThreadSafeUploads();
    
    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes. 
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes. 
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Checks for uploads in progress, and fires a property change event if
     * all uploads are completed.
     */
    public void updateUploadsCompleted();
    
    
    /**
     * Clears all completed uploads from the list.
     */
    public void clearFinished();

    /**Removes item from list.*/
    void remove(UploadItem item);
}
