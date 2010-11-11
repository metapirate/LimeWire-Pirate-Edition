package org.limewire.ui.swing.friends.chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.FriendPresence;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Draws the list of names to show who is online, what there status is,
 * and whether a chat is currently active.
 */
class ChatFriendRenderer extends JLabel implements TableCellRenderer {
    
    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color selectionColor;
    private @Resource Color lineColor;
    private @Resource Icon closeIcon;
    private @Resource Icon unviewedMessageIcon;
    private @Resource Icon chattingIcon;
    private @Resource Icon availableIcon;
    private @Resource Icon doNotDisturbIcon;
    private @Resource Icon awayIcon;
    
    private final Border emptyBorder;
    private final Border underlineBorder;
    
    private final ConversationPanel conversationPanel;
    
    public ChatFriendRenderer(ConversationPanel conversationPanel) {
        this.conversationPanel = conversationPanel;
        
        GuiUtils.assignResources(this);

        setFont(font);
        setForeground(fontColor);
        setBackground(selectionColor);
        emptyBorder = BorderFactory.createEmptyBorder(0,2,1,2);
        underlineBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0,2,0,2), 
                BorderFactory.createMatteBorder(0,0,1,0, lineColor));
        
        setBorder(emptyBorder);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof ChatFriend) { 
            ChatFriend chatFriend = (ChatFriend) value;
            setIcon(getIcon(chatFriend, table.getMousePosition(), table.getCellRect(row, column, true), table));
            setText(chatFriend.getName());
            if(chatFriend.equals(conversationPanel.getCurrentConversationFriend())) {
                setBorder(underlineBorder);
            } else {
                setBorder(emptyBorder);
            }
        } else {
            setIcon(null);
            setText("");
            setBorder(emptyBorder);
        }
        setOpaque(isSelected);
        return this;
    }
    
    /** 
	 * Returns the correct icon depending on the state of the friend and whether
 	 * the user is currently chatting with them.
	 */
    private Icon getIcon(ChatFriend chatFriend, Point mousePosition, Rectangle cellRectangle, JTable table) {
        //If chatting
        if (chatFriend.isChatting()) {
            // if mouse over close button
            if(displayCloseIcon(mousePosition, cellRectangle)) {
                return closeIcon;
            } else {
                // if new messages but not selected
                if(!chatFriend.equals(conversationPanel.getCurrentConversationFriend()) && chatFriend.hasUnviewedMessages()) {
           			if(chatFriend.isFlashState())
                        return chattingIcon;
                    else
                        return unviewedMessageIcon;
                } else {
                    return chattingIcon;
                }
            }
        }
        FriendPresence.Mode mode = chatFriend.getMode();
        switch(mode) {
        case available:
            return availableIcon;
        case chat:
            return chattingIcon;
        case dnd:
            return doNotDisturbIcon;
        default:
            return awayIcon;
        }
    }
    
    /**
     * Returns true if the given table cell contains the mouse coordinates.
     */
    private boolean displayCloseIcon(Point mousePosition, Rectangle cellRectangle) {
        if(mousePosition == null)
            return false;
        return cellRectangle.contains(mousePosition);
    }
}
