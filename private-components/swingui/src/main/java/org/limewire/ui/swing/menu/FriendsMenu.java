package org.limewire.ui.swing.menu;

import javax.swing.JSeparator;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.friends.actions.AddFriendAction;
import org.limewire.ui.swing.friends.actions.BrowseFriendsAction;
import org.limewire.ui.swing.friends.actions.LoginAction;
import org.limewire.ui.swing.friends.actions.LogoutAction;
import org.limewire.ui.swing.friends.actions.StatusActions;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class FriendsMenu extends MnemonicMenu implements DelayedMenuItemCreator  {
   
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    
    private final Provider<ShareListMenu> shareMenuProvider;
    
    private final Provider<BrowseFriendsAction> browseFriendsActionProvider;
    private final Provider<StatusActions> statusActionsProvider;
    private final Provider<AddFriendAction> addFriendActionProvider;
    private final Provider<LoginAction> loginActionProvider;
    private final Provider<LogoutAction> logoutActionProvider;
    
    @Inject
    public FriendsMenu(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            Provider<AutoLoginService> autoLoginServiceProvider,
            Provider<ShareListMenu> shareMenuProvider,
            Provider<BrowseFriendsAction> browseFriendsActionProvider,
            Provider<StatusActions> statusActionsProvider,
            Provider<AddFriendAction> addFriendActionProvider,
            Provider<LoginAction> loginActionProvider,
            Provider<LogoutAction> logoutActionProvider) {
        
        super(I18n.tr("F&riends"));

        this.friendConnectionEventBean = friendConnectionEventBean;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        
        this.shareMenuProvider = shareMenuProvider;
        
        this.browseFriendsActionProvider = browseFriendsActionProvider;
        this.statusActionsProvider = statusActionsProvider;
        this.addFriendActionProvider = addFriendActionProvider;
        this.loginActionProvider = loginActionProvider;
        this.logoutActionProvider = logoutActionProvider;
    }

    @Inject
    void register(ListenerSupport<FriendConnectionEvent> event) {
        event.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                case CONNECT_FAILED:
                case DISCONNECTED:
                    if (isPopupMenuVisible()) {
                        removeAll();
                        createMenuItems();
                        
                        // needed so that the menu does not stay squished after we add in all
                        // the new items.
                        setPopupMenuVisible(false);
                        setPopupMenuVisible(true);
                    }
                    break;
                }
            }
        });
    }

    @Override
    public void createMenuItems() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        boolean signedIn = friendConnection != null && friendConnection.isLoggedIn();
        boolean supportsAddRemoveFriend = signedIn && friendConnection != null
                && friendConnection.supportsAddRemoveFriend();
        boolean supportModeChanges = signedIn && friendConnection != null
                && friendConnection.supportsMode();
        boolean loggingIn = autoLoginServiceProvider.get().isAttemptingLogin()
                || (friendConnection != null && friendConnection.isLoggingIn());

        add(browseFriendsActionProvider.get()).setEnabled(signedIn);
        add(shareMenuProvider.get());
       
        
        if (supportsAddRemoveFriend) {
            add(new JSeparator());
            add(addFriendActionProvider.get());
        }
        
        if (supportModeChanges) {
            add(new JSeparator());
            StatusActions statusActions = statusActionsProvider.get();
            add(statusActions.getAvailableMenuItem());
            add(statusActions.getDnDMenuItem());
        }

        add(new JSeparator());
        if (!signedIn) {
            add(loginActionProvider.get()).setEnabled(!loggingIn);
        } else {
            add(logoutActionProvider.get());
        }
    }
}
