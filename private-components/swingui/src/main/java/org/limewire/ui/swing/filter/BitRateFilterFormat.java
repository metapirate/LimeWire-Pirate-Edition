package org.limewire.ui.swing.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for bit rate.
 */
class BitRateFilterFormat<E extends FilterableItem> implements RangeFilterFormat<E> {
    /** Array of bit rate options. */
    private static final long[] RATES = {
        0, 
        64,
        96,
        128, 
        160,
        192, 
        256,
        320,
        512
    };

    @Override
    public String getHeaderText() {
        return I18n.tr("Bitrate");
    }

    @Override
    public Matcher<E> getMatcher(long minValue, long maxValue) {
        return new BitRateMatcher<E>(minValue);
    }

    @Override
    public String getValueText(int valueIndex) {
        return String.valueOf(RATES[valueIndex]);
    }

    @Override
    public long[] getValues() {
        return RATES;
    }
    
    @Override
    public boolean isMaximumAbsolute() {
        return false;
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
     * A matcher used to filter an item by bit rate.
     */
    private static class BitRateMatcher<E extends FilterableItem> implements Matcher<E> {
        private final long bitRate;
        
        /**
         * Constructs a BitRateMatcher for the specified bit rate.
         */
        public BitRateMatcher(long bitRate) {
            this.bitRate = bitRate;
        }

        /**
         * Returns true if the specified item matches or exceeds the bit rate.
         */
        @Override
        public boolean matches(E item) {
            if (bitRate == 0) return true;
            
            Object rate = item.getProperty(FilePropertyKey.BITRATE);
            if (rate instanceof Long) {
                return (((Long) rate).longValue() >= bitRate);
            } else {
                return false;
            }
        }
    }
}
