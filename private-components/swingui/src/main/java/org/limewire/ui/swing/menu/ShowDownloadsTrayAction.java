package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Action to show Downloads in the tray.
 */
class ShowDownloadsTrayAction extends AbstractAction {

    private final Provider<TransferTrayNavigator> transferTrayNavigator;

    @Inject
    public ShowDownloadsTrayAction(Provider<TransferTrayNavigator> transferTrayNavigator) {
        super(I18n.tr("Downloads"));
        this.transferTrayNavigator = transferTrayNavigator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        transferTrayNavigator.get().selectDownloads();
    }
}
