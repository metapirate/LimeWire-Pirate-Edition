package org.limewire.ui.swing.library.sharing;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.FriendConnectionEvent.Type;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * The panel in all shared lists that contains either the login view, the list
 * of friends the list is shared with, or a way of editing the list of friends.
 */
@LazySingleton
public class LibrarySharingPanel {

    /**
     * Property string for whether the sharing panel is in edit
     * mode or not.
     */
    public static final String EDIT_MODE = "EDIT_MODE";
    
    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    
    private static final String LOGIN_VIEW = "LOGIN_VIEW";
    private static final String EDITABLE_VIEW = "EDITABLE_VIEW";
    private static final String FRIEND_LIST_VIEW = "FRIEND_LIST_VIEW";
    
    private final Provider<LibrarySharingLoginPanel> loginPanelProvider;
    private final Provider<LibrarySharingFriendListPanel> friendListPanelProvider;
    private final Provider<LibrarySharingEditablePanel> editablePanelProvider;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    
    // these are all lazily created.
    private LibrarySharingLoginPanel loginPanel;
    private LibrarySharingFriendListPanel friendListPanel;
    private LibrarySharingEditablePanel editablePanel;
    
    private final JPanel component;    
    private final CardLayout layout = new CardLayout();    
    private final Map<String, JComponent> layoutMap = new HashMap<String, JComponent>();
    
    private final EventBean<FriendConnectionEvent> connectionEvent;
    private final ListEventListener<String> friendsListener;
    
    private enum View { NONE, LOGIN, FRIEND_LIST, EDIT_LIST }
    private View currentView = View.NONE;
    
    private SharedFileList currentList;
    
    @Inject
    public LibrarySharingPanel(Provider<LibrarySharingLoginPanel> loginPanel,
            Provider<LibrarySharingFriendListPanel> nonEditablePanel,
            Provider<LibrarySharingEditablePanel> editablePanel,
            EventBean<FriendConnectionEvent> connectionEvent,
            Provider<AutoLoginService> autoLoginServiceProvider) {
        this.loginPanelProvider = loginPanel;
        this.friendListPanelProvider = nonEditablePanel;
        this.editablePanelProvider = editablePanel;
        this.connectionEvent = connectionEvent;
        this.friendsListener = new FriendsListener();
        this.autoLoginServiceProvider = autoLoginServiceProvider;
                
        GuiUtils.assignResources(this);
        
        component = new JPanel();
        component.setMaximumSize(new Dimension(134, Integer.MAX_VALUE));
        component.setBackground(backgroundColor);
        component.setVisible(false);        
        component.setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));               
        component.setLayout(layout);
    }
    
    @Inject void register(ListenerSupport<FriendConnectionEvent> connectionEvent,
                          @Named("known") ListenerSupport<FriendEvent> friendSupport) {
        connectionEvent.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                if(component.isVisible()) {
                    if(!isLoggedIn()) {
                        showLoginView();
                    } else if(currentView == View.LOGIN) {
                        if(SwingUiSettings.SHOW_SHARING_OVERLAY_MESSAGE.getValue()) {
                            showEditableView();
                        } else {
                            showFriendListView();
                        }
                    }
                }
            }
        });
        
        friendSupport.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                if(currentView == View.EDIT_LIST) {
                    editablePanel.refreshSelectedList();
                }
            }
        });
    }
    
    void showLoginView() {
        if(!layoutMap.containsKey(LOGIN_VIEW)) {
            loginPanel = loginPanelProvider.get();
            JComponent newComponent = loginPanel.getComponent();
            component.add(newComponent, LOGIN_VIEW);
            layoutMap.put(LOGIN_VIEW, newComponent);
        }         
        currentView = View.LOGIN;
        loginPanel.setLoggingIn(isLoggingIn());
        sharesChanged();
        getComponent().firePropertyChange(EDIT_MODE, true, false);
        layout.show(component, LOGIN_VIEW);
    }
    
    void showEditableView() {
        if(!layoutMap.containsKey(EDITABLE_VIEW)) {
            editablePanel = editablePanelProvider.get();
            JComponent newComponent = editablePanel.getComponent();
            component.add(newComponent, EDITABLE_VIEW);
            layoutMap.put(EDITABLE_VIEW, newComponent);
        }         
        currentView = View.EDIT_LIST;
        editablePanel.editWithSelectedIds(currentList.getFriendIds());
        getComponent().firePropertyChange(EDIT_MODE, false, true);
        layout.show(component, EDITABLE_VIEW);
    }
    
    void showFriendListView() {
        if(!layoutMap.containsKey(FRIEND_LIST_VIEW)) {
            friendListPanel = friendListPanelProvider.get();
            JComponent newComponent = friendListPanel.getComponent();
            component.add(newComponent, FRIEND_LIST_VIEW);
            layoutMap.put(FRIEND_LIST_VIEW, newComponent);
        }
        currentView = View.FRIEND_LIST;
        sharesChanged();
        getComponent().firePropertyChange(EDIT_MODE, true, false);
        layout.show(component, FRIEND_LIST_VIEW);
    }

    /** Stops sharing the current list with any friends. */
    void stopSharing() {
        currentList.setFriendList(Collections.<String>emptyList());
    }
    
    /** Sets the new list of IDs that should be shared. */
    void setFriendIdsForSharing(List<String> friendIds) {
        currentList.setFriendList(friendIds);
    }
    
    private void sharesChanged() {
        if(currentList != null) {
            switch(currentView) {
            case FRIEND_LIST:
                friendListPanel.setSharedFriendIds(currentList.getFriendIds());
                break;
            case LOGIN:
                loginPanel.setSharedFriendIds(currentList.getFriendIds());
                break;
            }
        }
    }
    
    /** Sets a new backing SharedFileList that this sharing panel will use. */
    public void setSharedFileList(SharedFileList newList) {
        if(currentList != newList) {
            if(currentList != null) {
                currentList.getFriendIds().removeListEventListener(friendsListener);
            }
            currentList = newList;
            currentList.getFriendIds().addListEventListener(friendsListener); 
        } 
		// view may have changed based on sign out/sign in event
        if(isLoggedIn()) {
            if(SwingUiSettings.SHOW_SHARING_OVERLAY_MESSAGE.getValue()) {
                showEditableView();
            } else {
                showFriendListView();
            }
        } else {
            showLoginView();
        }
    }
    
    /**
	 * Enable the edit view if possible.
	 */
    public void showEditMode() {
        if(isLoggedIn()) {
            showEditableView();
        }
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    private boolean isLoggedIn() {
        if(connectionEvent.getLastEvent() == null) {
            return false;
        } else {
            return connectionEvent.getLastEvent().getType() == Type.CONNECTED;
        }
    }
    
    private boolean isLoggingIn() {
        if (autoLoginServiceProvider.get().isAttemptingLogin()) {
            return true;
        }
        
        if(connectionEvent.getLastEvent() == null) {
            return false;
        } else {
            return connectionEvent.getLastEvent().getType() == Type.CONNECTING;
        }
    }
    
    private class FriendsListener implements ListEventListener<String> {
        @Override
        public void listChanged(final ListEvent<String> listChanges) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sharesChanged();
                }
            });
        }
    }
}
