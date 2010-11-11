package org.limewire.ui.swing.library.navigator;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.util.I18n;


public class LibraryNavItem {

    public static enum NavType {
        LIBRARY, PUBLIC_SHARED, LIST
    }
    
    private final NavType navType;
    private final LocalFileList localFileList;
    private final SharedFileList sharedFileList; // null if it can't be typed to this.
    private Category selectedCategory;
    private String filterText;
    
    public LibraryNavItem(LocalFileList localFileList) {
        this.localFileList = localFileList;
        if(localFileList instanceof SharedFileList) {
            this.sharedFileList = (SharedFileList)localFileList;
            this.navType = sharedFileList.isPublic() ? NavType.PUBLIC_SHARED : NavType.LIST;
        } else {
            this.sharedFileList = null;
            this.navType = NavType.LIBRARY;
        }
        this.selectedCategory = null;
        this.filterText = "";
    }
    
    public LocalFileList getLocalFileList() {
        return localFileList;
    }
    
    public int getId() {
        return sharedFileList != null ? sharedFileList.getId() : -1;
    }
    
    /**
	 * Returns the selected category for this NavItem. This may return
	 * null if the ALL category is selected.
	 */
    public Category getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * Sets the category that is currently selected on this NavItem.
     */
    public void setSelectedCategory(Category selectedCategory) {
        this.selectedCategory = selectedCategory;
    }
    
    /**
     * Returns the text displayed in the filter textfield for this NavItem.
     */
    public String getFilteredText() {
        return filterText;
    }
    
    /**
     * Sets the text that is displayed in the filter textfield on this NavItem.
     */
    public void setFilteredText(String filteredText) {
        this.filterText = filteredText;
    }
    
    public String getDisplayText() {
        switch(navType) {
        case LIST:
            return sharedFileList.getCollectionName();
        case LIBRARY:
            return I18n.tr("Library");
        case PUBLIC_SHARED:
            return I18n.tr("Public Shared");
        default:
            throw new IllegalStateException("unknown type: " + navType);
        }
    }
    
    public NavType getType() {
        return navType;
    }
    
    public boolean isShared() {        
        if (getType() == NavType.PUBLIC_SHARED){
            return true;
        }
        
        if(sharedFileList != null){
            return sharedFileList.getFriendIds().size() > 0;
        }
        
        return false;
    }
}
