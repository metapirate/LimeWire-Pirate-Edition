package org.limewire.ui.swing.friends.login;

import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;


public interface XMPPUserEntryLoginPanelFactory {
    public XMPPUserEntryLoginPanel create(FriendAccountConfiguration accountConfig);
}
