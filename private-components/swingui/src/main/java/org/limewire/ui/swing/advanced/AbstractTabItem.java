package org.limewire.ui.swing.advanced;

import java.util.ArrayList;
import java.util.List;

import org.limewire.ui.swing.options.TabItemListener;

/**
 * Base implementation of a TabItem.
 */
public abstract class AbstractTabItem implements TabItem {

    /** List of listeners that are notified when the selected state changes. */
    private final List<TabItemListener> listeners = new ArrayList<TabItemListener>();

    /** 
     * Adds a listener that is notified when the selected state changes.
     */
    @Override
    public void addTabItemListener(TabItemListener listener) {
        this.listeners.add(listener);
    }

    /** 
     * Removes a listener that is notified when the selected state changes.
     */
    @Override
    public void removeTabItemListener(TabItemListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Returns the identifier of the tab item.
     */
    @Override
    public abstract String getId();

    /**
     * Returns true if this tab item is currently selected.
     */
    @Override
    public boolean isSelected() {
        return false;
    }

    /**
     * Selects this tab item.
     */
    @Override
    public abstract void select();
    
    /**
     * Notifies all registered listeners about the specified selected state. 
     */
    public void fireSelected(boolean selected) {
        for (TabItemListener listener : this.listeners) {
            listener.itemSelected(selected);
        }
    }
}
