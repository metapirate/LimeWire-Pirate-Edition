package org.limewire.ui.swing.library;

import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.TextFilterator;

/**
 * Sets up a filter to work with the TextField filter in the Library
 */
class LocalFileItemFilterator implements TextFilterator<LocalFileItem> {

    private Category category;
    
    public LocalFileItemFilterator(Category category) {
        this.category = category;
    }
    
    @Override
    public void getFilterStrings(List<String> baseList, LocalFileItem fileItem) {
        baseList.add(fileItem.getName());
        baseList.add(String.valueOf(fileItem.getSize()));
        //TODO: change props depending on the category

        for (FilePropertyKey key : FilePropertyKey.values()) {
            Object value = fileItem.getProperty(key);
            if (value != null) {
                baseList.add(value.toString());
            }
        }
        
        if(category == null) {
            baseList.add(fileItem.getCategory().getSingularName());
        }
        baseList.add(fileItem.getFileName());
    }

}
