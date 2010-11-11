package org.limewire.ui.swing.advanced;

import javax.swing.JPanel;

import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.EnabledType;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

/**
 * A base panel for tab content in the Advanced Tools window.
 */
public abstract class TabPanel extends JPanel implements Disposable {
    
    /** List of listeners notified when the tabEnabled state changes. */
    private final EventListenerList<EnabledType> enabledListenerList =
            new EventListenerList<EnabledType>();
    
    /**
     * Constructs a TabPanel.
     */
    public TabPanel() {
        super();
    }
    
    /**
     * Adds the specified listener to the list that is notified when the 
     * tabEnabled state changes.
     */
    public void addEnabledListener(EventListener<EnabledType> listener) {
        enabledListenerList.addListener(listener);
    }
    
    /**
     * Removes the specified listener from the list that is notified when the 
     * tabEnabled state changes.
     */
    public void removeEnabledListener(EventListener<EnabledType> listener) {
        enabledListenerList.removeListener(listener);
    }
    
    /**
     * Returns true if the tab content is enabled.
     */
    public abstract boolean isTabEnabled();

    /**
     * Performs startup tasks for the tab.  This method is called when the 
     * parent window is opened.
     */
    public abstract void initData();

    /**
     * Notifies all registered listeners that the enabled state has changed to
     * the specified value.
     */
    public void fireEnabledChanged(boolean enabled) {
        enabledListenerList.broadcast(EnabledType.valueOf(enabled));
    }
    
}
