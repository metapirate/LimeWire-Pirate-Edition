package org.limewire.ui.swing.options;


public interface OptionTabItem {

    /** Selects the option item. */
    void select();
    
    /** Returns true if this OptionTabItem is currently selected. */
    boolean isSelected();
    
    /** Returns the id of nav item. */
    String getId();
    
    /**
     * Returns the OptionPanel associated with this tab.
     */
    public OptionPanel getOptionPanel();
    
    /** Adds a NavItemListener. */
    void addTabItemListener(TabItemListener listener);
    
    /** Removes a NavItemListener. */
    void removeTabItemListener(TabItemListener listener);
}
