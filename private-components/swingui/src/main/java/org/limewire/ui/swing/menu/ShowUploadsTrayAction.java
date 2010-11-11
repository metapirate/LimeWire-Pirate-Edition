package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Action to show Uploads in the tray.
 */
class ShowUploadsTrayAction extends AbstractAction {

    private final Provider<TransferTrayNavigator> transferTrayNavigator;

    @Inject
    public ShowUploadsTrayAction(Provider<TransferTrayNavigator> transferTrayNavigator) {
        super(I18n.tr("Uploads"));
        this.transferTrayNavigator = transferTrayNavigator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        transferTrayNavigator.get().selectUploads();
    }
}
