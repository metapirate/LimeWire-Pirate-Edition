package org.limewire.ui.swing.filter;

import org.limewire.core.api.search.SearchCategory;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Defines the configuration format for a RangeFilter.
 */
interface RangeFilterFormat<E extends FilterableItem> {

    /**
     * Returns the header text.
     */
    String getHeaderText();
    
    /**
     * Returns a Matcher that uses the specified minimum and maximum values 
     * for filtering items.
     */
    Matcher<E> getMatcher(long minValue, long maxValue);
    
    /**
     * Returns an array of range values.
     */
    long[] getValues();
    
    /**
     * Returns a text string for the value at the specified index.
     */
    String getValueText(int valueIndex);
    
    /**
     * Returns true if the range maximum represents an absolute value.
     */
    boolean isMaximumAbsolute();
    
    /**
     * Returns true if the upper limit is enabled.
     */
    boolean isUpperLimitEnabled();
    
    /**
     * Updates the value array based on the specified filter category.  If
     * <code>lowerValue</code> or <code>upperValue</code> are greater than -1,
     * then their values must be included in the value array.  The method
     * returns true if the value array is updated.
     */
    boolean updateValues(SearchCategory filterCategory, long lowerValue, long upperValue);
}
