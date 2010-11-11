package org.limewire.ui.swing.util;

import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;

/**
 * Defines the sorting behavior for tables using event lists.
 */
public interface EventListTableSortFormat {

    /**
     * Returns a List of sort keys that define the initial sort order.  The 
     * keys are listed in order of significance, with index 0 being the most 
     * significant.  The method returns an empty list if there is no default 
     * sort order.
     * 
     * @return List of sort keys for initial sort order
     */
    List<SortKey> getDefaultSortKeys();

    /**
     * Returns a List of the secondary sort columns associated with the 
     * specified column.  The columns are listed in order of significance, with
     * index 0 being the most significant.  The method returns an empty list if
     * there are no secondary sort columns.
     * 
     * @param column the model index of the primary column
     * @return List of model indices for secondary columns
     */
    List<Integer> getSecondarySortColumns(int column);
    
    /**
     * Returns a List of sort keys that are always used prior to the user sort.
     * This sort will always supercede the default sort and any user initiated 
     * sort. These keys will never result in a sort indication on a table header. 
	 *
     * Caution must be used in adding too many keys to the preSort. There are 
     * a limited number of columns that will sorted at any given time. A large
     * number of preSort columns may remove some secondarySort columns assigned
     * to a given table column.
     * 
     * @return List of sort keys to always run prior to any additional sort.
     */
    List<SortKey> getPreSortColumns();
}
