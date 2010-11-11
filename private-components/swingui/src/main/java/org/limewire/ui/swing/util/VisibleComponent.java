package org.limewire.ui.swing.util;

import org.limewire.listener.EventListener;

/**
 * Defines a component with that can be made visible or invisible, and enabled
 * or disabled for use.
 */
public interface VisibleComponent {

    /**
     * Adds the specified listener to the list that is notified when the 
     * action enabled state changes.
     */
    void addEnabledListener(EventListener<EnabledType> listener);

    /**
     * Removes the specified listener from the list that is notified when the 
     * action enabled state changes.
     */
    void removeEnabledListener(EventListener<EnabledType> listener);
    
    /**
     * Returns true if the component is enabled for use. 
     */
    boolean isActionEnabled();
    
    /**
     * Sets the visibility of this component.
     */
    void setVisibility(boolean visible);

    /**
     * Toggles the visibility of this component.
     */
    void toggleVisibility();

    /**
     * Adds a listener to this items visibility.
     */
    void addVisibilityListener(EventListener<VisibilityType> listener);

    /**
     * Removes a listener from this items visibility.
     */
    void removeVisibilityListener(EventListener<VisibilityType> listener);

    /**
     * Returns true if this component is currently visible.
     */
    boolean isVisible();
}
