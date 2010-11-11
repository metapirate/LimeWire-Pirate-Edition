package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Paints a custom TableHeader for all Mouseable Tables.
 */
public class TableCellHeaderRenderer extends JXLabel implements TableCellRenderer {
    @Resource
    private Color topBorderColor;
    @Resource
    private Color bottomBorderColor;
    @Resource
    private Color topGradientColor;
    @Resource
    private Color bottomGradientColor;
    @Resource
    private Color leftBorderColor;
    @Resource
    private Color rightBorderColor;
    @Resource
    private Color fontColor;
    @Resource
    private Icon downIcon;
    @Resource
    private Icon upIcon;

    private final Font font;
    
    private Icon sortIcon;
    
    /**
     * Constructs a TableCellHeaderRenderer with the default horizontal
     * alignment.
     */
    public TableCellHeaderRenderer() {
        this(LEADING);
    }
    
    /**
     * Constructs a TableCellHeaderRenderer with the specified horizontal
     * alignment.
     */
    public TableCellHeaderRenderer(int horizontalAlignment) {
        GuiUtils.assignResources(this);
        
        switch (horizontalAlignment) {
        case TRAILING: case RIGHT:
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            break;
        default:
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            break;
        }
        font = getFont().deriveFont(Font.BOLD, 11);

        setHorizontalAlignment(horizontalAlignment);
        setHorizontalTextPosition(JLabel.LEFT);
        setForeground(fontColor);
        setBackgroundPainter(new HeaderBackgroundPainter());
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if(value instanceof String)
            setText((String) value);
        else if(value != null)
            setText(value.toString());
        else
            setText("");
        setIcon(sortIcon);
        
        setPreferredSize(new Dimension(20, getPreferredSize().width));
        setFont(font);
        
        if(column >= 0) {
            // show the appropriate arrow if this column is sorted            
            SortOrder order = getSortOrder(table, column);
            if(order == SortOrder.UNSORTED) { 
                setIcon(null);
            } else if(order == SortOrder.ASCENDING) {
                setIcon(upIcon);
            } else {
                setIcon(downIcon);
            }
        }
        
        return this;
    }
    
    /**
     * Returns the sort order associated with the specified JXTable and view
     * column index.  The sort order is meaningful only if the column is the 
     * first sort key column; otherwise, SortOrder.UNSORTED is returned.
     */
    private SortOrder getSortOrder(JTable table, int viewColumn) {
        if(table instanceof GlazedJXTable) {     
            SortController sortController = ((GlazedJXTable)table).getSortController();
            if (sortController == null) {
                return SortOrder.UNSORTED;
            }
            
            List<? extends SortKey> sortKeys = sortController.getSortKeys();
            if (sortKeys == null) {
                return SortOrder.UNSORTED;
            }
            
            SortKey firstKey = SortKey.getFirstSortingKey(sortKeys);
            if ((firstKey != null) && (firstKey.getColumn() == table.convertColumnIndexToModel(viewColumn))) {
                return firstKey.getSortOrder();
            } else {
                return SortOrder.UNSORTED;
            }
        } else 
            if(table instanceof JXTable) {
            return ((JXTable)table).getSortOrder(viewColumn);
        } else {
            return SortOrder.UNSORTED;
        }
    }
    
    // The following methods override the defaults for performance reasons
    @Override
    public void validate() {}
    @Override
    public void revalidate() {}
    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

    
    /**
     * Painter for the background of the header.
     */
    private class HeaderBackgroundPainter extends AbstractPainter<JXLabel> {

        private RectanglePainter<JXLabel> painter;
        
        public HeaderBackgroundPainter() {
            painter = new RectanglePainter<JXLabel>();
            painter.setFillPaint(new GradientPaint(0,0, topGradientColor, 0, 1, bottomGradientColor, false));
            painter.setFillVertical(true);
            painter.setFillHorizontal(true);
            painter.setPaintStretched(true);
            painter.setBorderPaint(null);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXLabel object, int width, int height) {
            painter.paint(g, object, width, height);
            
            // paint the top border
            g.setColor(topBorderColor);
            g.drawLine(0, 0, width, 0);

            //paint the bottom border
            g.setColor(bottomBorderColor);
            g.drawLine(0, height-1, width, height-1);
            
            //paint the left border
            g.setColor(leftBorderColor);
            g.drawLine(0, 0, 0, height-2);

            //paint the bottom border
            g.setColor(rightBorderColor);
            g.drawLine(width-1, 0, width-1, height);
        }
    }
}
