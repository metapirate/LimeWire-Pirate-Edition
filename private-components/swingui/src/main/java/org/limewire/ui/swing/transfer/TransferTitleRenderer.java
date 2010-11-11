package org.limewire.ui.swing.transfer;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;

import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;

/**
 * Cell renderer for the title column in the transfer tables.
 */
public class TransferTitleRenderer extends DefaultLimeTableCellRenderer {

    protected final TransferRendererResources resources;
    
    /**
     * Constructs an TransferTitleRenderer.
     */
    public TransferTitleRenderer() {
        this.resources = new TransferRendererResources();
        
        setIconTextGap(6);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Set font and foreground.
        resources.decorateComponent(this);
        
        // Set icon and text;
        setIcon(getIcon(value));
        setText(getText(value));
        
        return this;
    }
    
    /**
     * Returns the display icon for the specified value.  The default icon
     * is null.  Subclasses may override this method to provide a suitable
     * icon.
     */
    protected Icon getIcon(Object value) {
        return null;
    }
    
    /**
     * Returns the display text for the specified value.
     */
    protected String getText(Object value) {
        return (value != null) ? String.valueOf(value) : "";
    }
}
