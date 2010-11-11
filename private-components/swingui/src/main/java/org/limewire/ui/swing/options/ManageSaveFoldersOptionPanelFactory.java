package org.limewire.ui.swing.options;

import javax.swing.Action;

import org.limewire.ui.swing.options.actions.CancelDialogAction;

public interface ManageSaveFoldersOptionPanelFactory {
    
    ManageSaveFoldersOptionPanel create(Action okAction, CancelDialogAction cancelAction);
}
