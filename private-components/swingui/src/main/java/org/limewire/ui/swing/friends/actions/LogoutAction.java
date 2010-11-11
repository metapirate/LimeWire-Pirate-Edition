package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class LogoutAction extends AbstractAction {

    private final FriendAccountConfigurationManager accountManager;

    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    @Inject
    public LogoutAction(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            FriendAccountConfigurationManager accountManager) {
        super(I18n.tr("Sign out"));
        this.accountManager = accountManager;
        this.friendConnectionEventBean = friendConnectionEventBean;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        accountManager.setAutoLoginConfig(null);
        FriendConnection connection = EventUtils.getSource(friendConnectionEventBean);
        if (connection != null && connection.isLoggedIn()) {
            connection.logout();
        }
    }
}
