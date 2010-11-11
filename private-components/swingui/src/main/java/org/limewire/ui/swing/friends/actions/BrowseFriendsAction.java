package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class BrowseFriendsAction extends AbstractAction {

    public static final String DISPLAY_TEXT = I18nMarker.marktr("Browse Friends' Files");

    private FriendPresenceActions remoteHostActions;

    @Inject
    public BrowseFriendsAction(FriendPresenceActions remoteHostActions) {
        super(I18n.tr(DISPLAY_TEXT));
        this.remoteHostActions = remoteHostActions;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {        
        remoteHostActions.browseAllFriends(true);
    }
}
