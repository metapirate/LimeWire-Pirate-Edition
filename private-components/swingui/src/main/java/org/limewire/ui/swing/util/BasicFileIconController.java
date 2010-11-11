package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;

/**
 * A FileIconController that uses default icons.
 */
public class BasicFileIconController extends NonBlockFileIconController {
    
    private final CategoryIconManager categoryIconManager;
    private final CategoryManager categoryManager;
    
    @Inject
    public BasicFileIconController(CategoryIconManager categoryIconManager, CategoryManager categoryManager) {
        this.categoryIconManager = categoryIconManager;
        this.categoryManager = categoryManager;
    }
    
    /** Returns the icon associated with the extension of the file. */
    public Icon getIconForFile(File f) {
        if(f == null)
            return null;
        
        String extension = FileUtils.getFileExtension(f);
        
        return getIconForExtension(extension);
    }
    
    /** Returns the icon associated with the extension. */
    public Icon getIconForExtension(String ext) {
        Category category = null;
        if (ext != null) {
            category = categoryManager.getCategoryForExtension(ext);
        }
        
        return this.categoryIconManager.getIcon(category);
    }

    /** Icons are always available immediately. */
    public boolean isIconForFileAvailable(File f) {
        return true;
    }
}
