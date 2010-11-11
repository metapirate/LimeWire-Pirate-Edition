package org.limewire.ui.swing.properties;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.LimeWireTorrentProperties;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * A Panel that allows the user to management seeding and bandwidth
 * on a given Torrent.
 */
public class TorrentManagementPanel implements FileInfoPanel {

    private final Torrent torrent;
    private final TorrentManagerSettings torrentSettings;
    private final JPanel component;
    
    private JRadioButton defaultRadioButton;
    private JRadioButton uploadForeverRadioButton;
    private JRadioButton uploadControlRadioButton;
    private JSpinner seedRatioSpinner;
    private JSpinner seedDaysSpinner;
    private JSpinner seedHoursSpinner;
    private JCheckBox limitDownloadBandWidthCheckBox;
    private JCheckBox limitUploadBandwidthCheckBox;
    private JSpinner maxUploadSpeedSpinner;
    private JSpinner maxDownloadSpeedSpinner;
    
    public TorrentManagementPanel(Torrent torrent, TorrentManagerSettings torrentSettings) {
        this.torrent = torrent;
        this.torrentSettings = torrentSettings;
        component = new JPanel(new MigLayout("insets 0"));
        
        init();
        initValues();
    }
    
    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        if (!torrent.isValid()) {
            return false;
        }
        
