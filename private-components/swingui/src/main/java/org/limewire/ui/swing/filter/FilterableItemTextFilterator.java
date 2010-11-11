package org.limewire.ui.swing.filter;

import java.util.List;

import org.limewire.core.api.FilePropertyKey;

import ca.odell.glazedlists.TextFilterator;

/**
 * Implementation of TextFilterator for a FilterableItem.  Property values for 
 * all indexable keys, along with the file extension, are added to the list of 
 * strings checked by the filter.  
 */
class FilterableItemTextFilterator<E extends FilterableItem> implements TextFilterator<E> {

    @Override
    public void getFilterStrings(List<String> baseList, E item) {
        // Add file extension to list.
        baseList.add(item.getFileExtension());
        
        for(FilePropertyKey key : FilePropertyKey.getFilterableKeys()) {
            String value = item.getPropertyString(key);
            if (value != null) {
                baseList.add(value);
            }
        }
    }
}
