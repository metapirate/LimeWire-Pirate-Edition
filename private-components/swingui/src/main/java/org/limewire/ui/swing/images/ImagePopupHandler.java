package org.limewire.ui.swing.images;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.table.LibraryPopupMenu;
import org.limewire.ui.swing.table.TablePopupHandler;

import com.google.inject.Provider;

class ImagePopupHandler implements TablePopupHandler {
    private final ImageList imageList;
    private final Provider<LibraryPopupMenu> popupMenu;
    
    public ImagePopupHandler(ImageList imageList, Provider<LibraryPopupMenu> popupMenu) {
        this.imageList = imageList;
        this.popupMenu = popupMenu;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        int popupRow = imageList.locationToIndex(new Point(x, y));
        if(popupRow < 0)
            return;
        
        LocalFileItem selectedItem = (LocalFileItem) imageList.getModel().getElementAt(popupRow);
        
        List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(imageList.getSelectedItems());

        if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            selectedItems.clear();
            imageList.setSelectedIndex(popupRow);
        } 
        
        popupMenu.get().show(component, x, y);
    }
}

