package org.limewire.core.api;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Represents the various properties that a file can have in its meta-data. 
 */
public enum FilePropertyKey {
    TITLE(false),//for audio this is the track name
    AUTHOR(false),//for audio files this represents the artists name
    BITRATE(true), // ex. 128, 160, 192, 224, 320
    DESCRIPTION(false),
    COMPANY(false),//for video files this is the studio, for applications the publisher
    DATE_CREATED(true),
    FILE_SIZE(true), // in bytes
    GENRE(false),
    HEIGHT(true),
    LENGTH(true), // in seconds
    NAME(false),
    PLATFORM(false),
    QUALITY(true),
    RATING(false),
    TRACK_NUMBER(false),
    ALBUM(false),
    WIDTH(true),
    LOCATION(false),
    YEAR(true),
    TORRENT(false),
    USERAGENT(false),
    /** URI to location where item was referenced, ie. the download page for torrent websearch */
    REFERRER(false);
    
    private final static Collection<FilePropertyKey> indexableKeys = EnumSet.noneOf(FilePropertyKey.class); 
    private final static Collection<FilePropertyKey> editableKeys = EnumSet.noneOf(FilePropertyKey.class);
    private final static Collection<FilePropertyKey> filterableKeys = EnumSet.noneOf(FilePropertyKey.class);
    
    private final boolean isLongKey;
    
    private FilePropertyKey(boolean isLongKey) {
        this.isLongKey = isLongKey;
    }

    
    static {
        indexableKeys.add(ALBUM);
        indexableKeys.add(TITLE);
        indexableKeys.add(AUTHOR);
        indexableKeys.add(DESCRIPTION);
        indexableKeys.add(COMPANY);
        indexableKeys.add(NAME);
        indexableKeys.add(PLATFORM);
    };

    static {
        filterableKeys.add(ALBUM);
        filterableKeys.add(TITLE);
        filterableKeys.add(AUTHOR);
        filterableKeys.add(DESCRIPTION);
        filterableKeys.add(COMPANY);
        filterableKeys.add(GENRE);
        filterableKeys.add(NAME);
        filterableKeys.add(PLATFORM);
    };
    
    static {
        editableKeys.add(ALBUM);
        editableKeys.add(AUTHOR);
        editableKeys.add(DESCRIPTION);
        editableKeys.add(COMPANY);
        editableKeys.add(GENRE);
        editableKeys.add(PLATFORM);
        editableKeys.add(TITLE);
        editableKeys.add(TRACK_NUMBER);
        editableKeys.add(YEAR);
        editableKeys.add(RATING);
    };
   
    /**
     * Returns a Collection of the keys which are supposed to be indexed for file searching purposes. 
     */
    public static Collection<FilePropertyKey> getIndexableKeys() {
        return indexableKeys;
    }
    
    /**
     * Returns a Collection of the keys which are supposed to be filtered.
     */
    public static Collection<FilePropertyKey> getFilterableKeys() {
        return filterableKeys;
    }
    
    /**
     * Returns a Collection of keys which are supposed to be editable by the user.  
     */
    public static Collection<FilePropertyKey> getEditableKeys() {
        return editableKeys;
    }
    
    /**
     * Returns true if the key contains a Long value, false otherwise. 
     */
    public static boolean isLong(FilePropertyKey key){
        return key.isLongKey;
    }
}

