package org.limewire.ui.swing.table;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;

public abstract class TableRendererEditor extends JXPanel implements TableCellRenderer, TableCellEditor {

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    protected final JPanel emptyPanel = new JXPanel();

    @Override
    public final void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis))
                listeners.add(lis);
        }
    }

    @Override
    public final void cancelCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    @Override
    public final void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis))
                listeners.remove(lis);
        }
    }
    
    @Override
    public final Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if (value == null) {
            return emptyPanel;
        }
        return doTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    /**
     * Takes the place of the normal getTableCellRendererComponent(), which in this parent class performs a null check
     * for the cell value.
     */
    protected abstract Component doTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column);

    @Override
    public final Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if (value == null) {
            return emptyPanel;
        }
        return doTableCellEditorComponent(table, value, isSelected, row, column);
    }

    /**
     * Takes the place of the normal getTableCellEditorComponent(), which in
     * this parent class performs a null check for the cell value.
     */
    protected abstract Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column);

    @Override
    public boolean shouldSelectCell(EventObject e) {
        return false;
    }

    @Override
    public final boolean stopCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingStopped(new ChangeEvent(this));
            }
        }
        return true;
    }

}
