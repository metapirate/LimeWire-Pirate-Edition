package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Updates whether or not to show uploads in the transfers tray.
 */
public class ShowUploadsInTrayAction extends AbstractAction {
    private final Provider<TransferTrayNavigator> transferTrayNavigator;

    @Inject
    public ShowUploadsInTrayAction(Provider<TransferTrayNavigator> transferTrayNavigator) {
        super(I18n.tr("Show Uploads in Tray"));
        this.transferTrayNavigator = transferTrayNavigator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(!UploadSettings.SHOW_UPLOADS_IN_TRAY.getValue()) {
            transferTrayNavigator.get().selectUploads();
        } else {
            UploadSettings.SHOW_UPLOADS_IN_TRAY.setValue(false);
        }
    }
}
