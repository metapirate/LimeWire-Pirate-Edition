package org.limewire.ui.swing.advanced;

import org.limewire.ui.swing.options.TabItemListener;

/**
 * Defines a tab item.
 * 
 * <p><b>NOTE</b>: This is a simplified version of OptionTabItem, which could
 * be refactored to extend this.</p>
 */
public interface TabItem {
    
    /** 
     * Adds a listener that is notified when the selected state changes.
     */
    void addTabItemListener(TabItemListener listener);
    
    /**
     * Removes a listener that is notified when the selected state changes.
     */
    void removeTabItemListener(TabItemListener listener);
    
    /**
     * Returns the identifier of the tab item.
     */
    String getId();
    
    /**
     * Returns true if this tab item is currently selected.
     */
    boolean isSelected();

    /**
     * Selects this tab item.
     */
    void select();
}
