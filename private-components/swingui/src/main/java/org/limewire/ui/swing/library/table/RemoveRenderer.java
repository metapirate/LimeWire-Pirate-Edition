package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;

@LazySingleton
class RemoveRenderer extends JPanel implements TableCellRenderer {
    
    @Inject
    public RemoveRenderer(RemoveButton removeButton){       
        add(removeButton);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}
