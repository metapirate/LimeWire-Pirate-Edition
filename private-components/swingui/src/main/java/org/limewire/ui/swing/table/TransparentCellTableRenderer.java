package org.limewire.ui.swing.table;

import javax.swing.table.DefaultTableCellRenderer;

public class TransparentCellTableRenderer extends DefaultTableCellRenderer {
    
    @Override
    public boolean isOpaque() {
        return false;
    }

}
