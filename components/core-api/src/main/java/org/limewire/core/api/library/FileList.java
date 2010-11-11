package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

/** A list of FileItems. */
public interface FileList <T extends FileItem> {
    
    /** An {@link EventList} that describes this list. */
    EventList<T> getModel();
    
    /**
     * An {@link EventList} that, for convenience, is usable from Swing.
     */
    EventList<T> getSwingModel();
    
    /** The size of the list. */
    public int size();
}
