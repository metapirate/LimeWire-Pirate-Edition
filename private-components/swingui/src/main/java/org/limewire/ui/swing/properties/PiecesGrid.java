package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;

/**
 * Lightweight component for displaying a coloured grid.  
 * 
 * <p> At this moment specialised for displaying piece data, however could be extended. 
 */
public class PiecesGrid extends JXPanel {
    
    private int rows = -1;
    private int columns = -1;
    private Paint[] gridFillPaint = null;
    private final Map<Integer,Paint> borderPaintMap = new HashMap<Integer,Paint>();
    
    public PiecesGrid() {
        init();
    }
    
    public PiecesGrid(int r, int c) {
        resizeGrid(r, c);
    
        init();
    }
    
    private void init() {
        setBackgroundPainter(new GridPainter());
        
        // Just for fun
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if ((e.getModifiers() & MouseWheelEvent.CTRL_MASK) == 0) {
                    return;
                }
                
                int newRows = rows;
                int newColumns = columns;
                if (e.getUnitsToScroll() < 0) {
                    if (columns > 4) {
                        newColumns = columns - 1;
                    }
                    if (rows > 4) {
                        newRows = rows - 1;
                    }
                } else if (e.getUnitsToScroll() > 0) {
                    if (columns < 200) {
                        newColumns = columns + 1;
                    }
                    if (rows < 200 ) {
                        newRows = rows + 1;
                    }
                }
                resizeGrid(newRows, newColumns);
            }
        });
    }
    
    void resizeGrid(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        gridFillPaint = new Paint[rows*columns];
    }
    
    public void clear() {
        clearFillPaints();
        clearBorderPaints();
    }
    
    public void clearFillPaints() {
        gridFillPaint = new Paint[rows*columns];
    }
    
    public void clearBorderPaints() {
        borderPaintMap.clear();
    }
    
    boolean hasGrid() {
        return gridFillPaint != null;
    }
    
    public int getRows() {
        return rows;
    }
    
    public int getColumns() {
        return columns;
    }
    
    public int getCellCount() {
        return gridFillPaint.length;
    }
    
    public void setCellFillPaint(int cell, Paint paint) {
        gridFillPaint[cell] = paint;
    }
    
    public void setCellBorderPaint(int cell, Paint paint) {
        borderPaintMap.put(cell, paint);
    }
    
    public Paint getCellFillPaint(int cell) {
        return gridFillPaint[cell];
    }
    
    public Paint getCellBorderPaint(int cell) {
        return borderPaintMap.get(cell);
    }
    
    @Override
    public void repaint() {
        Painter painter = getBackgroundPainter();
        if (painter instanceof AbstractPainter) {
            ((AbstractPainter)painter).clearCache();
        }
        super.repaint();
    }
    
    /**
     * Hack to aid with alignment of components along the outer edge above 
     *  or below this component.  No way to easily avoid.  Only relevant with
     *  centred x-alignment at this moment.
     */
    public int getInnerMargin() {
        Painter painter = this.getBackgroundPainter();
        if (painter instanceof GridPainter) {
            ((GridPainter)painter).marginUpdated = false;
            return ((GridPainter)painter).marginX;
        }
        return 0;
    }
    
    public boolean isMarginUpdated() {
        Painter painter = this.getBackgroundPainter();
        if (painter instanceof GridPainter) {
            return ((GridPainter)painter).marginUpdated;
        }
        return false;
    }
        
    private static class GridPainter extends AbstractPainter<PiecesGrid> {
        
        /**
         * Variable used for hack to correct the margins of the components under the grid
         *  despite loss of table width due to need for rounding.
         */
        protected int marginX = 0;

        /**
         * Variable used for hack to correct the margins of the components under the grid
         *  despite loss of table width due to need for rounding.
         */
        protected boolean marginUpdated = false;
        
        GridPainter() {
            setCacheable(true);
            setAntialiasing(true);
        }
        
        @Override
        protected void doPaint(Graphics2D g, PiecesGrid grid, int width, int height) {
            if (!grid.hasGrid()) {
                g.setPaint(Color.BLACK);
                g.drawRect(0,0, width-1, height-1);
                return;
            }
            
            int columns = grid.getColumns();
            int rows = grid.getRows();
            
            int cellWidth = (width-2) / columns;
            int cellHeight = (height-2) / rows;

            int actualTableWidth = cellWidth*columns;
            int actualTableHeight = cellHeight*rows;
            
            int alignOffsetX = Math.round(grid.getAlignmentX() * (width-actualTableWidth));
            int alignOffsetY = Math.round(grid.getAlignmentY() * (height-actualTableHeight));

            if (marginX != alignOffsetX) {
                marginX = alignOffsetX;
                marginUpdated = true;
            }
            
            int cellRow = 0;
            for ( int i=0 ; i<grid.getCellCount() ; i++ ) {
                Paint cellPaint = grid.getCellFillPaint(i);
                int cellColumn = i % columns;
                if (i != 0 && i % columns == 0) {
                    cellRow++;
                }
                
                g.setPaint(cellPaint);
                
                g.fillRect(cellColumn*cellWidth+1+alignOffsetX,
                        cellRow*cellHeight+1+alignOffsetY,
                        cellWidth, cellHeight);
                
                
                Paint borderPaint = grid.getCellBorderPaint(i);
                
                if (borderPaint != null) {
                    g.setPaint(borderPaint);
                
                    g.drawRect(cellColumn*cellWidth+1+alignOffsetX,
                        cellRow*cellHeight+1+alignOffsetY,
                        cellWidth-2, cellHeight-2);
                }
                
                g.setPaint(Color.BLACK);
            }
            
            g.setPaint(Color.BLACK);
            g.drawRect(0+alignOffsetX, 0+alignOffsetY, actualTableWidth, actualTableHeight);
            
            for ( int i=1 ; i<columns ; i++ ) {
                g.drawLine(i*cellWidth+alignOffsetX, 0+alignOffsetY, 
                        i*cellWidth+alignOffsetX, actualTableHeight+alignOffsetY);
            }
            
            for ( int i=1 ; i<rows ; i++ ) {
                g.drawLine(0+alignOffsetX, i*cellHeight+alignOffsetY
                        , actualTableWidth+alignOffsetX, i*cellHeight+alignOffsetY);
            }
        }
    }

}
