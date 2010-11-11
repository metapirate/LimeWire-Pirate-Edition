package org.limewire.ui.swing.library.navigator;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JTable;

import org.limewire.ui.swing.table.TablePopupHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

class LibraryNavPopupHandler implements TablePopupHandler {

    private final Provider<LibraryNavPopupMenu> popupMenu;
    private final Provider<LibraryNavigatorTable> table;
    
    @Inject
    public LibraryNavPopupHandler(Provider<LibraryNavigatorTable> table,
            Provider<LibraryNavPopupMenu> popupMenu) {
        this.popupMenu = popupMenu;
        this.table = table;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        JTable navTable = table.get();
        int popupRow = navTable.rowAtPoint(new Point(x, y));
        navTable.setRowSelectionInterval(popupRow, popupRow);
        
        popupMenu.get().show(component, x, y);
    }
}
