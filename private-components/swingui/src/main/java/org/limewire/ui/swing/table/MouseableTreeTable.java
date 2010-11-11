package org.limewire.ui.swing.table;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.limewire.ui.swing.util.PropertyUtils;

public class MouseableTreeTable extends StripedJXTreeTable {

    private final TableColors colors = newTableColors();

    private boolean stripesPainted = false;

    private MouseMotionListener mouseOverEditorListener;

    public MouseableTreeTable() {
        super();
        initialize();
    }

    public MouseableTreeTable(TreeTableModel treeModel) {
        super(treeModel);
        initialize();
    }

    protected TableColors newTableColors() {
        return new TableColors();
    }

    public TableColors getTableColors() {
        return colors;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int col = columnAtPoint(event.getPoint());
        if (row > -1 && col > -1) {
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
    
    // overridden to bugfix checking isHierarchical on -1 column.
    @Override
    public int getEditingRow() {
        return editingColumn != -1 && isHierarchical(editingColumn) ? -1 : editingRow;
    }

    protected void initialize() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);

        // See http://sites.google.com/site/glazedlists/documentation/swingx
        getSelectionMapper().setEnabled(false); // Breaks horribly with
                                                // glazedlists

        // HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
        setHighlighters(colors.getEvenHighlighter(), colors.getOddHighlighter());

        setGridColor(colors.getGridColor());

        // so that mouseovers will work within table
        mouseOverEditorListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Get the table cell that the mouse is over.
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());

                // If the cell is editable and
                // it's not already being edited ...
                if (isCellEditable(row, col)
                        && (row != getEditingRow() || col != getEditingColumn())) {
                    editCellAt(row, col);
                } else {
                    maybeCancelEditing();
                }
            }
        };

        addMouseMotionListener(mouseOverEditorListener);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {// adding this to editor
                                                    // messes up popups
                int col = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    if (isCellEditable(row, col)) {
                        TableCellEditor editor = getCellEditor(row, col);
                        if (editor != null) {
                            // force update editor colors
                            prepareEditor(editor, row, col);
                            // editor.repaint() takes about a second to show
                            // sometimes
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                maybeCancelEditing();
            }
        });
    }

    // Don't set the cell value when editing is cancelled
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            removeEditor();
        }
    }

    // gets rid of default editor color so that editors are colored by
    // highlighters and selection color is shown
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);

        if (compoundHighlighter != null) {
            ComponentAdapter adapter = getComponentAdapter(row, column);
            comp = compoundHighlighter.highlight(comp, adapter);
        }

        return comp;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return getColumnModel().getColumn(col).getCellEditor() != null;
    }

    public void setStripesPainted(boolean painted) {
        stripesPainted = painted;
    }

    /**
     * The parent paints all the real rows then the remaining space is
     * calculated and appropriately painted with grid lines and background
     * colors. These rows are not selectable.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (stripesPainted) {
            super.paintEmptyRows(g);
        }
    }

    // clears mouseover color
    private void maybeCancelEditing() {
        Point mousePosition = getMousePosition();
        if (getCellEditor() != null
                && (mousePosition == null || rowAtPoint(mousePosition) == -1 || columnAtPoint(mousePosition) == -1)) {
            getCellEditor().cancelCellEditing();
        }
    }
}
