package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.jdesktop.application.Resource;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.settings.FriendSettings;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

/**
 * Provides JMenuItems to be used for setting available or disabled status for
 * the users. These items are backed by a button group and JCheckBoxMenuItems
 */
public class StatusActions {

    @Resource private Icon available;
    @Resource private Icon doNotDisturb;

    private final Action availableAction;

    private final Action doNotDisturbAction;

    private final JCheckBoxMenuItem availableItem;

    private final JCheckBoxMenuItem doNotDisturbItem;

    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;

    private final ButtonGroup statusButtonGroup = new ButtonGroup();

    @Inject
    public StatusActions(final EventBean<FriendConnectionEvent> friendConnectionEventBean) {

        this.friendConnectionEventBean = friendConnectionEventBean;
        GuiUtils.assignResources(this);

        availableAction = new AbstractAction(I18n.tr("&Available")) {
            {
                putValue(Action.SMALL_ICON, available);
                setEnabled(false);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                FriendConnection friendConnection = EventUtils
                        .getSource(StatusActions.this.friendConnectionEventBean);
                if (friendConnection != null && friendConnection.supportsMode()) {
                    friendConnection.setMode(FriendPresence.Mode.available).addFutureListener(
                            new EventListener<FutureEvent<Void>>() {
                                @Override
                                public void handleEvent(FutureEvent<Void> event) {
                                    if (event.getType() == FutureEvent.Type.SUCCESS) {
                                        FriendSettings.DO_NOT_DISTURB.setValue(false);
                                    }
                                }
                            });
                }
            }
        };

        doNotDisturbAction = new AbstractAction(I18n.tr("&Do Not Disturb")) {
            {
                putValue(Action.SMALL_ICON, doNotDisturb);
                setEnabled(false);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                FriendConnection friendConnection = EventUtils
                        .getSource(StatusActions.this.friendConnectionEventBean);
                if (friendConnection != null && friendConnection.supportsMode()) {
                    friendConnection.setMode(FriendPresence.Mode.dnd).addFutureListener(
                            new EventListener<FutureEvent<Void>>() {
                                @Override
                                public void handleEvent(FutureEvent<Void> event) {
                                    if (event.getType() == FutureEvent.Type.SUCCESS) {
                                        FriendSettings.DO_NOT_DISTURB.setValue(true);
                                    }
                                }
                            });
                }
            }
        };

        this.availableItem = new JCheckBoxMenuItem(availableAction);
        this.doNotDisturbItem = new JCheckBoxMenuItem(doNotDisturbAction);

        updateSignedInStatus();

        FriendSettings.DO_NOT_DISTURB.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        updateSignedInStatus();
                    }
                });
            }
        });
    }

    public void updateSignedInStatus() {
        FriendConnection friendConnection = EventUtils
                .getSource(StatusActions.this.friendConnectionEventBean);
        
        boolean signedIn = friendConnection != null && friendConnection.isLoggedIn()
                && friendConnection.supportsMode();

        if (signedIn) {

            statusButtonGroup.remove(availableItem);
            statusButtonGroup.remove(doNotDisturbItem);
            statusButtonGroup.add(availableItem);
            statusButtonGroup.add(doNotDisturbItem);
            boolean dndBool = FriendSettings.DO_NOT_DISTURB.getValue();
            availableItem.setSelected(!dndBool);
            doNotDisturbItem.setSelected(dndBool);
        } else {
            // removing from button group so that no items are selected while
            // they are disabled.
            statusButtonGroup.remove(availableItem);
            statusButtonGroup.remove(doNotDisturbItem);
            // do not show selections when logged out
            availableItem.setSelected(false);
            doNotDisturbItem.setSelected(false);
        }

        availableAction.setEnabled(signedIn);
        doNotDisturbAction.setEnabled(signedIn);
    }

    public JMenuItem getAvailableMenuItem() {
        return availableItem;
    }

    public JMenuItem getDnDMenuItem() {
        return doNotDisturbItem;
    }
}
