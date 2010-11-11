package org.limewire.ui.swing.friends;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;

@EagerSingleton
public class FriendRequestNotificationController extends ComponentAdapter {
        
    private FriendRequestNotificationPanel currentPanel = null;

    private final Provider<FriendRequestNotificationPanel> friendRequestNotifiactionPanelProvider;

    @Inject
    public FriendRequestNotificationController(
            Provider<FriendRequestNotificationPanel> friendRequestNotifiactionPanelProvider) {
        this.friendRequestNotifiactionPanelProvider = friendRequestNotifiactionPanelProvider;
    }

    @Inject
    public void register(ListenerSupport<FriendRequestEvent> friendRequestListeners) {
        friendRequestListeners.addListener(new EventListener<FriendRequestEvent>() {
            
            /*  Testing code.  Please leave in until further notice.
            {
             new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    requests();
                }
             }).start();
            }
            
            public void requests() {
                handleEvent(new FriendRequestEvent(new FriendRequest("test", new FriendRequestDecisionHandler() {
                    @Override
                    public void handleDecision(String friendUsername, boolean accepted) {
                    }
                }), org.limewire.friend.api.FriendRequestEvent.Type.REQUESTED));
            }
            */
            
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendRequestEvent event) {
                if (currentPanel == null) {
                    currentPanel = friendRequestNotifiactionPanelProvider.get();
                    // component hidden event comes in to tell us we can show more
                    // warnings.
                    currentPanel.addComponentListener(FriendRequestNotificationController.this);
                }
                    
                currentPanel.addRequest(event.getData());
            }
        });
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        currentPanel.dispose();
        currentPanel = null;
    }
}

