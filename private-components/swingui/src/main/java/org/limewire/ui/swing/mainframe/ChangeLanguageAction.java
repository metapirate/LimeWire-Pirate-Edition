package org.limewire.ui.swing.mainframe;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.options.OptionsDialog;

public class ChangeLanguageAction extends AbstractAction {
    public ChangeLanguageAction() {
        // Always English
        super("C&hange Language...");
    }
        
    @Override
    public void actionPerformed(ActionEvent e) {
        ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
        map.get("showOptionsDialog").actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, OptionsDialog.MISC));
    }
}
