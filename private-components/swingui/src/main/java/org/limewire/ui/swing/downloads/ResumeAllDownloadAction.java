package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Resumes all downloads.
 */
class ResumeAllDownloadAction extends AbstractAction {
    private final Provider<DownloadMediator> downloadMediator;

    @Inject
    public ResumeAllDownloadAction(Provider<DownloadMediator> downloadMediator) {
        super(I18n.tr("Resume All"));
        
        this.downloadMediator = downloadMediator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        downloadMediator.get().resumeAll();
    } 
}
