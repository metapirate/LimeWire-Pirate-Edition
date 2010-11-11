package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Any download that is in an active state will be paused.
 */
class PauseAllDownloadAction extends AbstractAction {
    private final Provider<DownloadMediator> downloadMediator;
    
    @Inject
    public PauseAllDownloadAction(Provider<DownloadMediator> downloadMediator) {
        super(I18n.tr("Pause All"));
        
        this.downloadMediator = downloadMediator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        downloadMediator.get().pauseAll();
    } 
}
