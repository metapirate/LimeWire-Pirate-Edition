package org.limewire.ui.swing.nav;

/** A listener for a single NavItem. */
public interface NavItemListener {
    
    /** Notification that the selection has changed. */
    public void itemSelected(boolean selected);
    
    /** Notification that this NavItem has been removed. 
     * @param wasSelected TODO*/
    public void itemRemoved(boolean wasSelected);
}
