package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

class LibraryNavTableRenderer extends JLabel implements TableCellRenderer {
    
    private Border border;
    private Border dropBorder;
    private @Resource Color selectedColor;
    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource int iconGap;
    private @Resource Icon libraryIcon;
    private @Resource Icon publicIcon;
    private @Resource Icon listIcon;
    private @Resource Icon listSharedIcon;
    private @Resource Color dropBackgroundColor;
    private @Resource Color dropBorderColor;
    
    @Inject
    public LibraryNavTableRenderer() {        
        GuiUtils.assignResources(this);
        
        border = BorderFactory.createEmptyBorder(5,6,5,6);
        dropBorder = new CompoundBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, dropBorderColor), 
                BorderFactory.createEmptyBorder(3,4,3,4));
        
        setBackground(selectedColor);
        setFont(font);
        setIconTextGap(iconGap);
        setForeground(fontColor);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        if(value instanceof LibraryNavItem) {
            LibraryNavItem item = (LibraryNavItem) value;
            setText(item.getDisplayText());
            setToolTipText(item.getDisplayText());
            setIconType(item);
        } else {
            setText("");
            setToolTipText("");
            setIcon(null);
        }
        
        JTable.DropLocation dropLocation = table.getDropLocation();
        if(dropLocation != null             
                && dropLocation.getRow() == row
                && dropLocation.getColumn() == column) {
            setOpaque(true);
            setBorder(dropBorder);
            setBackground(dropBackgroundColor);
        } else {
            setBorder(border);
            setOpaque(isSelected);
            setBackground(selectedColor);
        }
           
        return this;
    }
    
    private void setIconType(LibraryNavItem item) {
        if(item.getType() == NavType.LIBRARY)
            setIcon(libraryIcon);
        else if(item.getType() == NavType.PUBLIC_SHARED)
            setIcon(publicIcon);
        else {
            if(item.getLocalFileList() instanceof SharedFileList) {
                if(((SharedFileList)item.getLocalFileList()).getFriendIds().size() > 0)
                    setIcon(listSharedIcon);
                else
                    setIcon(listIcon);
            } else {
                setIcon(listIcon);
            }
        }
    }
}
