package org.limewire.ui.swing.filter;

import java.util.List;

import org.limewire.ui.swing.components.Disposable;

/**
 * A utility class to generate debugging information on filters.  FilterDebugger
 * mediates between FilterManager and a FilterableSource to allow the source to
 * retrieve information about the current filters.  FilterDebugger needs to be 
 * initialized with an instance of FilterManager to be able to generate useful 
 * information. 
 */
public class FilterDebugger<E extends FilterableItem> implements Disposable {

    private FilterManager<E> filterManager;
    
    /**
     * Initializes the debugger using the specified filter manager.
     */
    public void initialize(FilterManager<E> filterManager) {
        this.filterManager = filterManager;
    }
    
    /**
     * Returns a string containing a dump of all available filters.  If
     * FilterDebugger has not been initialized, this method returns an empty
     * string.
     */
    public String getFilterString() {
        if (filterManager == null) return "";
        
        // Get list of all filters.
        List<Filter<E>> filterList = filterManager.getFiltersInUse();
        
        // Create dump of all filters.
        StringBuilder buf = new StringBuilder();
        for (Filter<E> filter : filterList) {
            if (buf.length() > 0) buf.append(", "); 
            buf.append(filter.toString());
        }
        
        return buf.toString();
    }

    @Override
    public void dispose() {
        filterManager = null;
    }
}
