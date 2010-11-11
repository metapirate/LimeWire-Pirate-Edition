package org.limewire.ui.swing.nav;

/**
 * A component in the main panel that can have an item selected when choosing it
 * as the visible component.
 */
public interface NavComponent {
    
    /** Instructs this component to select the given selectable. */
    public void select(NavSelectable selectable);
}
