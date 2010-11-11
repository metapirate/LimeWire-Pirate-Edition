package org.limewire.ui.swing.action;

import javax.swing.JMenuItem;

/**
 * This is a callback intended for Actions to let them know when the JMenuItem
 * wrapping them is about to be displayed.  This gives the Action a chance
 * to modify properties on the JMenuItem (its text, tooltip, etc.) if necessary.
 * 
 */
public interface ItemNotifyable {
    void notifyItem(JMenuItem item);
}
