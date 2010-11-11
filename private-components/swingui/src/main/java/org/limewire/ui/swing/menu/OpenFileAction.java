/**
 * 
 */
package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadException;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.DownloadExceptionHandler;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

class OpenFileAction extends AbstractAction {

    private final DownloadListManager downloadListManager;

    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;

    @Inject
    public OpenFileAction(DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler) {
        super( I18n.tr("&Open Torrent..."));
        this.downloadListManager = downloadListManager;
        this.downloadExceptionHandler = downloadExceptionHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<File> files = FileChooser.getInput(GuiUtils.getMainFrame(), I18n.tr("Open Torrent"), I18n
                .tr("Open"), FileChooser.getLastInputDirectory(), JFileChooser.FILES_ONLY,
                JFileChooser.APPROVE_OPTION, true, new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String extension = FileUtils.getFileExtension(f);
                        return f.isDirectory() || "torrent".equalsIgnoreCase(extension);
                    }

                    @Override
                    public String getDescription() {
                        return I18n.tr("\".torrent\" files");
                    }
                });

        if (files != null) {
            for (final File file : files) {
                if (file.exists()) {
                    try {
                        downloadListManager.addTorrentDownload(file, null,
                                false);
                    } catch (DownloadException ex) {
                        downloadExceptionHandler.get().handleDownloadException(
                                new DownloadAction() {
                                    @Override
                                    public void download(File saveDirectory, boolean overwrite)
                                            throws DownloadException {
                                        downloadListManager.addTorrentDownload(
                                                file, saveDirectory, overwrite);
                                    }

                                    @Override
                                    public void downloadCanceled(DownloadException ignored) {
					                    //nothing to do                                        
                                    }

                                }, ex, false);
                    }
                } else {
                    // {0}: name of file
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), I18n.tr("The file {0} does not exist.", file.getName()), I18n.tr("Unable to open torrent"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}