package org.limewire.ui.swing.nav;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Singleton;

@Singleton
class NavigatorImpl implements Navigator {
    
    private static final Log LOG = LogFactory.getLog(NavigatorImpl.class);

    // CoW to allow listeners to remove themselves during iteration
    private final List<NavigationListener> listeners = new CopyOnWriteArrayList<NavigationListener>();
    private final List<NavItemImpl> navItems = new ArrayList<NavItemImpl>();
    private final Map<NavCategory, Integer> categoryCount = new EnumMap<NavCategory, Integer>(NavCategory.class);
    private final List<NavItemImpl> selectionHistory = new ArrayList<NavItemImpl>();
    
    private NavItemImpl selectedItem;    
    
    public NavigatorImpl() {
        for(NavCategory category : NavCategory.values()) {
            categoryCount.put(category, 0);
        }
    }
 
    @Override
    public NavItem createNavItem(NavCategory category, String id, NavMediator navMediator) {
        NavItemImpl item = new NavItemImpl(category, id, navMediator);
        addNavItem(item);
        return item;
    }

    @Override
    public NavItem getNavItem(NavCategory category, String id) {
        for(NavItemImpl item : navItems) {
            LOG.debugf("Returning NavItem for id {0} navItem{1}", id, item);
            if(category.equals(item.category) && id.equals(item.getId())) {
                return item;
            }
        }
        
        return null;
    }

    @Override
    public boolean hasNavItem(NavCategory category, String id) {
        return getNavItem(category, id) != null;
    }
    
    @Override
    public void addNavigationListener(NavigationListener itemListener) {
        listeners.add(itemListener);
        for(NavItemImpl item : navItems) {
            itemListener.itemAdded(item.category, item);
        }
    }

    @Override
    public void removeNavigationListener(NavigationListener itemListener) {
        listeners.remove(itemListener);
    }
    
    @Override
    public boolean goBack() {
        if(selectionHistory.size() < 2) {
            return false;
        } else {
            // Remove the current.
            selectionHistory.remove(selectionHistory.size() - 1);
            // Remove & get the prior.
            NavItem item = selectionHistory.remove(selectionHistory.size() - 1);
            // And select it.
            item.select();
            return true;
        }
    }
    
    public NavItem getSelectedNavItem() { 
        return selectedItem;
    }
    
    @Override
    public void showNothing() {
        if(selectedItem != null) {
            NavItemImpl item = selectedItem;
            selectedItem = null;
            item.fireSelected(false);
            for(NavigationListener listener : listeners) {
                listener.itemSelected(null, null, null, null);
            }
        }
    }
        
    private void addNavItem(NavItemImpl item) {
        LOG.debugf("Adding item {0}", item);
        navItems.add(item);        
        for(NavigationListener listener : listeners) {
            listener.itemAdded(item.category, item);
        }
        
        categoryCount.put(item.category, categoryCount.get(item.category)+1);        
        if(categoryCount.get(item.category) == 1) {
            for(NavigationListener listener : listeners) {
                listener.categoryAdded(item.category);
            }    
        }
    }
    
    /**
     * Removes all instances of item from the history. Returns the item that
     * should be selected in its place, if it was selected.
     */
    private NavItemImpl removeFromHistory(NavItemImpl item) {
        if(!selectionHistory.isEmpty()) {            
            NavItemImpl priorSelection = null;
            boolean found = false;
            for(ListIterator<NavItemImpl> iter = selectionHistory.listIterator(selectionHistory.size()); iter.hasPrevious(); ) {
                NavItemImpl prior = iter.previous();
                if(prior == item) {
                    iter.remove();
                    found = true;
                } else if(found && priorSelection == null) {
                    priorSelection = prior;
                }
            }
            return priorSelection;
        } else {
            return null;
        }
    }
    
    private void addToHistory(NavItemImpl item) {
        selectionHistory.add(item);
        if(selectionHistory.size() > 10) {
            selectionHistory.remove(0);
        }
    }
    
    private void removeNavItem(NavItemImpl item) {
        if(navItems.remove(item)) {
            LOG.debugf("Removed item {0}", item);
            NavItemImpl priorSelected = removeFromHistory(item);

            boolean wasSelected = (selectedItem == item);
            for(NavigationListener listener : listeners) {
                listener.itemRemoved(item.category, item, wasSelected);
                if(wasSelected) {
                    listener.itemSelected(null, null, null, null);
                }
            }            
            if(wasSelected){
                selectedItem = null;
                item.fireSelected(false);
            }            
            item.fireRemoved(wasSelected);
            
            categoryCount.put(item.category, categoryCount.get(item.category)-1);
            if(categoryCount.get(item.category) == 0) {
                for(NavigationListener listener : listeners) {
                    listener.categoryRemoved(item.category, wasSelected);
                }
            }
            
            // if it was selected, and no listeners responded to the prior
            // callbacks by selecting something else, then select
            // the item that was previously selected.
            if(wasSelected && priorSelected != null && selectedItem == null) {
                assert priorSelected.valid;
                selectNavItem(priorSelected, null, false);
            }
        } else {
            LOG.debugf("Item {0} not contained in list.", item);
        }
    }
    
    private void selectNavItem(NavItemImpl item, NavSelectable selectable, boolean addToHistory) {
        if(item != selectedItem) {
            if(addToHistory) {
                addToHistory(item);            
            }
            if(selectedItem != null) {
                selectedItem.fireSelected(false);
            }
            selectedItem = item;
            item.fireSelected(true);
        }
        //We want the listeners alerted even if the already selected item is re-selected (item == selectedItem)
        for (NavigationListener listener : listeners) {
            listener.itemSelected(item.category, item, selectable, item.navMediator);
        }

    }
    
    private class NavItemImpl implements NavItem {
        // CoW to allow listeners to remove themselves during iteration
        private final List<NavItemListener> listeners = new CopyOnWriteArrayList<NavItemListener>();
        private final NavCategory category;
        private final String id;
        private final NavMediator navMediator;
        private boolean valid = true;
        
        public NavItemImpl(NavCategory category, String id, NavMediator navMediator) {
            this.category = category;
            this.id = id;
            this.navMediator = navMediator;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public void remove() {
            valid = false;
            removeNavItem(this);
        }
        
        @Override
        public void select() {
            select(null);
        }
        
        @Override
        public void select(NavSelectable selectable) {
            // need to check this is still valid, there's a race condition when
            // removing all open tabs and the user selecting one prior to
            // to it being removed.
            if(valid)
                selectNavItem(this, selectable, true);                
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
        @Override
        public void addNavItemListener(NavItemListener listener) {
            listeners.add(listener);
        }
        
        @Override
        public void removeNavItemListener(NavItemListener listener) {
            listeners.remove(listener);
        }
        
        void fireSelected(boolean selected) {
            for(NavItemListener listener : listeners) {
                listener.itemSelected(selected);
            }
        }
        
        void fireRemoved(boolean wasSelected) {
            for(NavItemListener listener : listeners) {
                listener.itemRemoved(wasSelected);
            }
        }
        
        @Override
        public boolean isSelected() {
            return selectedItem == this;
        }
    }
}