package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

@LazySingleton
public class FileSizeRenderer extends DefaultLimeTableCellRenderer {

    @Inject
    public FileSizeRenderer() {
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value instanceof Long) {
            setText(GuiUtils.formatUnitFromBytes((Long)value)); 
        } else 
            setText("");
        return this;
    }
}
