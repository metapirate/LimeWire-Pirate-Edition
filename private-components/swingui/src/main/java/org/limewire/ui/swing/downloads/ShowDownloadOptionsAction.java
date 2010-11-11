package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Opens the Options menu and shows the Download tab.
 */
class ShowDownloadOptionsAction extends AbstractAction {
    @Inject
    public ShowDownloadOptionsAction() {
        super(I18n.tr("More Transfer Options..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
        map.get("showOptionsDialog").actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, OptionsDialog.TRANSFERS));
    }
}
