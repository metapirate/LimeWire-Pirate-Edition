package org.limewire.ui.swing.friends.chat;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.FriendPresence;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.PopupCloseButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;

import net.miginfocom.swing.MigLayout;

/**
 * Header in the ChatFrame. Contains the minimize icon and information
 * about the currently selected chat if a conversation is open.
 */
class ChatHeader {
    @Resource private Font textFont;
    @Resource private Color textColor;
    @Resource private Color background;
    @Resource private Icon availableIcon;
    @Resource private Icon doNotDisturbIcon;
    @Resource private Icon awayIcon;
    

    private JPanel panel;
    private JLabel friendNameLabel;

    @Inject
    public ChatHeader(MinimizeAction minimizeAction) {
        GuiUtils.assignResources(this);
        
        panel = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        panel.setBackground(background);
        
        ResizeUtils.forceHeight(panel, 21);
        
        friendNameLabel = new JLabel();
        friendNameLabel.setForeground(textColor);
        friendNameLabel.setFont(textFont);

        IconButton closeButton = new PopupCloseButton(minimizeAction);
        
        panel.add(friendNameLabel, "gapleft 4, gapright 2, wmax 360, push");
        panel.add(closeButton, "gapright 3");
    }
    
    public JComponent getComponent() {
        return panel;
    }
    
    /**
	 * Removes any friend state from the header of the chat frame.
	 */
    public void clearFriend() {
        friendNameLabel.setText("");
        friendNameLabel.setIcon(null);
    }
    
    /**
	 * Sets the friend who the user is currently chatting with. This friend's
	 * information is displayed in the header of the chat frame.
	 */
    public void setFriend(ChatFriend friend) {
        friendNameLabel.setText(getFriendText(friend));
        friendNameLabel.setIcon(getIcon(friend.getMode()));
    }
    
    /**
	 * Returns the icon for the current state of the selected friend.
	 */
    private Icon getIcon(FriendPresence.Mode mode) {
        switch(mode) {
        case available:
            return availableIcon;
        case dnd:
            return doNotDisturbIcon;
        default:
            return awayIcon;
        }
    }
    
    /**
     * Returns the Name of the current friend along with any status message if
     * one is set.
     */
    private String getFriendText(ChatFriend chatFriend) {
        if(chatFriend.getStatus() != null && chatFriend.getStatus().length() > 0) {
            return chatFriend.getName() + " - " + chatFriend.getStatus();
        } else {
            return chatFriend.getName();
        }
    }
}

