package org.limewire.ui.swing.filter;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.RangeSlider;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter to select items according to a range of values.
 */
class RangeFilter<E extends FilterableItem> extends AbstractFilter<E> {

    private final JPanel panel = new JPanel();
    private final JLabel headerLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final RangeSlider slider = new RangeSlider();
    
    private final RangeFilterFormat<E> rangeFormat;
    
    private boolean resetAdjusting;
    
    /**
     * Constructs a RangeFilter with the specified range format.
     */
    public RangeFilter(RangeFilterFormat<E> rangeFormat) {
        this.rangeFormat = rangeFormat;
        
        FilterResources resources = getResources();
        
        panel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left,grow]",
                "[top][top]3[top]"));
        panel.setOpaque(false);

        headerLabel.setFont(resources.getHeaderFont());
        headerLabel.setForeground(resources.getHeaderColor());
        headerLabel.setText(rangeFormat.getHeaderText());
        
        valueLabel.setFont(resources.getRowFont());
        valueLabel.setForeground(resources.getRowColor());
        
        slider.setMinimum(0);
        slider.setMaximum(rangeFormat.getValues().length - 1);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(resources.getFilterWidth(), slider.getPreferredSize().height));
        slider.setFocusable(false);
        slider.setRequestFocusEnabled(false);
        slider.setUpperThumbEnabled(rangeFormat.isUpperLimitEnabled());
        
        // Set initial values.
        resetSliderRange();

        // Add listener to update filter.
        slider.addChangeListener(new SliderListener());
        
        panel.add(headerLabel, "gap 6 6, wrap");
        panel.add(valueLabel , "gap 6 6, wrap");
        panel.add(slider     , "gap 6 6, growx");
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Reset slider values.  We temporarily set resetAdjusting to true so 
        // the slider listener can avoid handling the events.
        resetAdjusting = true;
        resetSliderRange();
        resetAdjusting = false;
        
        // Deactivate filter.
        deactivate();
    }

    @Override
    public void dispose() {
        // Do nothing.
    }
    
    /**
     * Returns a text description of the filter state.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        buf.append(getClass().getSimpleName()).append("[");
        buf.append("header=").append(rangeFormat.getHeaderText());
        buf.append(", active=").append(isActive());
        buf.append(", selectedRange=").append(createRangeText());
        buf.append("]");
        
        return buf.toString();
    }
    
    /**
     * Updates the range values based on the specified filter category.
     */
    public void updateRange(SearchCategory filterCategory) {
        // Get current values.
        int oldLower = slider.getValue();
        int oldUpper = slider.getUpperValue();
        long[] oldValues = rangeFormat.getValues();
        
        // Determine lower and upper values to be included.
        long lowerValue = (oldLower > 0) ? oldValues[oldLower] : -1;
        long upperValue = (oldUpper < oldValues.length - 1) ? oldValues[oldUpper] : -1;
        
        // Update range values for category.  By definition, the new range
        // must include the specified lower and upper values.
        if (rangeFormat.updateValues(filterCategory, lowerValue, upperValue)) {
            // Get new value array.
            long[] newValues = rangeFormat.getValues();
            
            // Find lower and upper slider inputs.
            int newLower = 0;
            int newUpper = newValues.length - 1;
            if ((lowerValue > -1) || (upperValue > -1)) {
                for (int i = 0; i < newValues.length; i++) {
                    if (newValues[i] == lowerValue) {
                        newLower = i;
                    }
                    if (newValues[i] == upperValue) {
                        newUpper = i;
                    }
                }
            }
            
            // Disable listener and re-initialize slider values.
            resetAdjusting = true;
            try {
                slider.setMaximum(newValues.length - 1);
                slider.setValue(newLower);
                slider.setUpperValue(newUpper);
                slider.repaint();
                
            } finally {
                resetAdjusting = false;
            }
        }
    }
    
    /**
     * Creates a text string describing the current selected range.
     */
    protected String createRangeText() {
        if (rangeFormat.isUpperLimitEnabled()) {
            // Get text for lower and upper values.
            String minText = rangeFormat.getValueText(slider.getValue());
            String maxText = rangeFormat.getValueText(slider.getUpperValue());
            
            if (slider.getUpperValue() == slider.getMaximum()) {
                if (rangeFormat.isMaximumAbsolute()) {
                    return minText;
                }
                // {0}: lower bound of open-ended numeric range
                return I18n.tr("{0} or above", minText);
            } else {
                // {0}: lower bound of numeric range, {1}: upper bound of numeric range
                return I18n.tr("{0} to {1}", minText, maxText);
            }

        } else {
            // Get text for lower value only.
            String minText = rangeFormat.getValueText(slider.getValue());
            
            if (slider.getValue() == slider.getMinimum()) {
                return I18n.tr("all");
            } else if ((slider.getValue() == slider.getMaximum()) && rangeFormat.isMaximumAbsolute()) {
                return minText;
            } else {
                return I18n.tr("{0} or above", minText);
            }
        }
    }
    
    /**
     * Resets the range values to the minimum and maximum. 
     */
    private void resetSliderRange() {
        // Set lower value to minimum.
        slider.setValue(slider.getMinimum());
        
        // Set upper value to maximum if enabled.
        if (rangeFormat.isUpperLimitEnabled()) {
            slider.setUpperValue(slider.getMaximum());
        }
        
        // Set range text.
        valueLabel.setText(createRangeText());
    }

    /**
     * Listener to handle slider change events.
     */
    private class SliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            // Skip event if filter is being reset.
            if (resetAdjusting) {
                return;
            }
            
            // Update range display.
            RangeSlider slider = (RangeSlider) e.getSource();
            String rangeText = createRangeText();
            valueLabel.setText(rangeText);
            
            // When slider movement has ended, apply filter for selected range.
            if (!slider.getValueIsAdjusting()) {
                long[] values = rangeFormat.getValues();
                
                // Determine if filter is activated.  This is true when the 
                // slider values are set to anything other than the min/max.
                boolean activate = false;
                if (rangeFormat.isUpperLimitEnabled()) {
                    activate = (slider.getValue() > 0) || (slider.getUpperValue() < values.length - 1);
                } else {
                    activate = (slider.getValue() > 0);
                }
                
                if (activate) {
                    // Get selected range.
                    long minValue = values[slider.getValue()];
                    long maxValue = values[slider.getUpperValue()];

                    // Create new matcher and activate.
                    Matcher<E> newMatcher = rangeFormat.getMatcher(minValue, maxValue);
                    activate(rangeText, newMatcher);
                    
                } else {
                    // Deactivate to clear matcher.
                    deactivate();
                }
                
                // Notify filter listeners.
                fireFilterChanged(RangeFilter.this);
            }
        }
    }
}
