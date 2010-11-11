package org.limewire.ui.swing.table;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.util.PropertyUtils;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class MouseableTable extends StripedJXTable {

    private TablePopupHandler popupHandler;

    private TableDoubleClickHandler rowDoubleClickHandler;

    private TableColumnDoubleClickHandler columnDoubleClickHandler;

    private TableColors colors = newTableColors();

    private TableCellHeaderRenderer defaultRenderer;

    protected MouseMotionListener mouseOverEditorListener;

    public MouseableTable() {
        initialize();
    }

    protected TableColors newTableColors() {
        return new TableColors();
    }

    public TableColors getTableColors() {
        return colors;
    }

    public MouseableTable(TableModel model) {
        super(model);
        initialize();
    }

    public void setPopupHandler(TablePopupHandler popupHandler) {
        this.popupHandler = popupHandler;
    }

    public void setDoubleClickHandler(TableDoubleClickHandler tableDoubleClickHandler) {
        this.rowDoubleClickHandler = tableDoubleClickHandler;
    }

	public void setColumnDoubleClickHandler(TableColumnDoubleClickHandler columnDoubleClickHandler) {
        this.columnDoubleClickHandler = columnDoubleClickHandler;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        if (row > -1 && col > -1) {
            return getToolTipText(row, col);
        }

        return null;
    }

    /**
     * Returns the tooltip text for the item at the given row and column.
     * The default implementation only shows the tooltip text if the text is clipped. 
     */
    protected String getToolTipText(int row, int col) {
        Object value = getValueAt(row, col);
        JComponent renderer = getRendererComponent(row, col, value);

        if (value != null && isClipped(renderer, col)) {
            String toolTip = renderer.getToolTipText();

            if (toolTip != null) {
                return toolTip;
            } else if (renderer instanceof JLabel) {
                // works for DefaultTableCellRenderer
                return ((JLabel) renderer).getText();
            }

            return PropertyUtils.getToolTipText(value);
        }
        
        return null;
    }

    /**
     * Checks if the renderer fits in the column.
     * 
     * @param row the view index of the row
     * @param col the view index of the column
     * @return true if the column width is less than the preferred width of the
     *         renderer
     */
    private boolean isClipped(JComponent renderer, int col) {
        return renderer.getPreferredSize().width > getColumnModel().getColumn(col).getWidth();
    }

    private JComponent getRendererComponent(int row, int col, Object value) {
        TableCellRenderer tcr = getCellRenderer(row, col);
        return (JComponent) tcr.getTableCellRendererComponent(this, value, false, false, row, col);
    }
      


    protected void initialize() {	
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);        
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);
        setTableHeaderRenderer();
        setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        setFont(colors.getTableFont());

        //HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
        setHighlighters(colors.getEvenHighlighter(), 
                colors.getOddHighlighter(),
                new ColorHighlighter(new MenuHighlightPredicate(this), colors.menuRowColor,  colors.menuRowForeground, colors.menuRowColor, colors.menuRowForeground));

        setGridColor(colors.getGridColor());

        //so that mouseovers will work within table		
        mouseOverEditorListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Get the table cell that the mouse is over.
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                
                // If the cell is editable and
                // it's not already being edited ...
                if (isCellEditable(row, col) && (row != getEditingRow() || col != getEditingColumn())) {
                    editCellAt(row, col);
                } else {
                    maybeCancelEditing();
                }
            }
        };
        
        addMouseMotionListener(mouseOverEditorListener);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {//adding this to editor messes up popups

                int col = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    if (rowDoubleClickHandler != null || columnDoubleClickHandler != null) {
                        Component component = e.getComponent();
                        //launch file on double click unless the click is on a button
                        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                                && !(component.getComponentAt(e.getPoint()) instanceof JButton)) {
                            if (rowDoubleClickHandler != null) {
                                rowDoubleClickHandler.handleDoubleClick(row);
                            }
                            if (columnDoubleClickHandler != null) {
                                columnDoubleClickHandler.handleDoubleClick(col);
                            }
                        }
                    }
                }
            }
            

            @Override
            public void mouseExited(MouseEvent e) {
                maybeCancelEditing();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int col = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                if (isEditing() && isCellEditable(row, col)) { 
                    TableCellEditor editor = getCellEditor(row, col);
                    if (editor != null) {
                        // force update editor colors
                        prepareEditor(editor, row, col);
                    }                        
                }
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && popupHandler != null) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        popupHandler.maybeShowPopup(
                            e.getComponent(), e.getX(), e.getY());
                        TableCellEditor editor = getCellEditor();
                        if (editor != null) {
                            editor.cancelCellEditing();
                        }
                    }
                }
            }

        });
    }

    //Don't set the cell value when editing is cancelled
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {          
            removeEditor();
        }
    }

    public void setStripeHighlighterEnabled(boolean striped){
        if (striped) {
            // HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
            setHighlighters(
                    colors.getEvenHighlighter(),
                    colors.getOddHighlighter());

        } else {
            setHighlighters(
                    new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor,
                            colors.evenForeground, colors.evenColor,
                            colors.evenForeground),
                    new ColorHighlighter(HighlightPredicate.ODD, colors.evenColor,
                            colors.evenForeground, colors.evenColor,
                            colors.evenForeground),
                    new ColorHighlighter(new MenuHighlightPredicate(this), colors.menuRowColor,
                            colors.menuRowForeground, colors.menuRowColor, colors.menuRowForeground));

        }
    }

    // gets rid of default editor color so that editors are colored by highlighters and selection color is shown
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);
        
        if (compoundHighlighter != null) {
            ComponentAdapter adapter = getComponentAdapter(row, column);
            comp = compoundHighlighter.highlight(comp, adapter);
        }
        
        return comp;
    }
    
    protected void setTableHeaderRenderer() {
        JTableHeader th = getTableHeader();
        th.setDefaultRenderer(getTableCellHeaderRenderer());
    }
    
    private TableCellRenderer getTableCellHeaderRenderer() {
        if(defaultRenderer == null)
            defaultRenderer = new TableCellHeaderRenderer();
        return defaultRenderer;
    }
    
    /**
     * Fills in the top right corner if a scrollbar appears
     * with an empty table header.
     */
    @Override
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane)gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                JTableHeader th = new JTableHeader();
                th.setDefaultRenderer(getTableCellHeaderRenderer());
                // Put a dummy header in the upper-right corner.
                final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
                JPanel cornerComponent = new JPanel(new BorderLayout());
                cornerComponent.add(renderer, BorderLayout.CENTER);
                scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
            }
        }
    }
          
    /**
     * @return whether or not a popup menu is showing on the row
     */
    public boolean isMenuShowing(int row) {
    	if(popupHandler != null) {
    		return popupHandler.isPopupShowing(row);
    	}
    	return false;
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return getColumnModel().getColumn(col).getCellEditor() != null;
    }

    /**
     * Does this row have a popup menu showing?
     */
    private static class MenuHighlightPredicate implements HighlightPredicate {

        private MouseableTable table;

        public MenuHighlightPredicate(MouseableTable table) {
            this.table = table;
        }

        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            if (!adapter.getComponent().isEnabled())
                return false;

            return table.isMenuShowing(adapter.row);
        }
    }

    @Override
    public void setDefaultEditor(Class clazz, TableCellEditor editor) {
        boolean usesEventTableModel = getModel() instanceof DefaultEventTableModel;
        boolean usesAdvancedTableFormat = false;
        TableFormat tableFormat = null;

        if (usesEventTableModel) {
            tableFormat = ((DefaultEventTableModel)getModel()).getTableFormat();
            usesAdvancedTableFormat =
                tableFormat instanceof AdvancedTableFormat;
        }

        if (usesEventTableModel && usesAdvancedTableFormat) {
            AdvancedTableFormat format = (AdvancedTableFormat) tableFormat;
            for (int i = 0; i < getModel().getColumnCount(); i++) {
                Class columnClass = format.getColumnClass(i);
                if (columnClass == clazz) {
                    getColumnModel().getColumn(i).setCellEditor(editor);
                }
            }
        } else {
            super.setDefaultEditor(clazz, editor);
        }
    }

    @Override
    public void setDefaultRenderer(Class clazz, TableCellRenderer renderer) {
        boolean usesEventTableModel = getModel() instanceof DefaultEventTableModel;
        boolean usesAdvancedTableFormat = false;
        TableFormat tableFormat = null;

        if (usesEventTableModel) {
            tableFormat = ((DefaultEventTableModel)getModel()).getTableFormat();
            usesAdvancedTableFormat =
                tableFormat instanceof AdvancedTableFormat;
        }

        if (usesEventTableModel && usesAdvancedTableFormat) {
            AdvancedTableFormat format = (AdvancedTableFormat) tableFormat;
            for (int i = 0; i < getModel().getColumnCount(); i++) {
                Class columnClass = format.getColumnClass(i);
                if (columnClass == clazz) {
                    getColumnModel().getColumn(i).setCellRenderer(renderer);
                    break;
                }
            }
        } else {
            super.setDefaultRenderer(clazz, renderer);
        }
    }
    
    /**
     * Ensures the given row is visible.
     */
    public void ensureRowVisible(int row) {
        if(row != -1) {
            Rectangle cellRect = getCellRect(row, 0, false);
            Rectangle visibleRect = getVisibleRect();
            if( !visibleRect.intersects(cellRect) )
                scrollRectToVisible(cellRect);
        }

    }
    
    public boolean isColumnVisible(int column) {
        Rectangle cellRect = getCellRect(0, column, false);
        Rectangle visibleRect = getVisibleRect();
        return visibleRect.intersects(cellRect);
    }
    
    //clears mouseover color
    private void maybeCancelEditing() {
        Point mousePosition = getMousePosition();
        if (getCellEditor() != null && 
                (mousePosition == null || rowAtPoint(mousePosition) == -1 || columnAtPoint(mousePosition) == -1)){
            getCellEditor().cancelCellEditing();
        } 
    }

}