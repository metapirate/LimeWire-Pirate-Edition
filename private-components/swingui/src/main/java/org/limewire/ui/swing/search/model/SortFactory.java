package org.limewire.ui.swing.search.model;

import static org.limewire.util.Objects.compareToNull;
import static org.limewire.util.Objects.compareToNullIgnoreCase;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;

/**
 * Factory class for creating sort comparators.
 */
public class SortFactory {

    /**
     * Returns a search result Comparator for the specified sort option.
     */
    @SuppressWarnings("unchecked")
    public static Comparator<VisualSearchResult> getSortComparator(SortOption sortOption) {
        switch (sortOption) {
        case ALBUM:
            return getStringPropertyPlusNameComparator(FilePropertyKey.ALBUM, true);

        case ARTIST:
            return getStringPropertyPlusNameComparator(FilePropertyKey.AUTHOR, true);

        case COMPANY:
            return getStringPropertyPlusNameComparator(FilePropertyKey.COMPANY, true);

        case DATE_CREATED:
            return new SimilarResultsGroupingDelegateComparator(getDateComparator(FilePropertyKey.DATE_CREATED, false), getNameComparator(true)); 

        case FILE_EXTENSION:
        case TYPE:
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr1.getFileExtension(), vsr2.getFileExtension());
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };

        case CATEGORY:
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr1.getCategory(), vsr2.getCategory());
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };

        case LENGTH:
            return new SimilarResultsGroupingDelegateComparator(getLongComparator(FilePropertyKey.LENGTH, false), getNameComparator(true));

        case NAME:
        case TITLE:
            return new SimilarResultsGroupingDelegateComparator(getNameComparator(true));

        case PLATFORM:
            return getStringPropertyPlusNameComparator(FilePropertyKey.COMPANY, true);

        case QUALITY:
            return new SimilarResultsGroupingDelegateComparator(getLongComparator(FilePropertyKey.QUALITY, false), getNameComparator(true));

        case RELEVANCE_ITEM:
            return getRelevanceComparator();

        case SIZE_HIGH_TO_LOW:
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr2.getSize(), vsr1.getSize(), false);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };

        case SIZE_LOW_TO_HIGH:
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr1.getSize(), vsr2.getSize(), false);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };

        case YEAR:
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                private Comparator<VisualSearchResult> propertyComparator = getLongComparator(
                        FilePropertyKey.YEAR, true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = propertyComparator.compare(vsr1, vsr2);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        
        default:
            throw new IllegalArgumentException("unknown item " +  sortOption);
        }
    }
    
    /**
     * Returns a search result Comparator for date values.  The specified key
     * must reference property values stored as Long objects.
     */
    static Comparator<VisualSearchResult> getDateComparator(
            final FilePropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, true);
            }
        };
    }

    /**
     * Returns a search result Comparator for Long values.  The specified key
     * must reference property values that can be converted to Long objects.
     */
    static Comparator<VisualSearchResult> getLongComparator(
            final FilePropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long l1 = (Long)vsr1.getProperty(key);
                Long l2 = (Long)vsr2.getProperty(key);
                return compareNullCheck(l1, l2, ascending, true);
            }
        };
    }

    /**
     * Returns a search result Comparator that compares the heading field.
     */
    static Comparator<VisualSearchResult> getNameComparator(
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = vsr1.getHeading();
                String v2 = vsr2.getHeading();
                return ascending ? compareToNullIgnoreCase(v1, v2, false)
                        : compareToNullIgnoreCase(v2, v1, false);
            }
        };
    }

    /**
     * Returns a search result Comparator that compares the relevance and name
     * values.
     */
    @SuppressWarnings("unchecked")
    static Comparator<VisualSearchResult> getRelevanceComparator() {
        return new SimilarResultsGroupingDelegateComparator(
                getRelevanceComparator(false), getNameComparator(true));
    }

    /**
     * Returns a search result Comparator for relevance values with the 
     * specified sort order.
     */
    static Comparator<VisualSearchResult> getRelevanceComparator(
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                float r1 = vsr1.getRelevance();
                float r2 = vsr2.getRelevance();
                return ascending ? compareToNull(r1, r2, false) 
                        : compareToNull(r2, r1, false);
            }
        };
    }

    /**
     * Returns a search result Comparator for string values.  The specified key
     * must reference property values stored as String objects.
     */
    static Comparator<VisualSearchResult> getStringComparator(
            final FilePropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = (String) vsr1.getProperty(key);
                String v2 = (String) vsr2.getProperty(key);
                return ascending ? compareToNullIgnoreCase(v1, v2, false)
                        : compareToNullIgnoreCase(v2, v1, false);
            }
        };
    }

    /**
     * Returns a search result Comparator that compares the specified string
     * property and name values.
     */
    @SuppressWarnings("unchecked")
    static Comparator<VisualSearchResult> getStringPropertyPlusNameComparator(
            final FilePropertyKey filePropertyKey, final boolean ascending) {
        return new SimilarResultsGroupingDelegateComparator(
                getStringComparator(filePropertyKey, ascending), getNameComparator(ascending));
    }
    
    /**
     * Compare the two specified Comparable objects, and returns a negative,
     * zero, or positive value if the first object is less than, equal to, or
     * greater than the second.  If <code>ascending</code> is false, then the
     * sign of the return value is reversed.  If <code>nullsFirst</code> is
     * false, then null values are treated as larger than non-null values.
     */
    private static int compareNullCheck(Comparable c1, Comparable c2, 
            boolean ascending, boolean nullsFirst) {
        return ascending ? compareToNull(c1, c2, nullsFirst) 
                : compareToNull(c2, c1, nullsFirst);
    }
}
