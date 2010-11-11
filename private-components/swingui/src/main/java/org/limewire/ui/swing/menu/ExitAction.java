/**
 * 
 */
package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ExitAction extends AbstractAction {
    
    @Inject
    public ExitAction() {
        super(I18n.tr("E&xit"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ActionMap actionMap = Application.getInstance().getContext().getActionMap();
        Action exitApplication = actionMap.get("exitApplication");
        exitApplication.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutdown"));
    }
}