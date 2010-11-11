package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Cancels all downloads that are in an error state.
 */
class CancelAllErrorDownloadAction extends AbstractAction {
    private final Provider<DownloadMediator> downloadMediator;

    @Inject
    public CancelAllErrorDownloadAction(Provider<DownloadMediator> downloadMediator) {
        super(I18n.tr("All Error"));
        
        this.downloadMediator = downloadMediator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (confirmCancellation(I18n.tr("Cancel all error downloads?"))) {
            downloadMediator.get().cancelError();
        }
    } 
    
    private boolean confirmCancellation(String message){
        return FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), message, I18n.tr("Cancel"),
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}