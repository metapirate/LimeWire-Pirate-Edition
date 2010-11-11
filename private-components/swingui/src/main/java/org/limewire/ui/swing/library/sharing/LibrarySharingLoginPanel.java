package org.limewire.ui.swing.library.sharing;

import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.friends.actions.LoginAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

/** Creates Login Panel for inner sharing Nav. */
class LibrarySharingLoginPanel {
   
    @Resource
    private Font textFont;
    
    private static final String SIGN_IN = "#signin";
    private static final String STOP_SHARING = "#stopsharing";
    
    private final HTMLLabel htmlLabel;    
    private final JPanel component;   
    
    private boolean hasShared;
    private boolean loggedOut;
    
    @Inject
    public LibrarySharingLoginPanel(final Provider<LoginAction> loginAction,
            final Provider<StopSharingAction> stopSharing) {
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("", "134!", ""));
                
        component.setOpaque(false);
        
        htmlLabel = new HTMLLabel("<html>" + I18n.tr("<a href={0}>Sign in</a> to share this list.", SIGN_IN) + "</html>");
        htmlLabel.setHtmlFont(textFont);
        htmlLabel.setOpenUrlsNatively(false);
        htmlLabel.setOpaque(false);
        htmlLabel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (EventType.ACTIVATED == e.getEventType()) {
                    if (e.getDescription().equals(SIGN_IN)) {
                        loginAction.get().actionPerformed(null);
                    } else if (e.getDescription().equals(STOP_SHARING)) {
                        stopSharing.get().actionPerformed(null);
                    }
                }
            }
        });
        component.add(htmlLabel, "width 94%");
    }
    
    public JComponent getComponent() {
        return component;
    }

    /** Sets the new set of people that this list is shared with. */
    void setSharedFriendIds(EventList<String> friendIds) {
        hasShared = !friendIds.isEmpty();
        setMessage();
    }
    
    private void setMessage() {
        if(!loggedOut) {
            htmlLabel.setText("<html>" + I18n.tr("Logging in...") + "</html>");
        } else if(hasShared) {
            htmlLabel.setText("<html>" + I18n.tr("<a href={0}>Sign in</a> to share this list or edit sharing, or <a href={1}>stop sharing</a> it now.", SIGN_IN, STOP_SHARING) + "</html>");
        } else {
            htmlLabel.setText("<html>" + I18n.tr("<a href={0}>Sign in</a> to share this list.", SIGN_IN) + "</html>");
        }
    }

    void setLoggingIn(boolean loggingIn) {
        loggedOut = !loggingIn;
        setMessage();
    }
}
