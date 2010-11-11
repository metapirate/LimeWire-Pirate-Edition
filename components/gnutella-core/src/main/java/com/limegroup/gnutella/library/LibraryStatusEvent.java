package com.limegroup.gnutella.library;

import org.limewire.util.StringUtils;


public class LibraryStatusEvent {
    
    public static enum Type {        
        /**
         * Called once FileManager is preparing to finish the loading process. 
         * 
         * Loading is completed as follows:
         *  1)Load_finishing
         *  2)save
         *  3)load_complete
         *  
         * The purpose of the multi-phased approach is to allow listeners
         * of the SAVE event a chance to cleanup data prior to SAVE. 
         */
        LOAD_FINISHING,
        
        /**
         * Called after load_finishing and prior to load_complete. Allows 
         * anything to write to disk.  It is also called periodially
         * if FileManager has any changed that should be written to disk.
         */
        SAVE,
        
        /**
         * Called after FileManager has completely finished loading
         */
        LOAD_COMPLETE,
        
    }
    
    private final Type type;
    private final Library library;
    
    public LibraryStatusEvent(Library list, Type type) {
        this.type = type;
        this.library = list;
    }
    
    public Type getType() {
        return type;
    }
    
    public Library getLibrary() {
        return library;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
