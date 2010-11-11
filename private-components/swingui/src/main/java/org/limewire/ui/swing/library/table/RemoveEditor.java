package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;

import com.google.inject.Inject;

class RemoveEditor extends JPanel implements TableCellEditor {

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    private LocalFileItem currentEditingItem;
    
    @Inject
    public RemoveEditor(RemoveButton removeButton, final LibraryNavigatorPanel libraryNavigatorPanel) {
        add(removeButton);
        removeButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO: there has to be a cleaner way of handling actions in tables
                //   performing the function on stopCellEditing within the table seems just as 
                //   hacky. Maybe inject everything into the buttonAction and handle it there along
                //   with a mouselistener for selecting the correct row. 
                if(currentEditingItem != null) {
                    LibraryNavItem item = libraryNavigatorPanel.getSelectedNavItem();
                    item.getLocalFileList().removeFile(currentEditingItem.getFile());
                    currentEditingItem = null;
                }
                stopCellEditing();
            }
        });
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value instanceof LocalFileItem)
            currentEditingItem = (LocalFileItem) value;
        else
            currentEditingItem = null;
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis))
                listeners.add(lis);
        }
    }

    @Override
    public void cancelCellEditing() {
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
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis))
                listeners.remove(lis);
        }
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    @Override
    public boolean stopCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingStopped(new ChangeEvent(this));
            }
        }
        return true;
    }
}
