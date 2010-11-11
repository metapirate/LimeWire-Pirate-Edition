package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.AddFriendDialog;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class AddFriendAction extends AbstractAction {

    private final EventBean<FriendConnectionEvent> connectionEventBean;

    /**
     * Creates add friend action.
     * <p>
     * Action is disabled if <code>friendConnection</code> is null or does not
     * support adding friends, see
     * {@link FriendConnection#supportsAddRemoveFriend()} or is not logged in.
     * 
     * @param friendConnection can be null, action will be constructed in a
     *        disabled state then
     */
    @Inject
    public AddFriendAction(EventBean<FriendConnectionEvent> connectionEventBean) {
        super(I18n.tr("Add Friend..."));
        this.connectionEventBean = connectionEventBean;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FriendConnection friendConnection = EventUtils.getSource(connectionEventBean);
        assert friendConnection != null;
        new AddFriendDialog(friendConnection);
    }
}
