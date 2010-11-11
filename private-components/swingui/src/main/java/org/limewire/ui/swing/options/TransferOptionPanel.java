package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.malware.VirusEngine;
import org.limewire.core.settings.MalwareSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.FileSetting;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LabelTextField;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.ui.swing.util.SaveDirectoryHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Downloads Option View.
 */
public class TransferOptionPanel extends OptionPanel {

    private final Provider<IconManager> iconManager;
    private final ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory;
    private final TransferLimitsOptionPanel connectionsOptionPanel;
    private final BitTorrentOptionPanel bitTorrentOptionPanel;
    private final VirusEngine virusEngine;    

    private DownloadsPanel downloadsPanel;
    private TrayPanel trayPanel;
    private TransferPanel transferPanel;
    
    @Inject
    public TransferOptionPanel(Provider<IconManager> iconManager,
            ManageSaveFoldersOptionPanelFactory manageFoldersOptionPanelFactory,
            Provider<TransferLimitsOptionPanel> connectionOptionPanel,
            Provider<BitTorrentOptionPanel> bitTorrentOptionPanel,
            VirusEngine virusEngine) {
        this.iconManager = iconManager;
        this.manageFoldersOptionPanelFactory = manageFoldersOptionPanelFactory;
        this.connectionsOptionPanel = connectionOptionPanel.get();
        this.bitTorrentOptionPanel = bitTorrentOptionPanel.get();
        this.virusEngine = virusEngine;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap, gap 4"));

        add(getDownloadsPanel(), "pushx, growx, wrap");
        add(getTrayPanel(), "pushx, growx, wrap");
        add(getTransfersPanel(), "pushx, growx, wrap");
        add(new JButton(new DialogDisplayAction(this, this.bitTorrentOptionPanel, I18n.tr("Configure Torrent Settings"), I18n.tr("Configure Torrent Settings..."), I18n.tr("Configure torrent settings."))), "wrap");
        
    }
    
    private OptionPanel getDownloadsPanel() {
        if (downloadsPanel == null) {
            downloadsPanel = new DownloadsPanel();
        }
        
        return downloadsPanel;
    }

    private OptionPanel getTrayPanel() {
        if (trayPanel == null) {
            trayPanel = new TrayPanel();
        }
        
        return trayPanel;
    }
    
