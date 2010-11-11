package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatMediator;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.mainframe.ChangeLanguageAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ViewMenu extends DelayedMnemonicMenu {

    private static final String visibleText = I18n.tr("Hide &Chat Window");
    private static final String notVisibleText = I18n.tr("Show &Chat Window");

    private final Provider<LoginPopupPanel> friendsSignInPanelProvider;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    private final Provider<HideTransferTrayAction> hideTransferTrayTrayActionProvider;
    private final Provider<ShowDownloadsTrayAction> showDownloadsTrayActionProvider;
    private final Provider<ShowUploadsTrayAction> showUploadsTrayActionProvider;
    private final Provider<ChatMediator> chatFrameProvider;
    private final Provider<ChangeLanguageAction> changeLanguageActionProvider;
    private final Provider<TransferTrayNavigator> transferTrayNavigator;
    private final Provider<HomeMediator> homeMediatorProvider;
    private final Provider<Navigator> navigatorProvider;

    @Inject
    public ViewMenu(Provider<LoginPopupPanel> friendsSignInPanel,
            Provider<AutoLoginService> autoLoginServiceProvider,
            EventBean<FriendConnectionEvent> friendConnectionEventBean,
            Provider<HideTransferTrayAction> hideTransferTrayTrayActionProvider,
            Provider<ShowDownloadsTrayAction> showHideDownloadTrayAction,
            Provider<ShowUploadsTrayAction> uploadTrayActionProvider,
            Provider<ChatMediator> chatFrameProvider,
            Provider<ChangeLanguageAction> changeLanguageActionProvider,
            Provider<TransferTrayNavigator> transferTrayNavigator,
            Provider<HomeMediator> homeMediatorProvider,
            Provider<Navigator> navigatorProvider) {

        super(I18n.tr("&View"));

        this.friendsSignInPanelProvider = friendsSignInPanel;
        this.hideTransferTrayTrayActionProvider = hideTransferTrayTrayActionProvider;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        this.friendConnectionEventBean = friendConnectionEventBean;

        this.showDownloadsTrayActionProvider = showHideDownloadTrayAction;
        this.showUploadsTrayActionProvider = uploadTrayActionProvider;
        this.chatFrameProvider = chatFrameProvider;
        this.changeLanguageActionProvider = changeLanguageActionProvider;
        this.transferTrayNavigator = transferTrayNavigator;
        this.homeMediatorProvider = homeMediatorProvider;
        this.navigatorProvider = navigatorProvider;
    }

    @Override
    public void createMenuItems() {
        JCheckBoxMenuItem hideTransferTray = new JCheckBoxMenuItem(
                hideTransferTrayTrayActionProvider.get());
        JCheckBoxMenuItem showDownloads = new JCheckBoxMenuItem(showDownloadsTrayActionProvider
                .get());
        JCheckBoxMenuItem showUploads = new JCheckBoxMenuItem(showUploadsTrayActionProvider.get());

        boolean showTransfers = transferTrayNavigator.get().isTrayShowing();
        
        hideTransferTray.setSelected(!showTransfers);
        showDownloads.setSelected(showTransfers
                && transferTrayNavigator.get().isDownloadsSelected());
        showUploads.setSelected(showTransfers && transferTrayNavigator.get().isUploadsSelected());

        ButtonGroup group = new ButtonGroup();
        group.add(hideTransferTray);
        group.add(showDownloads);
        group.add(showUploads);
        
        add(buildShowHomeScreenAction());
        add(buildShowHideChatWindowAction(chatFrameProvider));
        addSeparator();
        add(hideTransferTray);
        add(showDownloads);
        add(showUploads);
        addSeparator();
        add(changeLanguageActionProvider.get());
    }

    /**
     * @return if there is a connection that is either logged in, logging in or
     * a login service provider is attempting to log in.
     */
    private boolean hasActiveConnection() {
        if (autoLoginServiceProvider.get().isAttemptingLogin()) {
            return true;
        }
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null) {
            return friendConnection.isLoggedIn() || friendConnection.isLoggingIn();
        }
        return false;
    }
    
    private boolean isLoggingIn() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        return friendConnection != null && friendConnection.isLoggingIn();
    }

    private Action buildShowHideChatWindowAction(final Provider<ChatMediator> chatFrameProvider) {
        
        Action action = new AbstractAction(chatFrameProvider.get().isVisible() ? visibleText : notVisibleText) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!chatFrameProvider.get().isVisible() && !hasActiveConnection()) {
                        friendsSignInPanelProvider.get().setVisible(true);
                } else {
                    // TODO: nothing happens if we are logging in, seems strange.
                    if (!autoLoginServiceProvider.get().isAttemptingLogin() && !isLoggingIn()) {
                        chatFrameProvider.get().setVisible(!chatFrameProvider.get().isVisible());
                    }
                }
                
            }
        };
        
        return action;
    }
    
    private Action buildShowHomeScreenAction(){
        return new AbstractAction(I18n.tr("&Home Screen")) {
            @Override
           public void actionPerformed(ActionEvent e) {
                navigatorProvider.get().getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                homeMediatorProvider.get().getComponent().loadDefaultUrl();
           }
        };
    }
}
