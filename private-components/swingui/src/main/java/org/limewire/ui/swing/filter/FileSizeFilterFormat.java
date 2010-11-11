package org.limewire.ui.swing.filter;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for file size.
 */
class FileSizeFilterFormat<E extends FilterableItem> implements RangeFilterFormat<E> {
    /** Default size options in bytes. */
    private static final long[] DEFAULT_SIZES = {
        0,
        1024 * 10,   // 10 KB
        1024 * 50,   // 50 KB
        1024 * 100,  // 100 KB
        1024 * 500,  // 500 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 5,    // 5 MB
        1024 * 1024 * 10,   // 10 MB
        1024 * 1024 * 50,   // 50 MB
        1024 * 1024 * 100,  // 100 MB
        1024 * 1024 * 500,  // 500 MB
        1024 * 1024 * 1024, // 1 GB
        (long) 1024 * 1024 * 1024 * 5,  // 5 GB  
        (long) 1024 * 1024 * 1024 * 10, // 10 GB  
        (long) 1024 * 1024 * 1024 * 50, // 50 GB  
        (long) 1024 * 1024 * 1024 * 100 // 100 GB
    };
    
    /** Size options for audio files. */
    private static final long[] AUDIO_SIZES = {
        0,
        1024 * 100,  // 100 KB
        1024 * 250,  // 250 KB
        1024 * 500,  // 500 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 2,    // 2 MB
        1024 * 1024 * 5,    // 5 MB
        1024 * 1024 * 10,   // 10 MB
        1024 * 1024 * 25,   // 25 MB
        1024 * 1024 * 50,   // 50 MB
        1024 * 1024 * 100,  // 100 MB
        1024 * 1024 * 250,  // 250 MB
        1024 * 1024 * 500,  // 500 MB
        1024 * 1024 * 1024, // 1 GB
    };
    
    /** Size options for video files. */
    private static final long[] VIDEO_SIZES = {
        0,
        1024 * 1024 * 10,   // 10 MB
        1024 * 1024 * 25,   // 25 MB
        1024 * 1024 * 50,   // 50 MB
        1024 * 1024 * 100,  // 100 MB
        1024 * 1024 * 250,  // 250 MB
        1024 * 1024 * 500,  // 500 MB
        1024 * 1024 * 1024, // 1 GB
        (long) 1024 * 1024 * 1024 * 2,   // 2 GB  
        (long) 1024 * 1024 * 1024 * 5,   // 5 GB  
        (long) 1024 * 1024 * 1024 * 10,  // 10 GB  
        (long) 1024 * 1024 * 1024 * 25,  // 25 GB  
        (long) 1024 * 1024 * 1024 * 50,  // 50 GB  
        (long) 1024 * 1024 * 1024 * 100, // 100 GB  
    };
    
    /** Size options for image files. */
    private static final long[] IMAGE_SIZES = {
        0,
        1024 * 10,   // 10 KB
        1024 * 25,   // 25 KB
        1024 * 50,   // 50 KB
        1024 * 100,  // 100 KB
        1024 * 250,  // 250 KB
        1024 * 500,  // 500 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 2,   // 2 MB
        1024 * 1024 * 5,   // 5 MB
        1024 * 1024 * 10,  // 10 MB
        1024 * 1024 * 25,  // 25 MB
        1024 * 1024 * 50,  // 50 MB
        1024 * 1024 * 100, // 100 MB
    };
    
    /** Array of size options in bytes. */
    private long[] sizes;
    
    /**
     * Constructs a FileSizeFilterFormat with default size values.
     */
    public FileSizeFilterFormat() {
        sizes = getSizes(SearchCategory.ALL);
    }

    @Override
    public String getHeaderText() {
        return I18n.tr("Size");
    }

    @Override
    public Matcher<E> getMatcher(long minValue, long maxValue) {
        return new FileSizeMatcher(minValue, maxValue);
    }

    @Override
    public long[] getValues() {
        return sizes;
    }

    @Override
    public String getValueText(int valueIndex) {
        return GuiUtils.formatUnitFromBytes(sizes[valueIndex]);
    }
    
    @Override
    public boolean isMaximumAbsolute() {
        return false;
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return true;
    }

    @Override
    public boolean updateValues(SearchCategory filterCategory, long lowerValue, long upperValue) {
        // Get array of sizes for category.
        long[] newSizes = getSizes(filterCategory);

        // Create new value list.
        List<Long> valueList = createValueList(newSizes, lowerValue, upperValue);

        // Set array of sizes.
        sizes = new long[valueList.size()];
        for (int i = 0, len = valueList.size(); i < len; i++) {
            sizes[i] = valueList.get(i);
        }

        return true;
    }

    /**
     * Creates a list of values containing the specified value array, which
     * must be sorted in ascending order.  If <code>lowerValue</code> or
     * <code>upperValue</code> are greater than -1, then their values are
     * included in the returned list.
     */
    List<Long> createValueList(long[] values, long lowerValue, long upperValue) {
        List<Long> valueList = new ArrayList<Long>();

        // Process value array.
        for (int i = 0; i < values.length; i++) {
            // Insert lower value if necessary.
            if (lowerValue > -1) {
                if (values[i] == lowerValue) {
                    lowerValue = -1;
                } else if (values[i] > lowerValue) {
                    valueList.add(lowerValue);
                    lowerValue = -1;
                }
            }

            // Insert upper value if necessary.
            if (upperValue > -1) {
                if (values[i] == upperValue) {
                    upperValue = -1;
                } else if (values[i] > upperValue) {
                    valueList.add(upperValue);
                    upperValue = -1;
                }
            }

            // Add current value.
            valueList.add(values[i]);
        }

        // Add lower value if not found.
        if (lowerValue > -1) {
            valueList.add(lowerValue);
        }

        // Add upper value if not found.
        if (upperValue > -1) {
            valueList.add(upperValue);
        }

        return valueList;
    }

    /**
     * Returns an array of size values for the specified filter category.
     */
    private long[] getSizes(SearchCategory filterCategory) {
        switch (filterCategory) {
        case AUDIO:
            return AUDIO_SIZES;

        case VIDEO:
            return VIDEO_SIZES;

        case IMAGE:
        case DOCUMENT:
            return IMAGE_SIZES;

        default:
            return DEFAULT_SIZES;
        }
    }

    /**
     * A matcher used to filter an item by file size.
     */
    private class FileSizeMatcher implements Matcher<E> {
        private final long minSize;
        private final long maxSize;
        
        /**
         * Constructs a FileSizeMatcher for the specified file size range.
         */
        public FileSizeMatcher(long minSize, long maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        /**
         * Returns true if the specified item is within the file size range.
         */
        @Override
        public boolean matches(E item) {
            long size = item.getSize();
            boolean minValid = (minSize == sizes[0]) || (size >= minSize);
            boolean maxValid = (maxSize == sizes[sizes.length - 1]) || (size <= maxSize);
            return (minValid && maxValid); 
        }
    }
}