    private OptionPanel getTransfersPanel() {
        if (transferPanel == null) {
            transferPanel = new TransferPanel();
        }
        
        return transferPanel;
    }
    
    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getDownloadsPanel().setOptionTabItem(tab);
        getTrayPanel().setOptionTabItem(tab);
        getTransfersPanel().setOptionTabItem(tab);
    }

    @Override
    ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        
        result = getDownloadsPanel().applyOptions();
        if (result.isSuccessful())
            result.applyResult(getTrayPanel().applyOptions());
        
        if (result.isSuccessful())
            result.applyResult(getTransfersPanel().applyOptions());
        
        if (result.isSuccessful())
            result.applyResult(bitTorrentOptionPanel.applyOptions());
        
        return result;
    }

    @Override
    boolean hasChanged() {
        return getDownloadsPanel().hasChanged() || getTrayPanel().hasChanged() || getTransfersPanel().hasChanged() || bitTorrentOptionPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        getDownloadsPanel().initOptions();
        getTrayPanel().initOptions();
        getTransfersPanel().initOptions();
        bitTorrentOptionPanel.initOptions();
    }

    /**
     * Defines the Saving sub-panel in the Download options container.
     */
    private class DownloadsPanel extends OptionPanel {

        private String currentSaveDirectory;
        private final LabelTextField downloadSaveTextField;
        private final JButton browseSaveLocationButton;
        private final JCheckBox autoRenameDuplicateFilesCheckBox;
        private final JCheckBox useAntivirusCheckBox;
        private final ManageSaveFoldersOptionPanel saveFolderPanel;
        private final JButton multiLocationConfigureButton;
        private final JRadioButton singleLocationButton;
        private final JRadioButton multiLocationButton;

        public DownloadsPanel() {
            super(I18n.tr("Downloads"));

            ButtonGroup downloadOptions = new ButtonGroup();
            singleLocationButton = new JRadioButton(I18n.tr("Save all downloads to one folder:"));
            multiLocationButton = new JRadioButton(I18n.tr("Save different categories to different folders"));
            singleLocationButton.setOpaque(false);
            multiLocationButton.setOpaque(false);
            downloadOptions.add(singleLocationButton);
            downloadOptions.add(multiLocationButton);

            downloadSaveTextField = new LabelTextField(iconManager);
            downloadSaveTextField.setEditable(false);

            BrowseDirectoryAction directoryAction = new BrowseDirectoryAction(TransferOptionPanel.this, downloadSaveTextField);
            downloadSaveTextField.addMouseListener(directoryAction);
            browseSaveLocationButton = new JButton(directoryAction);
            autoRenameDuplicateFilesCheckBox = new JCheckBox(I18n.tr("If the file already exists, download it with a different name"));
            autoRenameDuplicateFilesCheckBox.setOpaque(false);

            useAntivirusCheckBox = new JCheckBox(I18n.tr("Scan files I download for viruses"));
            useAntivirusCheckBox.setOpaque(false);

            add(singleLocationButton);
            add(downloadSaveTextField, "span, growx");
            add(browseSaveLocationButton, "wrap");

            saveFolderPanel = manageFoldersOptionPanelFactory.create(new OKDialogAction(), new CancelDialogAction());
            ResizeUtils.forceSize(saveFolderPanel, new Dimension(600, 430));
            
            multiLocationConfigureButton = new JButton(new DialogDisplayAction(this,
                    saveFolderPanel, I18n.tr("Download Folders"), I18n.tr("Configure..."), I18n
                            .tr("Configure where different categories are downloaded")));

            add(multiLocationButton);
            add(multiLocationConfigureButton, "wrap");

            add(autoRenameDuplicateFilesCheckBox, "wrap");
            add(useAntivirusCheckBox, "hidemode 3, wrap");

            ActionListener downloadSwitchAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (singleLocationButton.isSelected()) {
                        downloadSaveTextField.setVisible(true);
                        browseSaveLocationButton.setVisible(true);
                        multiLocationConfigureButton.setVisible(false);
                    } else {
                        downloadSaveTextField.setVisible(false);
                        browseSaveLocationButton.setVisible(false);
                        multiLocationConfigureButton.setVisible(true);
                    }
                }
            };
            singleLocationButton.addActionListener(downloadSwitchAction);
            multiLocationButton.addActionListener(downloadSwitchAction);            
        }

        @Override
        ApplyOptionResult applyOptions() {

            if (singleLocationButton.isSelected() && saveFolderPanel.isConfigCustom()) {
                saveFolderPanel.revertToDefault();
            }

            SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.setValue(autoRenameDuplicateFilesCheckBox
                    .isSelected());
            
            if(virusEngine.isSupported()){
                boolean isReEnable = useAntivirusCheckBox.isSelected()
                        && MalwareSettings.VIRUS_SCANNER_ENABLED.getValue() == false;
                
                MalwareSettings.VIRUS_SCANNER_ENABLED.setValue(useAntivirusCheckBox.isSelected());
                
                if (isReEnable){
                    //check for av updates if the user re-enabled the scanner.
                    virusEngine.checkForUpdates();
                }
            }
            
            final String save = downloadSaveTextField.getText();
            if (!save.equals(currentSaveDirectory)) {
                try {
                    File saveDir = new File(save);
                    if (!SaveDirectoryHandler.isDirectoryValid(saveDir)) {
                        if (!saveDir.mkdirs())
                            throw new IOException();
                    }
                    SharingSettings.setSaveDirectory(saveDir);
                    updateMediaSaveDirectories(currentSaveDirectory, save);
                    currentSaveDirectory = save;
                } catch (Exception ioe) {
                    FocusJOptionPane.showMessageDialog(TransferOptionPanel.this, I18n
                            .tr("Could not save download directory, reverted to old directory"),
                            I18n.tr("Save Folder Error"), JOptionPane.ERROR_MESSAGE);
                    downloadSaveTextField.setText(currentSaveDirectory);
                }
            }
            
            return saveFolderPanel.applyOptions();
        }

        /**
         * Updates the save location for all media types.
         */
        private void updateMediaSaveDirectories(String oldSaveDir, String newSaveDir) {
            updateMediaSaveDirectory(Category.AUDIO, oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(Category.VIDEO, oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(Category.IMAGE, oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(Category.DOCUMENT, oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(Category.PROGRAM, oldSaveDir, newSaveDir);
            updateMediaSaveDirectory(Category.OTHER, oldSaveDir, newSaveDir);
        }

        /**
         * Update the save location for the specified media type. If the media
         * save location is equal to the old or new default location, then the
         * media setting is reverted to the default setting.
         */
        private void updateMediaSaveDirectory(Category category, String oldSaveDir,
                String newSaveDir) {
            FileSetting mediaSetting = SharingSettings.getFileSettingForCategory(category);
            if (!mediaSetting.isDefault()) {
                String mediaSaveDir = mediaSetting.get().getAbsolutePath();
                if (oldSaveDir.equals(mediaSaveDir) || newSaveDir.equals(mediaSaveDir)) {
                    mediaSetting.revertToDefault();
                }
            }
        }

        @Override
        boolean hasChanged() {
            return !currentSaveDirectory.equals(downloadSaveTextField.getText())
                    || saveFolderPanel.hasChanged()
                    || singleLocationButton.isSelected()
                    && saveFolderPanel.isConfigCustom()
                    ||MalwareSettings.VIRUS_SCANNER_ENABLED.getValue() != useAntivirusCheckBox.isSelected()
                    || SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue() != autoRenameDuplicateFilesCheckBox.isSelected();
        }
        
        public void setAVGCheckBoxVisible() {
            boolean supported = virusEngine.isSupported();
            useAntivirusCheckBox.setVisible(supported);
        }

        @Override
        public void initOptions() {
            autoRenameDuplicateFilesCheckBox
                    .setSelected(SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue());
            
            useAntivirusCheckBox.setSelected(MalwareSettings.VIRUS_SCANNER_ENABLED.getValue());

            setAVGCheckBoxVisible();
            
            saveFolderPanel.initOptions();
            
            if (saveFolderPanel.isConfigCustom()) {
                multiLocationButton.doClick();
            } else {
                singleLocationButton.doClick();
            }

            try {
                File file = SharingSettings.getSaveDirectory();
                if (file == null) {
                    file = SharingSettings.DEFAULT_SAVE_DIR;
                    if (file == null)
                        throw (new FileNotFoundException());
                }
                currentSaveDirectory = file.getCanonicalPath();
                downloadSaveTextField.setText(file.getCanonicalPath());
            } catch (FileNotFoundException fnfe) {
                // simply use the empty string if we could not get the save
                // directory.
                currentSaveDirectory = "";
                downloadSaveTextField.setText("");
            } catch (IOException ioe) {
                currentSaveDirectory = "";
                downloadSaveTextField.setText("");
            }
        }
    }

    private class TrayPanel extends OptionPanel {

        private JCheckBox closeTrayCheckBox;
        private JCheckBox showBandwidthCheckBox;
        private JCheckBox clearDownloadsCheckBox;
        private JCheckBox clearUploadCheckBox;

        // private JCheckBox deleteFileOnCancelCheckBox;

        public TrayPanel() {
            super(I18n.tr("Tray"));
            closeTrayCheckBox = new JCheckBox(I18n.tr("Close tray when there are no transfers"));
            closeTrayCheckBox.setOpaque(false);

            showBandwidthCheckBox = new JCheckBox(I18n.tr("Show total bandwidth"));
            showBandwidthCheckBox.setOpaque(false);

            clearDownloadsCheckBox = new JCheckBox(I18n
                    .tr("Clear downloads from list when finished"));
            clearDownloadsCheckBox.setOpaque(false);

            clearUploadCheckBox = new JCheckBox(I18n.tr("Clear uploads from list when finished"));
            clearUploadCheckBox.setOpaque(false);

            // we aren't using deleteFileOnCancelCheckBox yet
            // deleteFileOnCancelCheckBox = new
            // JCheckBox(I18n.tr("When I cancel a download, delete the file"));
            // deleteFileOnCancelCheckBox.setOpaque(false);
            // deleteFileOnCancelCheckBox.setVisible(false);

            add(closeTrayCheckBox, "wrap");
            add(showBandwidthCheckBox, "wrap");
            add(clearDownloadsCheckBox, "wrap");
            add(clearUploadCheckBox, "wrap");
            // add(deleteFileOnCancelCheckBox);

        }

        @Override
        ApplyOptionResult applyOptions() {
            SwingUiSettings.HIDE_BOTTOM_TRAY_WHEN_NO_TRANSFERS.setValue(closeTrayCheckBox.isSelected());
            SwingUiSettings.SHOW_TOTAL_BANDWIDTH.setValue(showBandwidthCheckBox.isSelected());
            SharingSettings.CLEAR_DOWNLOAD.setValue(clearDownloadsCheckBox.isSelected());
            SharingSettings.CLEAR_UPLOAD.setValue(clearUploadCheckBox.isSelected());

            // DownloadSettings.DELETE_CANCELED_DOWNLOADS.setValue(
            // deleteFileOnCancelCheckBox.isSelected());
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return (SwingUiSettings.HIDE_BOTTOM_TRAY_WHEN_NO_TRANSFERS.getValue() != closeTrayCheckBox.isSelected())
                || (SwingUiSettings.SHOW_TOTAL_BANDWIDTH.getValue() != showBandwidthCheckBox.isSelected())
                || (SharingSettings.CLEAR_DOWNLOAD.getValue() != clearDownloadsCheckBox.isSelected())
                || (SharingSettings.CLEAR_UPLOAD.getValue() != clearUploadCheckBox.isSelected());
            // || DownloadSettings.DELETE_CANCELED_DOWNLOADS.getValue() !=
            // deleteFileOnCancelCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            closeTrayCheckBox.setSelected(SwingUiSettings.HIDE_BOTTOM_TRAY_WHEN_NO_TRANSFERS.getValue());
            showBandwidthCheckBox.setSelected(SwingUiSettings.SHOW_TOTAL_BANDWIDTH.getValue());
            clearDownloadsCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
            clearUploadCheckBox.setSelected(SharingSettings.CLEAR_UPLOAD.getValue());

            // deleteFileOnCancelCheckBox.setSelected(DownloadSettings.
            // DELETE_CANCELED_DOWNLOADS.getValue());
        }
    }
    
    private class TransferPanel extends OptionPanel {

        public TransferPanel() {
            super(I18n.tr("Upload/Download Limits"));
            
            add(new JLabel(I18n.tr("Set limits on downloads and uploads")), "split");
            add(new JButton(new DialogDisplayAction(this, connectionsOptionPanel, I18n.tr("Transfer Limits"), I18n.tr("Settings..."), I18n.tr("Configure transfer limit settings."))), "wrap");
        }
        
        @Override
        ApplyOptionResult applyOptions() {
            return connectionsOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return connectionsOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            connectionsOptionPanel.initOptions();            
        }
    }
}
