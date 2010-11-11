package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.limewire.setting.AbstractSettingsGroup;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.setting.evt.SettingsGroupEvent.EventType;


/**
 * A container of LibraryData.
 */
@Deprecated
@SuppressWarnings("deprecation")
final class OldLibraryData extends AbstractSettingsGroup {
    
    /**
     * The Container data, storing all the information.
     */
    private final Container DATA = new Container("library.dat");
    
    /**
	 * The directories not to share.
	 */
    public final Set<File> DIRECTORIES_NOT_TO_SHARE = DATA.getSet("DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Sensitive directories that are explicitly allowed to be shared.
     */
    public final Set<File> SENSITIVE_DIRECTORIES_VALIDATED = DATA.getSet("SENSITIVE_DIRECTORIES_VALIDATED");
    
    /**
     * Sensitive directories that are explicitly not allowed to be shared.
     */
    public final Set<File> SENSITIVE_DIRECTORIES_NOT_TO_SHARE = DATA.getSet("SENSITIVE_DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Individual files that should be shared despite being located outside
     * of any shared directory, and despite any extension limitations.
     */
    public final Set<File> SPECIAL_FILES_TO_SHARE = DATA.getSet("SPECIAL_FILES_TO_SHARE");
    
    /**
     * Files that should be not shared despite being located inside
     * a shared directory.
     */
    public final Set<File> FILES_NOT_TO_SHARE = DATA.getSet("FILES_NOT_TO_SHARE");    
    
    /**
     * Files in a shared folder that are not the location of the LWS downloads but 
     * were purchased from the LWS.
     */
    public final Set<File> SPECIAL_STORE_FILES = DATA.getSet("SPECIAL_STORE_FILES");
    
    /**
     * Constructs a new LibraryData, adding it to the SettingsHandler for maintanence.
     */
    public OldLibraryData() {
        SettingsGroupManager.instance().addSettingsGroup(this);
    }
    
    /**
     * Saves all the settings to disk.
     */
    public boolean save() {
        if (getShouldSave()) {
            DATA.save();
            fireSettingsEvent(EventType.SAVE);
            return true;
        }
        return false;
    }
    
    /**
     * Reverts all settings to their defaults -- this clears all the settings.
     */
    public boolean revertToDefault() {
        DATA.clear();
        fireSettingsEvent(EventType.REVERT_TO_DEFAULT);
        return true;
    }
    
    /**
     * Reloads all settings to match what's on disk.
     */
    public void reload() {
        DATA.load();
        fireSettingsEvent(EventType.RELOAD);
    }
    
	/**
	 * Cleans special file sharing settings by removing references to files that
	 * no longer exist.
	 */
	public final void clean() {
	    OldLibrarySettings.DIRECTORIES_TO_SHARE.clean();
		Set<File> parents = OldLibrarySettings.DIRECTORIES_TO_SHARE.get();
		clean(DIRECTORIES_NOT_TO_SHARE, parents);
		clean(FILES_NOT_TO_SHARE, parents);
		clean(SENSITIVE_DIRECTORIES_VALIDATED, parents);
		clean(SENSITIVE_DIRECTORIES_NOT_TO_SHARE, parents);
        clean(SPECIAL_STORE_FILES, parents);
	}
	
	/**
	 * Cleans out entries from a setting that no long exist on disk, or,
	 * if the second parameter is non-null, don't exist anywhere in the list
	 * of the second parameter's settings.
	 */
	private void clean(Set<File> one, Set<File> two) {
	    synchronized(one) {
	        for(Iterator<File> i = one.iterator(); i.hasNext(); ) {
	            File f = i.next();
                if(!f.exists())
                    i.remove();
                else if(two != null && !containsParent(f, two))
                    i.remove();
	        }
        }
    }
	
	/**
	 * Determines if the File or any of its parents is contained in the given Set.
	 */
	private boolean containsParent(File parent, Set<File> set) {
	    while(parent != null) {
	        if(set.contains(parent))
                return true;
            parent = parent.getParentFile();
        }
        return false;
    }
}   