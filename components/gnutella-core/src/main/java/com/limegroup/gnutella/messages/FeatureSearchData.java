package com.limegroup.gnutella.messages;

/**
 * A simple enum-like class that has constants related to feature searches.
 */
public final class FeatureSearchData {
    private FeatureSearchData() {}

    /**
     *  The highest currently supported feature search.
     */
    public static final int FEATURE_SEARCH_MAX_SELECTOR = 1;

    /**
     * The value for a 'what is new' search.  This will never change.
     */
    public static final int WHAT_IS_NEW = 1;

    
    /**
     * Determines if 'what is new' is supported by the given version.
     */
    public static boolean supportsWhatIsNew(int version) {
        return version >= WHAT_IS_NEW;
    }
    
    /**
     * Determines if we support the feature.
     *
     * This will also return true if the feature is not a feature (ie: 0)
     */
    public static boolean supportsFeature(int feature) {
        return feature <= FEATURE_SEARCH_MAX_SELECTOR;
    }
}