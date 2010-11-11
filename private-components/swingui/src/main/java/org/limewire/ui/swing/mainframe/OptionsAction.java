package org.limewire.ui.swing.mainframe;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class OptionsAction extends AbstractAction {   
    @Inject
    public OptionsAction() {
        super(I18n.tr("&Options..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
        map.get("showOptionsDialog").actionPerformed(new ActionEvent(this, -1, null));
    }
}
