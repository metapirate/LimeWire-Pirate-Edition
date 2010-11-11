package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Point;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.TablePopupHandler;

import com.google.inject.Provider;

class LibraryPopupHandler implements TablePopupHandler {
    private final LibraryTable libraryTable;
    private final Provider<LibraryPopupMenu> popupMenu;
    
    public LibraryPopupHandler(LibraryTable libraryTab, Provider<LibraryPopupMenu> popupMenu) {
        this.libraryTable = libraryTab;
        this.popupMenu = popupMenu;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        int popupRow = libraryTable.rowAtPoint(new Point(x, y));
        if(popupRow < 0)
            return;
        
        LocalFileItem selectedItem = (LocalFileItem) libraryTable.getModel().getValueAt(popupRow, 0);
        List<LocalFileItem> selectedItems = Collections.unmodifiableList(libraryTable.getSelection());
        
        if(selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            libraryTable.setRowSelectionInterval(popupRow, popupRow);
        }

        popupMenu.get().show(component, x, y);
    }
}
