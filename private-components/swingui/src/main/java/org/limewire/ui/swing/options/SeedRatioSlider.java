package org.limewire.ui.swing.options;

import java.text.NumberFormat;

import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Basic component that provides the user with a slider for picking a percentage
 * of bandwidth to use. The slider lets the user select from 25-100% of their
 * bandwidth for use. It displays the expected bandwidth as a label after the
 * slider.
 */
public class SeedRatioSlider extends LimeSlider {

    public static final int MIN_SLIDER = BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
            .getMinValue().intValue();

    public static final int MAX_SLIDER = BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
            .getMaxValue().intValue();

    public SeedRatioSlider() {
        super(MIN_SLIDER * 10, MAX_SLIDER * 10);
    }

    @Override
    public String getMessage(int value) {
        if (value >= getModel().getMaximum()) {
            return I18n.tr("Unlimited");
        } else {
            Float f = new Float(value / (float) 10);
            NumberFormat formatter = NumberFormat.getInstance();
            formatter.setMaximumFractionDigits(2);
            String labelText = String.valueOf(formatter.format(f));
            return labelText;
        }
    }

    public float getSeedRatio() {
        int value = getValue();
        if (value == getModel().getMaximum()) {
            return BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMaxValue();
        }
        return value / (float) 10;
    }

    public void setSeedRatio(float value) {
       setValue((int) (value * 10));
    }
}
