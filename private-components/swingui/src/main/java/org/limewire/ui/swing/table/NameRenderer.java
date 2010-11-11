package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Renders the name for the given fileItem. If the file is incomplete
 * (incomplete) is appending to the name. If the file is an audio file, the
 * title property is used instead of the name property if it exists.
 */
@LazySingleton
public class NameRenderer extends DefaultLimeTableCellRenderer {

    @Inject
    public NameRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String name = null;
        if (value != null && value instanceof LocalFileItem) {
            LocalFileItem localFileItem = (LocalFileItem) value;
            name = getName(localFileItem);
        }
        
        if (name != null) {
            setText(name);
        } else {
            setText("");
        }

        return this;
    }

    private String getName(LocalFileItem localFileItem) {
        String name = null;

        if (localFileItem.getCategory() == Category.AUDIO) {
            name = localFileItem.getPropertyString(FilePropertyKey.TITLE);
        } else if (localFileItem.getCategory() == Category.VIDEO) {
            name = localFileItem.getFileName();
        }

        if (name == null) {
            name = localFileItem.getPropertyString(FilePropertyKey.NAME);
        }

        if (name == null) {
            name = localFileItem.getFileName();
        }

        if (name != null) {
            if (localFileItem.isIncomplete()) {
                name = I18n.tr("{0} (downloading)", name);
            }
        }
        return name;
    }
}
