package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PropertiableFileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class NameCategoryRenderer extends DefaultLimeTableCellRenderer implements TableCellRenderer {

    private final Provider<IconManager> iconManager;
    private final CategoryIconManager categoryIconManager;
    
    @Resource private Font font;

    @Inject
    public NameCategoryRenderer(Provider<IconManager> iconManager, 
            CategoryIconManager categoryIconManager) {
        GuiUtils.assignResources(this);
        
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        
        setIconTextGap(5);
        setFont(font);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value instanceof LocalFileItem) {
            LocalFileItem item = (LocalFileItem) value;
            if(item.isIncomplete()) {
                setText(I18n.tr("{0} (downloading)", item.getFileName()));
            } else if(item.getCategory() == Category.AUDIO) {
                setText(getAudioName(item));
            } else {
                setText(item.getFileName());
            }
            setIcon(getIcon(item));
        } else { 
            setText("");
            setIcon(null);
        }
        
        return this;
    }
    
    private Icon getIcon(LocalFileItem item) {
        if(item.getCategory() == Category.DOCUMENT || item.getCategory() == Category.OTHER) {
            return iconManager.get().getIconForFile(item.getFile());
        } else {
            return categoryIconManager.getIcon(item.getCategory());
        }
    }
    
    private String getAudioName(LocalFileItem item) {
        return PropertiableFileUtils.getNameProperty(item, true);
    }
}
