package org.limewire.ui.swing.util;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;

/**
 * A universal handler for DownloadExceptions generated while
 * performing downloads.
 */
public class DownloadExceptionHandlerImpl implements DownloadExceptionHandler {
    private static final Log LOG = LogFactory.getLog(DownloadExceptionHandlerImpl.class);

    private final SaveLocationManager saveLocationManager;
    private final TransferTrayNavigator transferTrayNavigator;

    /**
     * Constructs a DownloadExceptionHandler with the specified
     * SaveLocationManager.
     */
    @Inject
    public DownloadExceptionHandlerImpl(SaveLocationManager saveLocationManager,
            TransferTrayNavigator transferTrayNavigator) {
        this.saveLocationManager = saveLocationManager;
        this.transferTrayNavigator = transferTrayNavigator;
    }

    /**
     * Handles the supplied DownloadException. The method may take one of
     * several actions: eat the exception, try downloading again using the
     * supplied <code>downloadAction</code>, or popup a dialog to try and save
     * the download in a new location.
     * 
     * @param supportNewSaveFileName - Indicates that the downloader supports a
     *        new saveFileName. If true the user will have the option of picking
     *        a new file name, provided they do not have the setting to
     *        automatically rename the file turned on. Otherwise if it does not
     *        support a new file name, a directory chooser is opened that will
     *        allow the
     */
    public void handleDownloadException(final DownloadAction downLoadAction,
            final DownloadException e, final boolean supportNewSaveFileName) {

        // Create Runnable to execute task on UI thread. This is necessary
        // if the handler method has been invoked from a background thread.
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                handleException(downLoadAction, e, supportNewSaveFileName);
            }
        });
    }

    /**
     * Handles the specified DownloadException. The method may prompt the
     * user for input, and should be executed from the UI thread.
     */
    private void handleException(final DownloadAction downLoadAction,
            final DownloadException e, final boolean supportNewSaveFileName) {

        if (e.getErrorCode() == DownloadException.ErrorCode.DOWNLOAD_CANCELLED) {
            //no error to handle.
            downLoadAction.downloadCanceled(e);
            return;
        }
        
        if (e.getErrorCode() == DownloadException.ErrorCode.FILE_ALREADY_DOWNLOADING) {
            // ignore, just return because we are already downloading this file
            downLoadAction.downloadCanceled(e);
            showErrorMessage(e);
            return;
        }

        // check to make sure this is a DownloadException we can handle
        if ((e.getErrorCode() != DownloadException.ErrorCode.FILE_ALREADY_EXISTS)
                && (e.getErrorCode() != DownloadException.ErrorCode.FILE_IS_ALREADY_DOWNLOADED_TO)) {
            // Create user message.
            downLoadAction.downloadCanceled(e);
            showErrorMessage(e);
            return;
        } else if (e.getErrorCode() == DownloadException.ErrorCode.FILE_IS_ALREADY_DOWNLOADED_TO
                && !supportNewSaveFileName) {
            // prevents infinite loop case where for bit torrent files we can't
            // change the save file at the moment
            downLoadAction.downloadCanceled(e);
            showErrorMessage(e);
            return;
        }

        // select a save file name
        File saveFile = null;
        if (supportNewSaveFileName && SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue()) {
            saveFile = getAutoSaveFile(e);
        } else {
            if (supportNewSaveFileName) {
                saveFile = FileChooser.getSaveAsFile(GuiUtils.getMainFrame(), I18n
                        .tr("Save File As..."), e.getFile());
            } else {
                saveFile = e.getFile();
                if (saveFile != null && saveFile.exists()) {
                    createOverwriteDialogue(saveFile, downLoadAction, e, supportNewSaveFileName);
                    return;
                }
            }

            if (saveFile == null) {
                // null saveFile means user selected cancel
                downLoadAction.downloadCanceled(e);
                return;
            }
        }

        // if the file already exists at this point, the user has already agreed
        // to overwrite it
        download(downLoadAction, supportNewSaveFileName, saveFile, saveFile.exists());
    }

    private void showErrorMessage(final DownloadException e) {
        Object message = null;

        switch (e.getErrorCode()) {
        case FILE_ALREADY_UPLOADING:
            transferTrayNavigator.selectUploads();
            message = I18n.tr("Sorry, this file is already being uploaded.");
            break;
        case FILE_ALREADY_DOWNLOADING:
            transferTrayNavigator.selectDownloads();
            message = I18n.tr("Sorry, this file is already being downloaded.");
            break;
        case DIRECTORY_NOT_WRITEABLE:
        case DIRECTORY_DOES_NOT_EXIST:
        case NOT_A_DIRECTORY:
        case PATH_NAME_TOO_LONG:
            message = I18n.tr("Sorry, you can't download files to this location.");
            break;
        case NO_TORRENT_MANAGER:
            MultiLineLabel label =
                new MultiLineLabel(I18n.tr(
                        "Sorry, there is a problem with torrents.\nPlease try reinstalling LimeWire to solve this problem."));
            HyperlinkButton help =
                new HyperlinkButton(I18n.tr(
                        "You can also ask for help on the forums."));
            help.addActionListener(new UrlAction(
                    "http://www.gnutellaforums.com/"));
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new MigLayout("gapy 15"));
            panel.add(label, "wrap");
            panel.add(help, "wrap");
            panel.validate();
            message = panel;
            break;
        case FILES_STILL_RESUMING:
            message = I18n.tr("Sorry, we are still loading your old downloads.\nPlease wait to add a new download until we are done.");
            break;
        default:
            message = I18n.tr("Sorry, there was a problem downloading your file.");
            break;
        }

        // Log exception and display user message.
        LOG.error(message, e);
        FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), message, I18n.tr("Download"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Iterates through possible file names until an available one is found and
     * is returned.
     */
    private File getAutoSaveFile(final DownloadException e) {
        File saveFile;
        saveFile = e.getFile();
        int index = 1;
        String fileName = FileUtils.getFilenameNoExtension(saveFile.getName());
        String extension = FileUtils.getFileExtension(saveFile);
        while (saveFile.exists() || saveLocationManager.isSaveLocationTaken(saveFile)) {
            String newFileName = fileName + "(" + index + ")";
            if (extension.length() > 0) {
                newFileName += "." + extension;
            }
            saveFile = new File(saveFile.getParentFile(), newFileName);
            index++;
        }
        return saveFile;
    }

    /**
     * Downloads the given file using the supplied download action and handles
     * any possible DownloadExceptions.
     */
    private void download(final DownloadAction downLoadAction, final boolean supportNewSaveFileName,
            File saveFile, boolean overwrite) {
        
        File newSaveFile = supportNewSaveFileName ? saveFile : saveFile.getParentFile();
        
        try {
            downLoadAction.download(newSaveFile, overwrite);
        } catch (DownloadException e1) {
            handleDownloadException(downLoadAction, e1, supportNewSaveFileName);
        }
    }

    private void createOverwriteDialogue(final File overwriteFile, final DownloadAction downLoadAction,
            final DownloadException ex, final boolean supportNewSaveFileName) {

        final JDialog dialog = new LimeJDialog(GuiUtils.getMainFrame());
        dialog.setModalityType(ModalityType.APPLICATION_MODAL);

        final MultiLineLabel message = new MultiLineLabel(I18n
                .tr("File already exists. What do you want to do?"), 400);

        final JTextField filePathField = new JTextField(25);
        filePathField.setEnabled(false);
        filePathField.setText(overwriteFile.getAbsolutePath());

        JToggleButton overwriteButton = null;
        overwriteButton = new JToggleButton(I18n.tr("Overwrite"));
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
                download(downLoadAction, supportNewSaveFileName, overwriteFile, true);
            }
        });

        JButton cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                File oldSaveFile = overwriteFile;
                File oldSaveFileParent = overwriteFile.getParentFile() != null ? overwriteFile.getParentFile() : overwriteFile;

                File newSaveParent = FileChooser.getInputDirectory(GuiUtils.getMainFrame(), I18n.tr("Choose a new folder to save download."), I18n.tr("Select"), oldSaveFileParent);
                
                if (newSaveParent != null && new File(newSaveParent, oldSaveFile.getName()).exists()) {
                    File saveFile = new File(newSaveParent, oldSaveFile.getName());
                    createOverwriteDialogue(saveFile, downLoadAction, ex, supportNewSaveFileName);
                    return;
                } else if(newSaveParent != null) {
                    File saveFile = new File(newSaveParent, oldSaveFile.getName());
                    download(downLoadAction, supportNewSaveFileName, saveFile, false);
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("hidemode 3, gapy 10", "", ""));
        panel.add(message, "span 2, wrap");
        panel.add(filePathField, "grow x, push, wrap");
        panel.add(overwriteButton, "alignx right");
        panel.add(cancelButton);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setVisible(true);
    }
}
