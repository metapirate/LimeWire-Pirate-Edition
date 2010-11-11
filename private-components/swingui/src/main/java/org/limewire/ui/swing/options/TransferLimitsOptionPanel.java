package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.components.EmbeddedComponentLabel;
import org.limewire.ui.swing.components.PeriodicFieldValidator;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Transfer Limits Option View.
 */
public class TransferLimitsOptionPanel extends OptionPanel {

    private final DownloadsPanel downloadsPanel;
    private final UploadsPanel uploadPanel;
    private final Provider<TorrentManager> torrentManager;
    private final TorrentManagerSettings torrentSettings;

    @Inject
    public TransferLimitsOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        
        this.downloadsPanel = new DownloadsPanel();
        this.uploadPanel = new UploadsPanel();
        
        setLayout(new MigLayout("fill"));
        setOpaque(false);

        add(downloadsPanel, "pushx, growx, wrap");
        add(new JSeparator(), "growx, wrap");
        add(uploadPanel, "pushx, growx, wrap");
        add(new JButton(new OKDialogAction()), "tag ok, alignx right, split 2");
        add(new JButton(new CancelDialogAction()), "tag cancel");
    }

    @Override
    ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        
        result = downloadsPanel.applyOptions();
        if (result.isSuccessful())
            result.applyResult(uploadPanel.applyOptions());

        if (result.isSuccessful() && torrentManager.get().isInitialized() && torrentManager.get().isValid()) {
            BackgroundExecutorService.execute(new Runnable() {
               @Override
                public void run() {
                   torrentManager.get().setTorrentManagerSettings(torrentSettings);
                } 
            });
        }
        return result;
    }

    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getDownloadsPanel().setOptionTabItem(tab);
        getUploadPanel().setOptionTabItem(tab);
    }
    
    private DownloadsPanel getDownloadsPanel() {
        return downloadsPanel;
    }
    

    private UploadsPanel getUploadPanel() {
        return uploadPanel;
    }
    

    @Override
    boolean hasChanged() {
        return downloadsPanel.hasChanged() || uploadPanel.hasChanged();
    }

    @Override
    public void initOptions() {
        downloadsPanel.initOptions();
        uploadPanel.initOptions();
    }

    private class DownloadsPanel extends OptionPanel {

        private static final int MIN_DOWNLOADS = 1;
        private static final int MAX_DOWNLOADS = 999;
        private final JSpinner maxDownloadSpinner;
        private final JSpinner maxDownloadSpeedSpinner;
        private final JCheckBox limitBandWidthCheckBox;
        private final EmbeddedComponentLabel maxDownloadSpeedController;

        public DownloadsPanel() {
            setLayout(new MigLayout("insets 4, fill, nogrid"));
            setOpaque(false);

            maxDownloadSpinner = new JSpinner(new SpinnerNumberModel(MIN_DOWNLOADS, MIN_DOWNLOADS,
                    MAX_DOWNLOADS, 1));

            JFormattedTextField maxDownloadField = ((JSpinner.DefaultEditor)maxDownloadSpinner.getEditor()).getTextField();
            maxDownloadField.addKeyListener(new PeriodicFieldValidator(maxDownloadField));
            
            maxDownloadSpeedSpinner = new JSpinner(new SpinnerNumberModel(
                    DownloadSettings.MAX_DOWNLOAD_SPEED.getValue() / 1024,
                    DownloadSettings.MAX_DOWNLOAD_SPEED.getMinValue().intValue() / 1024,
                    DownloadSettings.MAX_DOWNLOAD_SPEED.getMaxValue().intValue() / 1024, 1));
            
            JFormattedTextField maxDownloadSpeedField = ((JSpinner.DefaultEditor)maxDownloadSpeedSpinner.getEditor()).getTextField();
            maxDownloadSpeedField.addKeyListener(new PeriodicFieldValidator(maxDownloadSpeedField));
            
            limitBandWidthCheckBox = new JCheckBox(I18n.tr("Limit your download bandwidth"));
            limitBandWidthCheckBox.setContentAreaFilled(false);
            maxDownloadSpeedController = new EmbeddedComponentLabel("{c} KB/s", maxDownloadSpeedSpinner);
            maxDownloadSpeedController.setVisible(false);

            limitBandWidthCheckBox.addItemListener(new CheckBoxListener(maxDownloadSpeedController,
                    limitBandWidthCheckBox));

            add(new JLabel(I18n.tr("Maximum downloads at once:")));
            add(maxDownloadSpinner, "wrap");
            add(limitBandWidthCheckBox);
            add(maxDownloadSpeedController, "wrap");
        }

        @Override
        ApplyOptionResult applyOptions() {
            DownloadSettings.MAX_SIM_DOWNLOAD.setValue((Integer) maxDownloadSpinner.getModel()
                    .getValue());
            DownloadSettings.MAX_DOWNLOAD_SPEED
                    .setValue((Integer) maxDownloadSpeedSpinner.getValue() * 1024);
            DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.setValue(limitBandWidthCheckBox.isSelected());
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return DownloadSettings.MAX_SIM_DOWNLOAD.getValue() != (Integer) maxDownloadSpinner
                    .getModel().getValue()
                    || DownloadSettings.MAX_DOWNLOAD_SPEED.getValue() != ((Integer) maxDownloadSpeedSpinner
                            .getValue() * 1024)
                    || DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.getValue() != limitBandWidthCheckBox
                            .isSelected();
        }

        @Override
        public void initOptions() {
            maxDownloadSpeedSpinner.setValue(DownloadSettings.MAX_DOWNLOAD_SPEED.getValue() / 1024);
            limitBandWidthCheckBox
                    .setSelected(DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.getValue());
            maxDownloadSpinner.getModel().setValue(DownloadSettings.MAX_SIM_DOWNLOAD.getValue());
        }
    }

    private static class UploadsPanel extends OptionPanel {

        private static final int MIN_UPLOADS = 0;
        private static final int MAX_UPLOADS = 50;

        private JSpinner maxUploadSpinner;
        private JCheckBox limitBandwidthCheckBox;
        private JSpinner maxUploadSpeedSpinner;
        private final EmbeddedComponentLabel maxUploadSpeedController;

        public UploadsPanel() {
            setLayout(new MigLayout("insets 4, fill, nogrid"));
            setOpaque(false);

            maxUploadSpinner = new JSpinner(new SpinnerNumberModel(MIN_UPLOADS, MIN_UPLOADS,
                    MAX_UPLOADS, 1));
            
            JFormattedTextField maxUploadField = ((JSpinner.DefaultEditor)maxUploadSpinner.getEditor()).getTextField();
            maxUploadField.addKeyListener(new PeriodicFieldValidator(maxUploadField));
            
            limitBandwidthCheckBox = new JCheckBox(I18n.tr("Limit your upload bandwidth"));
            limitBandwidthCheckBox.setContentAreaFilled(false);

            maxUploadSpeedSpinner = new JSpinner(new SpinnerNumberModel(UploadSettings.MAX_UPLOAD_SPEED
                    .getValue()/1024, UploadSettings.MAX_UPLOAD_SPEED.getMinValue().intValue()/1024,
                    UploadSettings.MAX_UPLOAD_SPEED.getMaxValue().intValue()/1024, 1));
            
            JFormattedTextField maxUploadSpeedField = ((JSpinner.DefaultEditor)maxUploadSpeedSpinner.getEditor()).getTextField();
            maxUploadSpeedField.addKeyListener(new PeriodicFieldValidator(maxUploadSpeedField));
            
            maxUploadSpeedController = new EmbeddedComponentLabel("{c} KB/s", maxUploadSpeedSpinner);
            maxUploadSpeedController.setVisible(false);

            limitBandwidthCheckBox.addItemListener(new CheckBoxListener(maxUploadSpeedController,
                    limitBandwidthCheckBox));

            add(new JLabel(I18n.tr("Maximum uploads at once:")));
            add(maxUploadSpinner, "wrap");
            add(limitBandwidthCheckBox);
            add(maxUploadSpeedController, "wrap");
        }

        @Override
        ApplyOptionResult applyOptions() {
            UploadSettings.HARD_MAX_UPLOADS.setValue((Integer) maxUploadSpinner.getModel()
                    .getValue());

            UploadSettings.MAX_UPLOAD_SPEED.setValue((Integer) maxUploadSpeedSpinner.getValue() * 1024);
            UploadSettings.LIMIT_MAX_UPLOAD_SPEED.setValue(limitBandwidthCheckBox.isSelected());

            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return UploadSettings.HARD_MAX_UPLOADS.getValue() != (Integer) maxUploadSpinner
                    .getModel().getValue()
                    || (UploadSettings.MAX_UPLOAD_SPEED.getValue() != (Integer) maxUploadSpeedSpinner
                            .getValue() * 1024)
                    || UploadSettings.LIMIT_MAX_UPLOAD_SPEED.getValue() != limitBandwidthCheckBox
                            .isSelected();
        }

        @Override
        public void initOptions() {
            maxUploadSpeedSpinner.setValue(UploadSettings.MAX_UPLOAD_SPEED.getValue() / 1024);
            maxUploadSpinner.getModel().setValue(UploadSettings.HARD_MAX_UPLOADS.getValue());
            limitBandwidthCheckBox.setSelected(UploadSettings.LIMIT_MAX_UPLOAD_SPEED.getValue());
        }
    }

    private static class CheckBoxListener implements ItemListener {

        private JComponent component;
        private JCheckBox checkBox;

        public CheckBoxListener(JComponent slider, JCheckBox checkBox) {
            this.component = slider;
            this.checkBox = checkBox;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            component.setVisible(checkBox.isSelected());
        }
    }
}
