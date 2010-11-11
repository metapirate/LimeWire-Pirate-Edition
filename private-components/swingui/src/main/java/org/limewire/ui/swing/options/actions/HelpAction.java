package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

/**
 * Help Button action, launches a webpage with help information.
 */
public class HelpAction extends AbstractAction {
    
    public HelpAction(Application application) {        
        putValue(Action.NAME, I18n.tr("Help"));
        putValue(Action.SHORT_DESCRIPTION, I18n.tr("Learn more..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        NativeLaunchUtils.openURL("http://www.gnutellaforums.com/");
    }
}
