package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LoginAction extends AbstractAction {

    public static final String DISPLAY_TEXT = I18nMarker.marktr("Sign in");
    
    private final Provider<LoginPopupPanel> friendsSignInPanel;
    
    @Inject
    public LoginAction(Provider<LoginPopupPanel> friendsSignInPanel) {
        super(I18n.tr(DISPLAY_TEXT));

        this.friendsSignInPanel = friendsSignInPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        friendsSignInPanel.get().setVisible(true);
    }
}
