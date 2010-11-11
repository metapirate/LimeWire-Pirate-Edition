package org.limewire.ui.swing.table;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JTable;

import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.StringUtils;

public class MultilineTooltipRenderer extends DefaultLimeTableCellRenderer {

    private static final Dimension INFINITE_SIZE = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static final String AND_MORE_MSG = I18n.tr("...and more...");
    
    @SuppressWarnings("unchecked")
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!(value instanceof List)) {
            setText("");
            setToolTipText("");
            return this;
        }
        
        List<Object> lines = (List<Object>) value;
        
        StringBuilder builder = new StringBuilder("<html>");
        builder.append(StringUtils.explode(lines, "<br/>", 14, 60, AND_MORE_MSG));
        builder.append("</html>");
        setToolTipText(builder.toString());
        
        // Only should show the first line in the table...
        if (lines.size() > 0) {
            setText(String.valueOf(lines.get(0)));
        }
        
        if (lines.size() > 1) {
            // Overriden so these cells always exceed their table's space restrictions and thus
            //  always show tooltips and subsequent lines.
            setPreferredSize(INFINITE_SIZE);
        } else {
            setPreferredSize(new Dimension(FontUtils.getPixelWidth(getText(), getFont()), Integer.MAX_VALUE));
        }
        
        return this;
    }
}