        float ratio = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_RATIO_LIMIT, -1f);
        int time = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, -1);
        
        if(defaultRadioButton.isSelected()) {
            if(ratio != -1f || time != -1)
                return true;
        } else if(uploadForeverRadioButton.isSelected()) {
            if(ratio != Float.MAX_VALUE || time != Integer.MAX_VALUE)
                return true;
        } else {
            //check this
            if(ratio != ((SpinnerNumberModel)seedRatioSpinner.getModel()).getNumber().floatValue())
                return true;
            if(time != getSeconds(((SpinnerNumberModel)seedDaysSpinner.getModel()).getNumber().intValue(), ((SpinnerNumberModel)seedHoursSpinner.getModel()).getNumber().intValue()))
                return true;
        }
        
        int uploadRate = torrent.getMaxUploadBandwidth();
        int downloadRate = torrent.getMaxDownloadBandwidth();
        
        if(limitDownloadBandWidthCheckBox.isSelected()) {
            if(downloadRate != (((SpinnerNumberModel)maxDownloadSpeedSpinner.getModel()).getNumber().intValue() * 1024))
                return true;
        } else {
            if(downloadRate != -1)
                return true;
        }
        
        if(limitUploadBandwidthCheckBox.isSelected()) {
            if(uploadRate != (((SpinnerNumberModel)maxUploadSpeedSpinner.getModel()).getNumber().intValue() * 1024))
                return true;
        } else {
            if(uploadRate != -1)
                return true;
        }
        return false;
    }

    @Override
    public void save() {
        
        if (!torrent.isValid()) {
            return;
        }
        
        if(defaultRadioButton.isSelected()) {
            torrent.setProperty(LimeWireTorrentProperties.MAX_SEED_RATIO_LIMIT, null);
            torrent.setProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, null);
        } else if(uploadForeverRadioButton.isSelected()) {
            torrent.setProperty(LimeWireTorrentProperties.MAX_SEED_RATIO_LIMIT, Float.MAX_VALUE);
            torrent.setProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, Integer.MAX_VALUE);
        } else {
            torrent.setProperty(LimeWireTorrentProperties.MAX_SEED_RATIO_LIMIT, ((SpinnerNumberModel)seedRatioSpinner.getModel()).getNumber().floatValue());
            torrent.setProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, 
                        (getSeconds(((SpinnerNumberModel)seedDaysSpinner.getModel()).getNumber().intValue(), 
                                    ((SpinnerNumberModel)seedHoursSpinner.getModel()).getNumber().intValue())));
        }
        
        if(limitDownloadBandWidthCheckBox.isSelected()) {
            torrent.setMaxDownloadBandwidth(((SpinnerNumberModel)maxDownloadSpeedSpinner.getModel()).getNumber().intValue() * 1024);
        } else {
            torrent.setMaxDownloadBandwidth(0);
        }
        
        if(limitUploadBandwidthCheckBox.isSelected()) {
            torrent.setMaxUploadBandwidth(((SpinnerNumberModel)maxUploadSpeedSpinner.getModel()).getNumber().intValue() * 1024);
        } else {
            torrent.setMaxUploadBandwidth(0);
        }
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
    }

    @Override
    public void dispose() {
    }
    
    private void initValues() {
        if(torrent == null || !torrent.isEditable()) {
            defaultRadioButton.setSelected(true);
            limitDownloadBandWidthCheckBox.setSelected(false);
            limitUploadBandwidthCheckBox.setSelected(false);            
        } else {
            float ratio = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_RATIO_LIMIT, -1f);
            int time = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, -1);
            int uploadRate = -1;
            int downloadRate = -1;
            
            if (torrent.isValid()) {
                uploadRate = torrent.getMaxUploadBandwidth()/1024;
                downloadRate = torrent.getMaxDownloadBandwidth()/1024;
            }
            
            // seed forever
            if(ratio == Float.MAX_VALUE && time == Integer.MAX_VALUE) {
                uploadForeverRadioButton.setSelected(true);
                initTimeSpinners(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get());
            } else if((ratio >= 0 && ratio != torrentSettings.getSeedRatioLimit()) || (time >= 0 && time != torrentSettings.getSeedTimeLimit())) { // user set values 
                uploadControlRadioButton.setSelected(true);
                seedRatioSpinner.setValue(ratio);
                initTimeSpinners(getTime(torrent));
            } else { // default values 
                defaultRadioButton.setSelected(true);
                initTimeSpinners(getTime(torrent));
            }

            limitUploadBandwidthCheckBox.setSelected(uploadRate >= (Integer)((SpinnerNumberModel)maxUploadSpeedSpinner.getModel()).getMinimum() && uploadRate != torrentSettings.getMaxUploadBandwidth()/1024);
            if(limitUploadBandwidthCheckBox.isSelected())
                maxUploadSpeedSpinner.setValue(uploadRate);
            
            limitDownloadBandWidthCheckBox.setSelected(downloadRate >= (Integer) ((SpinnerNumberModel)maxDownloadSpeedSpinner.getModel()).getMinimum() && downloadRate != torrentSettings.getMaxDownloadBandwidth()/1024);
            if(limitDownloadBandWidthCheckBox.isSelected()) {
                maxDownloadSpeedSpinner.setValue(downloadRate);
            }
        }
    }
    
    private void initTimeSpinners(int time) {
        int wholeDays = getWholeDays(time);
        seedDaysSpinner.setValue(wholeDays);
        seedHoursSpinner.setValue(getRemainderHours(time, wholeDays));  
    }
    
    /**
     * Returns the number of seconds to seed this Torrent. If no value has
     * been set on the Torrent, returns the default seed time within TorrentSettings.
     */
    private int getTime(Torrent torrent) {
        int time = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, -1);
        float defaultTime = BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get();
        if(time == -1) 
            return (int)defaultTime;
        else
            return time;
    }

    private void init() {
        ButtonGroup buttonGroup = new ButtonGroup();
        defaultRadioButton = new JRadioButton(I18n.tr("Use default torrent options"));
        uploadForeverRadioButton = new JRadioButton(I18n.tr("Upload this torrent forever"));
        uploadControlRadioButton = new JRadioButton(I18n.tr("Upload this torrent until either of the following:")); 
        uploadControlRadioButton.setSelected(true);
        
        //add the radio buttons to the component
        addRadioButton(buttonGroup, defaultRadioButton);
        addRadioButton(buttonGroup, uploadForeverRadioButton);
        addRadioButton(buttonGroup, uploadControlRadioButton);
        
        seedRatioSpinner = createSeedRatioSpinner();
        seedDaysSpinner = createDaySpinner();   
        seedHoursSpinner = createHourSpinner();
        
        // add the spinners to the component
        component.add(new JLabel(I18n.tr("Ratio:")), "split 2, gapleft 20");
        component.add(seedRatioSpinner, "span, wrap");
        component.add(new JLabel(I18n.tr("Maximum days:")), "gapleft 20, split 4");
        component.add(seedDaysSpinner, "");
        component.add(new JLabel("Hours:"), "gapleft 20");
        component.add(seedHoursSpinner, "wrap");
        
        uploadControlRadioButton.addItemListener(new EnableSetterListener(seedRatioSpinner, seedDaysSpinner, seedHoursSpinner));
              
        // add separator
        component.add(new JSeparator(), "growx, span, gaptop 10, gapbottom 10, wrap");
        
        limitDownloadBandWidthCheckBox = createCheckBox(I18n.tr("Limit this torrent's download bandwidth"));
        maxDownloadSpeedSpinner = createDownloadSpinner();
        JLabel maxDownloadLabel = new JLabel("KB/s");   
        limitDownloadBandWidthCheckBox.addItemListener(new VisibilitySetterListener(maxDownloadSpeedSpinner, maxDownloadLabel));

        // add download bandwidth limits
        component.add(limitDownloadBandWidthCheckBox);
        component.add(maxDownloadSpeedSpinner);
        component.add(maxDownloadLabel, "wrap");
        
        limitUploadBandwidthCheckBox = createCheckBox(I18n.tr("Limit this torrent's upload bandwidth"));
        maxUploadSpeedSpinner = createUploadSpinner();
        JLabel maxUploadLabel = new JLabel("KB/s");  
        limitUploadBandwidthCheckBox.addItemListener(new VisibilitySetterListener(maxUploadSpeedSpinner, maxUploadLabel));
        
        // add upload bandwidth limits
        component.add(limitUploadBandwidthCheckBox);
        component.add(maxUploadSpeedSpinner);
        component.add(maxUploadLabel, "wrap");
    }
    
    private void addRadioButton(ButtonGroup buttonGroup, JRadioButton button) {
        buttonGroup.add(button);
        button.setOpaque(false);
        component.add(button, "wrap");
    }
    
    private JSpinner createSpinner(SpinnerNumberModel model) {
        JSpinner spinner = new JSpinner(model);
        spinner.setPreferredSize(new Dimension(50, 20));
        spinner.setMaximumSize(new Dimension(60, 20));
        return spinner;
    }
    
    private JCheckBox createCheckBox(String string) {
        JCheckBox checkBox = new JCheckBox(string);
        checkBox.setContentAreaFilled(false);
        checkBox.setSelected(true);
        return checkBox;
    }
    
    /**
     * Creates a Spinner to set the Max Download Bandwidth for a Torrent.
     */
    private JSpinner createDownloadSpinner() {
        SpinnerNumberModel maxSpinnerModel = new SpinnerNumberModel(
                DownloadSettings.MAX_DOWNLOAD_SPEED.getValue()/1024,
                DownloadSettings.MAX_DOWNLOAD_SPEED.getMinValue().intValue()/1024,
                DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.get() ? 
                        (DownloadSettings.MAX_DOWNLOAD_SPEED.getValue()/1024):
                        (DownloadSettings.MAX_DOWNLOAD_SPEED.getMaxValue().intValue()/1024), 1);
        return createSpinner(maxSpinnerModel);
    }
    
    /**
     * Creates a Spinner to set the Max Upload Bandwidth for a Torrent. 
     */
    private JSpinner createUploadSpinner() {
        SpinnerNumberModel maxUploadSpinnerModel = new SpinnerNumberModel(
                UploadSettings.MAX_UPLOAD_SPEED.getValue()/1024, 
                UploadSettings.MAX_UPLOAD_SPEED.getMinValue().intValue()/1024,
                UploadSettings.LIMIT_MAX_UPLOAD_SPEED.get() ? 
                    (UploadSettings.MAX_UPLOAD_SPEED.getValue()/1024) :
                    (UploadSettings.MAX_UPLOAD_SPEED.getMaxValue().intValue()/1024), 1);
        return createSpinner(maxUploadSpinnerModel);
    }
    
    /**
     * Creates a Spinner to set the number of days to seed a Torrent.
     */
    private JSpinner createDaySpinner() {
        int wholeDays = getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get());
        SpinnerNumberModel seedDaysModel = new SpinnerNumberModel(
                wholeDays,
                getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMinValue()),
                getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMaxValue()), 1);
        return createSpinner(seedDaysModel);
    }
    
    /**
     * Creates a Spinner to set the number of hours to seed a Torrent. The max is 24 hours, the 
     * minimum is 0 and the default is 0.
     */
    private JSpinner createHourSpinner() {
        int wholeDays = getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get());
        SpinnerNumberModel seedHoursModel = new SpinnerNumberModel(
                getRemainderHours(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get(), wholeDays),
                0,
                24, 1);
        return createSpinner(seedHoursModel);
    }
    
    /**
     * Creates a Spinner to set the Seed Ratio for a Torrent. The max value is Float.MAX_VALUE, the min 
     * is 0f and the default is 2.0f.
     */
    private JSpinner createSeedRatioSpinner() {
        SpinnerNumberModel seedRatioModel = new SpinnerNumberModel(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                .get().floatValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMinValue()
                .floatValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMaxValue()
                .floatValue(), .05);
        return createSpinner(seedRatioModel);
    }
    
    /**
     * Returns the number of days given a number of seconds.
     */
    private static int getWholeDays(Integer integer) {
        return (int) Math.floor(integer.doubleValue() / (60 * 60 * 24));
    }
    
    /**
     * Returns the number of hours from the number of seconds.
     */
    private static int getRemainderHours(Integer totalSeconds, int days) {
        return (int)Math.round(totalSeconds.doubleValue() / (60 *60) - days*24);
    }
    
    /**
     * Given a number of days and hours, returns the time in seconds.
     */
    private static int getSeconds(int days, int hours) {
        return days*24*60*60 + hours*60*60;
    }
    
    /**
     * Listens to a JToggleButton being selected and updates the isEnabled
     * method of a JComponent
     */
    private static class EnableSetterListener implements ItemListener {
        private final JComponent[] components;
        
        public EnableSetterListener(JComponent... component) {
            this.components = component;
        }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            for(JComponent component : components) {
                component.setEnabled(((JToggleButton)e.getSource()).isSelected());
            }
        }
    }
    
    /**
     * Listens to a JToggleButton being selected and updates the visibility
     * of a set of JComponents.
     */
    private static class VisibilitySetterListener implements ItemListener {
        private final JComponent[] components;

        public VisibilitySetterListener(JComponent... component) {
            this.components = component;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            for(JComponent component : components) {
                component.setVisible(((JToggleButton)e.getSource()).isSelected());
            }
        }
    }
}

