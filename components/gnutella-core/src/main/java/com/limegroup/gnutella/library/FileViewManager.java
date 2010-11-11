package com.limegroup.gnutella.library;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

/** A manager for retrieving named file views. */
public interface FileViewManager extends ListenerSupport<FileViewChangeEvent> {
    
    /** Returns a {@link FileView} that contains all files visible to the given id. */
    FileView getFileViewForId(String id);
    
    /**
     * Adds a listener to events from all file views. Use
     * {@link FileViewChangeEvent#getSource()} to determine which FileView was
     * modified.  {@link FileView#getName()} can be used to determine
     * the id of the person this is a view of files for.
     */
    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener);

    /** Removes a listener to events from all file views. */
    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener);
    
    
    
    

}
