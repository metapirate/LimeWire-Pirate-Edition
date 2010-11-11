package org.limewire.ui.swing.options;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.PeriodicFieldValidator;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * BitTorrent Option View.
 */
public class BitTorrentOptionPanel extends OptionPanel {

    private final Provider<TorrentManager> torrentManager;
    private final TorrentManagerSettings torrentSettings;
    
    private final JRadioButton uploadTorrentsForeverButton;
    private final JRadioButton uploadTorrentsControlButton;
    private final SpinnerNumberModel seedRatioModel;
    private final JSpinner seedRatioSpinner;
    private final SpinnerNumberModel seedDaysModel;
    private final SpinnerNumberModel seedHoursModel;
    private final JSpinner seedDaysSpinner;
    private final JSpinner seedHoursSpinner;
    private final JCheckBox chooseTorrentsCheckBox;

    @Inject
    public BitTorrentOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        
        setLayout(new MigLayout("fill"));
        setOpaque(false);
        uploadTorrentsForeverButton = new JRadioButton(I18n.tr("Upload torrents forever"));
        uploadTorrentsForeverButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadTorrentsForeverButton.isSelected());
            }
        });
        uploadTorrentsControlButton = new JRadioButton(I18n.tr("Upload torrents until either of the following:"));
        uploadTorrentsControlButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadTorrentsForeverButton.isSelected());
            }
        });

        uploadTorrentsForeverButton.setOpaque(false);
        uploadTorrentsControlButton.setOpaque(false);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(uploadTorrentsForeverButton);
        buttonGroup.add(uploadTorrentsControlButton);

        seedRatioModel = new SpinnerNumberModel(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                .get().doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMinValue()
                .doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMaxValue()
                .doubleValue(), .05);

        seedRatioSpinner = new JSpinner(seedRatioModel);
        seedRatioSpinner.setPreferredSize(new Dimension(50, 20));
        seedRatioSpinner.setMaximumSize(new Dimension(60, 20));

        
        int wholeDays = getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get());
        seedDaysModel = new SpinnerNumberModel(
                wholeDays,
                getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMinValue()),
                getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMaxValue()), 1);

        seedHoursModel = new SpinnerNumberModel(
                getRemainderHours(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get(), wholeDays),
                0,
                24, 1);
        
        seedDaysSpinner = new JSpinner(seedDaysModel);
        seedDaysSpinner.setPreferredSize(new Dimension(50, 20));
        seedDaysSpinner.setMaximumSize(new Dimension(60, 20));
        
        seedHoursSpinner = new JSpinner(seedHoursModel);
        seedHoursSpinner.setPreferredSize(new Dimension(50, 20));
        seedHoursSpinner.setMaximumSize(new Dimension(60, 20));
        
        JFormattedTextField seedDaysField = ((JSpinner.DefaultEditor)seedDaysSpinner.getEditor()).getTextField();
        seedDaysField.addKeyListener(new PeriodicFieldValidator(seedDaysField));        
        JFormattedTextField seedHoursField = ((JSpinner.DefaultEditor)seedHoursSpinner.getEditor()).getTextField();
        seedHoursField.addKeyListener(new PeriodicFieldValidator(seedHoursField));

        chooseTorrentsCheckBox = new JCheckBox(I18n.tr("Let me choose files to download when starting a torrent"));
        chooseTorrentsCheckBox.setOpaque(false);

        if (torrentManager.get().isValid()) {
            add(uploadTorrentsForeverButton, "span 3, wrap");
            add(uploadTorrentsControlButton, "span 3, wrap");
            add(new JLabel(I18n.tr("Ratio:")), "split 2, gapleft 20");
            add(seedRatioSpinner, "span, wrap");
            add(new JLabel(I18n.tr("Maximum days:")), "gapleft 20, split 4");
            add(seedDaysSpinner, "");
            add(new JLabel("Hours:"), "gapleft 20");
            add(seedHoursSpinner, "wrap");
            add(chooseTorrentsCheckBox, "span, gaptop 10, gapbottom 5, wrap");
        } else {
            add(new MultiLineLabel(I18n.tr("There was an error loading bittorrent. You will not be able to use bittorrent capabilities until this is resolved."),
                    500), "wrap");
        }

        add(new JButton(new OKDialogAction()), "span, tag ok, alignx right, split 2");
        add(new JButton(new CancelDialogAction()), "tag cancel");
    }

    @Override
    ApplyOptionResult applyOptions() {
        BittorrentSettings.UPLOAD_TORRENTS_FOREVER.setValue(uploadTorrentsForeverButton.isSelected());
        if (!uploadTorrentsForeverButton.isSelected()) {
            BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.setValue(seedRatioModel.getNumber().floatValue());
            BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.setValue(
                    getSeconds((Integer)seedDaysSpinner.getValue(), (Integer)seedHoursSpinner.getValue()));
        }

        BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.setValue(chooseTorrentsCheckBox
                .isSelected());

        if (torrentManager.get().isInitialized() && torrentManager.get().isValid()) {
            BackgroundExecutorService.execute(new Runnable() {
              @Override
                public void run() {
                  torrentManager.get().setTorrentManagerSettings(torrentSettings);
                }  
            });
        }
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        return BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue() != uploadTorrentsForeverButton.isSelected()
                || ((Float) seedRatioSpinner.getValue()).floatValue() != BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getValue()
                || getSeconds((Integer)seedDaysSpinner.getValue(), (Integer)seedHoursSpinner.getValue())
                    != BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue()
                || chooseTorrentsCheckBox.isSelected() != BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.getValue();
    }

    @Override
    public void initOptions() {
        boolean auto = BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue();
        if (auto) {
            uploadTorrentsForeverButton.setSelected(true);
        } 
        else {
            uploadTorrentsControlButton.setSelected(true);
        }

        seedRatioSpinner.setValue(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.get().doubleValue());
        seedDaysSpinner.setValue(getWholeDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()));
        chooseTorrentsCheckBox.setSelected(BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.getValue());
    }

    private static int getWholeDays(Integer integer) {
        return (int) Math.floor(integer.doubleValue() / (60 * 60 * 24));
    }
    
    private static int getRemainderHours(Integer totalSeconds, int days) {
        return (int)Math.round(totalSeconds.doubleValue() / (60 *60) - days*24);
    }

    private static int getSeconds(int days, int hours) {
        return days*24*60*60 + hours*60*60;
    }
    
    /**
     * Updates the state of the components based on whether the user has opted
     * to control the bittorrent settings manually, or let limewire control
     * them.
     */
    private void updateState(boolean uploadForever) {
        seedRatioSpinner.setEnabled(!uploadForever);
        seedDaysSpinner.setEnabled(!uploadForever);
        seedHoursSpinner.setEnabled(!uploadForever);
    }

}
