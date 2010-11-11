package org.limewire.ui.swing.friends.chat;

import java.awt.Component;
import java.awt.Point;

import org.limewire.ui.swing.table.TablePopupHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Handles right click events on the list of friends and displays 
 * a popup menu when appropriate.
 */
class ChatPopupHandler implements TablePopupHandler {

    private final ChatFriendList chatFriendList;
    private final Provider<ChatPopupMenu> chatPopupMenu;
    
    @Inject
    public ChatPopupHandler(ChatFriendList chatFiendList, Provider<ChatPopupMenu> chatPopupMenu) {
        this.chatFriendList = chatFiendList;
        this.chatPopupMenu = chatPopupMenu;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        int popupRow = chatFriendList.rowAtPoint(new Point(x, y));
        if(popupRow >= 0 && popupRow < chatFriendList.getRowCount())
            chatFriendList.setRowSelectionInterval(popupRow, popupRow);
        
        chatPopupMenu.get().show(component, x, y);
    }

}
