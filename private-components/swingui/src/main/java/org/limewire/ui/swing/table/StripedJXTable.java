package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;

/**
 * Paints the grid and background colors for empty rows if the the ViewPort
 * is larger than the number of rows to display.
 * <p>
 * If vertical and or horizontal grid lines are turned off, the corresponding grid
 * line for empty rows will not be painted either. 
 * <p>
 * There's the assumption that to paint the alternating effect, a HighLighter is being
 * used. It also assumes that the row highlighter is either off, or is only using an
 * simple alternating color to paint with.
 */
public class StripedJXTable extends GlazedJXTable {

    private boolean emptyRowsPainted = true;
    
    public StripedJXTable() {
        super();
    }
    
    public StripedJXTable(TableModel tableModel) {
        super(tableModel);
    }
    
    public StripedJXTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        super(tableModel, tableColumnModel);
    }
    
    public StripedJXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }
    
    public void setEmptyRowsPainted(boolean painted){
        emptyRowsPainted = painted;
    }
    
    /**
     * The parent paints all the real rows then the remaining space is calculated
     * and appropriately painted with grid lines and background colors. These 
     * rows are not selectable.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if(emptyRowsPainted)
            paintEmptyRows(g);
    }
    
    /**
     * Paints fake rows to fill the viewport.
     */
    protected void paintEmptyRows(Graphics g) {
        int rowCount = getRowCount();
        Rectangle clip = g.getClipBounds();
        int clipHeight = clip.y + clip.height;
        int tableHeight = getVisibleRect().height;
        int clipViewableRows = clipHeight / rowHeight;
        int tableViewableRows = tableHeight / rowHeight;
        int startDrawingAt = rowCount * rowHeight;
        
        // paint rows and horizontal lines
        if (startDrawingAt < tableHeight) {
            if(clipViewableRows < tableViewableRows) {
                // we need to call repaint in order to trigger things to properly paint.
                // ideally, we would just change the clip and draw...
                // but because tables are generally inside of a JViewport,
                // we can't do that, because the viewport draws to a backbuffer
                // and then blts the changes... so we need to tell the viewport
                // that we need to do our drawing explicitly.
                // it will recall this method with the proper clip,
                // and we can paint!
                repaint(clip.x, startDrawingAt, clip.width, tableHeight - startDrawingAt);
            } else {
                for (int i = rowCount; i <= clipViewableRows; ++i) {
                    g.setColor(getColorForRow(i));
                    g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
                    
                    // paint horizontal rows if they're shown
                    if(getShowHorizontalLines() && i > rowCount) {
                        g.setColor(gridColor);
                        g.drawLine(clip.x, i * rowHeight, clip.x + clip.width, i * rowHeight);
                    }
                }
                
                // paint vertical lines if they're shown
                if (getShowVerticalLines()) {
                    g.setColor(gridColor);
                    TableColumnModel columnModel = getColumnModel();
                    int x = 0;
                    for (int i = 0; i < columnModel.getColumnCount(); ++i) {
                        TableColumn column = columnModel.getColumn(i);
                        x += column.getWidth();
                        g.drawLine(x - 1, rowCount * rowHeight, x - 1, rowCount * rowHeight + clipHeight);
                    }
                }
            }

        }
    }
    
    /**
     * Gets the background color for the row. This is assuming 1) there's no row highlighter or
     * 2) there's only an alternate row highlighter. Anything else and the behaviour is unknown
     * @param row row to paint
     * @return Color to paint with
     */
    protected Color getColorForRow(int row) {
      return (row % 2 == 0) ? getHighlighterColor(0) : getHighlighterColor(1);
    }
        
    private Color getHighlighterColor(int index){
        if(getHighlighters() == null || getHighlighters().length == 0)
            return getBackground();
        else if(getHighlighters()[0] instanceof CompoundHighlighter) {
            if( ((CompoundHighlighter)getHighlighters()[0]).getHighlighters().length <= index)
                return getBackground();
            else
                return ((ColorHighlighter)((CompoundHighlighter)getHighlighters()[0]).getHighlighters()[index]).getBackground();
        }        
        else if(getHighlighters()[0] instanceof ColorHighlighter) {
            if(getHighlighters().length <= index)
                return getBackground();
            else
                return ((ColorHighlighter)getHighlighters()[index]).getBackground();

        }
        return getBackground();
    }
}
