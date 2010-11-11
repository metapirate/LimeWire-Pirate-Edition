package org.limewire.ui.swing.friends;

import java.awt.Rectangle;

import javax.swing.JLayeredPane;

import org.limewire.friend.api.FriendRequest;
import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;

import com.google.inject.Inject;

public class FriendRequestNotificationPanel extends OverlayPopupPanel {
    
    private final FriendRequestPanel friendRequestPanel;
    
    @Inject
    public FriendRequestNotificationPanel(@GlobalLayeredPane JLayeredPane layeredPane,
                FriendRequestPanel friendRequestPanel) { 
        super(layeredPane, friendRequestPanel);
        
        this.friendRequestPanel = friendRequestPanel;

        resize();
    }

    
    public void addRequest(FriendRequest request) {
        friendRequestPanel.addRequest(request);
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = layeredPane.getBounds();
        int w = 260;
        int h = 100;
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }
    
}
