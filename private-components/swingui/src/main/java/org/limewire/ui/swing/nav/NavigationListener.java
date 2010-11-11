package org.limewire.ui.swing.nav;




/** A listener for navigation.  This is intended to be used to listen to all changes on Navigation. */
public interface NavigationListener {
    
    /** Notification that the selection has changed. */
    public void itemSelected(NavCategory category, NavItem navItem, NavSelectable selectable, NavMediator navMediator);

    /** Notification that an item was removed. */
    public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected);
    
    /** Notification that an item was added. */
    public void itemAdded(NavCategory category, NavItem navItem);
    
    /** Notification that a new category was added. */
    public void categoryAdded(NavCategory category);
    
    /** Notification that the last item in a category was removed. 
     * @param wasSelected TODO*/
    public void categoryRemoved(NavCategory category, boolean wasSelected);
    
}
