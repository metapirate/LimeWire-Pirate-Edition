package org.limewire.ui.swing.nav;

import javax.swing.JComponent;


/**
 * The main hub for navigation.
 */
public interface Navigator {

    /**
     * Creates a new navigable item in the given category. When the item is
     * selected, the given panel should be rendered.
     * <p>
     * To remove the NavItem, call {@link NavItem#remove()} on the NavItem
     * returned by this, or retrieve the NavItem later by calling
     * {@link #getNavItem(NavCategory, String)}.
     * 
     * @param category the category this belongs in
     * @param id the id that identifies this panel
     * @param panel the panel to display when selected
     * 
     * @return A {@link NavItem} that can be used to select or remove the item
     */
    public NavItem createNavItem(NavCategory category, String id, NavMediator navMediator);

    /**
     * Returns true if a {@link NavItem} exists in the given category with the given id.
     */
    boolean hasNavItem(NavCategory category, String id);

    /**
     * Returns the NavItem for the given id in the given category.
     */
    NavItem getNavItem(NavCategory category, String id);

    /**
     * Returns the currently selected NavItem.
     */
    NavItem getSelectedNavItem();
    
    /**
     * Adds a listener that is notified when a {@link NavItem} is selected,
     * added or removed. When a new listener is installed, it is notified of all
     * existing NavItems via
     * {@link NavigationListener#itemAdded(NavCategory, NavItem, JComponent)},
     * in addition to notifying about future NavItems.
     */
    public void addNavigationListener(NavigationListener itemListener);

    /**
     * Removes the listener from the list of listeners.
     */
    public void removeNavigationListener(NavigationListener itemListener);
    
    /** Selects the prior item in the history. */
    public boolean goBack();

    /** Instructs the navigator to show nothing. This deselects any currently selected item. */
    public void showNothing();
}