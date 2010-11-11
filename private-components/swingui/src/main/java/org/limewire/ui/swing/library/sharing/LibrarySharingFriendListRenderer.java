package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.Network;
import org.limewire.ui.swing.util.GuiUtils;

class LibrarySharingFriendListRenderer extends DefaultTableCellRenderer {

    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color backgroundColor;
    
    private final JScrollPane scrollPane;
    
    private final Border border = BorderFactory.createEmptyBorder(10,14,10,5);
    
    public LibrarySharingFriendListRenderer(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        
        GuiUtils.assignResources(this);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if(value instanceof Friend) {
            Friend friend = (Friend)value;
            if(friend.getNetwork().getType() != Network.Type.FACEBOOK) {
                setToolTipText(friend.getRenderName() + " <" + friend.getId() + ">");
            } else {
                setToolTipText(friend.getRenderName());
            }
            setText(friend.getRenderName());
        } else if(value instanceof String){
            setText((String)value);
            setToolTipText((String)value);
        } else {
            setText("");
            setToolTipText("");
        }
        setBorder(border);
        setFont(font);
        setForeground(fontColor);
        setOpaque(!scrollPane.getVerticalScrollBar().isVisible());
        setBackground(backgroundColor);

        return this;
    }
}