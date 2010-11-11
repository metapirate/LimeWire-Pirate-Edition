package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchCategory;

/**
 * Enumeration of all sort options for all search categories.
 */
public enum SortOption {
    COMPANY,
    PLATFORM,
    TYPE,
    DATE_CREATED,
    QUALITY,
    YEAR,
    FILE_EXTENSION,
    TITLE,
    LENGTH,
    ALBUM,
    ARTIST,
    SIZE_LOW_TO_HIGH,
    SIZE_HIGH_TO_LOW,
    CATEGORY,
    NAME,
    RELEVANCE_ITEM;

    /**
     * Returns the default sort option.
     */
    public static SortOption getDefault() {
        return RELEVANCE_ITEM;
    }
    
    /**
     * Returns an array of valid sort options for the specified search category.
     */
    public static SortOption[] getSortOptions(SearchCategory category) {
        switch (category) {
        case ALL:
            return new SortOption[] {
                RELEVANCE_ITEM, NAME, CATEGORY,
                SIZE_HIGH_TO_LOW, SIZE_LOW_TO_HIGH
            };
        case AUDIO:
            return new SortOption[] {
                RELEVANCE_ITEM, NAME, ARTIST, ALBUM, LENGTH,
                QUALITY
            };
        case VIDEO:
            return new SortOption[] {
                RELEVANCE_ITEM, TITLE, FILE_EXTENSION, LENGTH,
                YEAR, QUALITY
            };
        case IMAGE:
            return new SortOption[] {
                RELEVANCE_ITEM, NAME, FILE_EXTENSION,
                DATE_CREATED
            };
        case DOCUMENT:
            return new SortOption[] {
                RELEVANCE_ITEM, NAME, TITLE, TYPE,
                SIZE_LOW_TO_HIGH, DATE_CREATED
            };
        case PROGRAM:
            return new SortOption[] {
                RELEVANCE_ITEM, NAME, SIZE_LOW_TO_HIGH,
                PLATFORM, COMPANY
            };
        default:
            return new SortOption[] {
                RELEVANCE_ITEM, NAME, TYPE,
                SIZE_HIGH_TO_LOW, SIZE_LOW_TO_HIGH
            };
        }
    }
}
