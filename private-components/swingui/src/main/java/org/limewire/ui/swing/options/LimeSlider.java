package org.limewire.ui.swing.options;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

/**
 * Abstract component that provides the user with a slider for picking a
 * percentage of bandwidth to use. The slider allows the implementor to select
 * between a provided min and a max value, and displays the message from the
 * implemented getMessage method to the right of the slider.
 */
public abstract class LimeSlider extends JComponent {
    private JSlider slider;

    private JLabel label;

    public LimeSlider(int minSlider, int maxSlider) {
        setOpaque(false);
        setLayout(new MigLayout("nogrid, fill"));
        label = new JLabel();
        slider = new JSlider(minSlider, maxSlider);
        slider.setOpaque(false);
        slider.setMajorTickSpacing(10);
        slider.addChangeListener(new SliderChangeListener(slider, label));
        add(slider);
        add(label);
    }

    public int getValue() {
        return slider.getValue();
    }

    public void setValue(int value) {
        slider.setValue(value);
    }

    public BoundedRangeModel getModel() {
        return slider.getModel();
    }

    /**
     * Changes the label for the slider based on the slider's current value.
     */
    private class SliderChangeListener implements ChangeListener {

        private JSlider slider;

        private JLabel label;

        public SliderChangeListener(JSlider slider, JLabel label) {
            this.slider = slider;
            this.label = label;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            int value = slider.getValue();
            label.setText(getMessage(value));
        }
    }

    /**
     * Returns a message for the slider based on the given value of the slider.
     * The value can be expected to be within the range of minSlider to
     * maxSlider that was initially passed to the LimeSlider implementor.
     */
    public abstract String getMessage(int value);
}
