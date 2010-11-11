package org.limewire.ui.swing.filter;

import javax.swing.JComponent;

import org.limewire.ui.swing.components.Disposable;

import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Defines a component used to filter items.
 */
public interface Filter<E extends FilterableItem> extends Disposable {

    /**
     * Adds the specified listener to the list that is notified when the 
     * filter changes.
     */
    void addFilterListener(FilterListener<E> listener);
    
    /**
     * Removes the specified listener from the list that is notified when the 
     * filter changes.
     */
    void removeFilterListener(FilterListener<E> listener);
    
    /**
     * Returns an indicator that determines whether the filter is in use.
     */
    boolean isActive();
    
    /**
     * Returns the display text for an active filter.
     */
    String getActiveText();
    
    /**
     * Returns the Swing component that displays the filter controls.
     */
    JComponent getComponent();
    
    /**
     * Returns the matcher/editor used to filter items.
     */
    MatcherEditor<E> getMatcherEditor();
    
    /**
     * Resets the filter.
     */
    void reset();
    
}
