package org.limewire.ui.swing.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for quality.
 */
class QualityFilterFormat<E extends FilterableItem> implements RangeFilterFormat<E> {
    /** Array of quality options. */
    private static final long[] QUALITIES = {
        0, // spam
        1, // poor
        2, // good
        3  // excellent
    };

    @Override
    public String getHeaderText() {
        return I18n.tr("Quality");
    }

    @Override
    public Matcher<E> getMatcher(long minValue, long maxValue) {
        return new QualityMatcher<E>(minValue);
    }

    @Override
    public String getValueText(int valueIndex) {
        return GuiUtils.toQualityStringShort(QUALITIES[valueIndex]);
    }

    @Override
    public long[] getValues() {
        return QUALITIES;
    }
    
    @Override
    public boolean isMaximumAbsolute() {
        return true;
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return false;
    }
    
    @Override
    public boolean updateValues(SearchCategory filterCategory, long lowerValue, long upperValue) {
        return false;
    }
    
    /**
     * A matcher used to filter an item by quality.
     */
    private static class QualityMatcher<E extends FilterableItem> implements Matcher<E> {
        private final long quality;
        
        /**
         * Constructs a QualityMatcher for the specified quality.
         */
        public QualityMatcher(long quality) {
            this.quality = quality;
        }

        /**
         * Returns true if the specified item matches or exceeds the quality.
         */
        @Override
        public boolean matches(E item) {
            if (quality == 0) return true;
            if (item.isSpam()) return false;
            
            Object value = item.getProperty(FilePropertyKey.QUALITY);
            if (value instanceof Long) {
                return (((Long) value).longValue() >= quality);
            } else {
                return false;
            }
        }
    }
}
