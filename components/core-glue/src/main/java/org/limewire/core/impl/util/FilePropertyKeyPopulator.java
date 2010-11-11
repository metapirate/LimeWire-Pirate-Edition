package org.limewire.core.impl.util;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.CommonUtils;
import org.limewire.util.I18NConvert;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Given a lime xml document this class will populate the given map by
 * converting limexml values to the appropriate FilePropertyKey.
 */
public class FilePropertyKeyPopulator {
    
    /** Returns the quality, based on all the supplied factors. */
    public static int calculateQuality(Category category, String extension, long fileSize, LimeXMLDocument document) {
        Long bitrate = null, length = null, height = null, width = null;
        switch(category) {
        case AUDIO:
            if(document != null) {
                bitrate = CommonUtils.parseLongNoException(document.getValue(LimeXMLNames.AUDIO_BITRATE));
                length = CommonUtils.parseLongNoException(document.getValue(LimeXMLNames.AUDIO_SECONDS));
            }
            return toAudioQualityScore(extension, fileSize, bitrate, length);
        case VIDEO:
            if(document != null) {
                bitrate = CommonUtils.parseLongNoException(document.getValue(LimeXMLNames.VIDEO_BITRATE));
                length = CommonUtils.parseLongNoException(document.getValue(LimeXMLNames.VIDEO_LENGTH));
                height = CommonUtils.parseLongNoException(document.getValue(LimeXMLNames.VIDEO_HEIGHT));
                width = CommonUtils.parseLongNoException(document.getValue(LimeXMLNames.VIDEO_WIDTH));
            }
            return toVideoQualityScore(extension, fileSize, bitrate, length, height, width);
        }
        
        return -1;
    }
    
    /** Gets an object that is the correct value for FilePropertyKey & Category. */
    public static Object get(Category category, FilePropertyKey property, LimeXMLDocument document) {
        if(document != null) {
            String limeXmlName = getLimeXmlName(category, property);
            if (limeXmlName != null) {
                Object value = document.getValue(limeXmlName);
                value = sanitizeValue(property, value);
                if(value != null) {
                    return value;
                }
            }
        }
        
        return null;
    }
    
    /** Sanitizes the value, according to the property. */
    public static Object sanitizeValue(FilePropertyKey property, Object value) {
        // Insert nothing if value is null|empty.
        if (value != null && !value.toString().isEmpty()) {
            if (value instanceof String) {
                if (FilePropertyKey.isLong(property)) {
                    return CommonUtils.parseLongNoException((String)value);
                } else {
                    return I18NConvert.instance().compose((String) value).intern();
                }
            } else {
                return value;
            }
        } else {
            return null;
        }
    }

    /*
     * TODO use a better analysis to map bit rates and file types to quality,
     * for now using the following articles as a guide for now.
     * <p>
     * http://www.extremetech.com/article2/0,2845,1560793,00.asp
     * <p>
     * http://www.cdburner.ca/digital-audio-formats-article/digital-audio-
     * comparison.htm
     * <p>
     * http://ipod.about.com/od/introductiontoitunes/a/sound_qual_test.htm
     * <p>
     * Returns 1 of 4 quality scores.
     * <p>
     * null - unscored 1 - poor 2 - good 3 - excellent
     */
    private static int toAudioQualityScore(String fileExtension, long fileSize, Long bitrate, Long length) {
        int quality = -1;
        if ("wav".equalsIgnoreCase(fileExtension) || "flac".equalsIgnoreCase(fileExtension)) {
            quality = 3;
        } else if (bitrate != null) {
            if ("mp3".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 96) {
                    quality = 1;
                } else if (bitrate < 192) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("wma".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("aac".equalsIgnoreCase(fileExtension)
                    || "m4a".equalsIgnoreCase(fileExtension)
                    || "m4b".equalsIgnoreCase(fileExtension)
                    || "m4p".equalsIgnoreCase(fileExtension)
                    || "m4v".equalsIgnoreCase(fileExtension)
                    || "mp4".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("ogg".equalsIgnoreCase(fileExtension)
                    || "ogv".equalsIgnoreCase(fileExtension)
                    || "oga".equalsIgnoreCase(fileExtension)
                    || "ogx".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 48) {
                    quality = 1;
                } else if (bitrate < 96) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            }
        } else if (length != null && length < 30) {
            quality = 1;
        } else {
            if (fileSize < (1 * 1024 * 1024)) {
                quality = 1;
            } else if (fileSize < (3 * 1024 * 1024)) {
                quality = 2;
            } else {
                quality = 3;
            }
        }
        return quality;
    }

    /*
     * TODO use a better analysis to map video attributes to quality for now
     * using the following articles as a guide for now.
     * <p>
     * Right now the scoring is somewhat arbitrary.
     * <p>
     * Returns 1 of 4 quality scores.
     * <p>
     * null - unscored 1 - poor 2 - good 3 - excellent
     */
    private static int toVideoQualityScore(String fileExtension, long fileSize, Long bitrate, Long length, Long height, Long width) {
        int quality = -1;

        if ("mpg".equalsIgnoreCase(fileExtension) && height != null && width != null) {
            if ((height * width) < (352 * 240)) {
                quality = 1;
            } else if ((height * width) < (352 * 480)) {
                quality = 2;
            } else {
                quality = 3;
            }
        } else if (length != null && length < 60) {
            quality = 1;
        } else {
            if (fileSize < (5 * 1024 * 1024)) {
                quality = 1;
            } else if (fileSize < (100 * 1024 * 1024)) {
                quality = 2;
            } else {
                quality = 3;
            }
        }
        return quality;
    }
    
    /** Returns the XML Schema URI for the given category. */
    public static String getLimeXmlSchemaUri(Category category) {
        switch (category) {
        case AUDIO:
            return LimeXMLNames.AUDIO_SCHEMA;
        case DOCUMENT:
            return LimeXMLNames.DOCUMENT_SCHEMA;
        case IMAGE:
            return LimeXMLNames.IMAGE_SCHEMA;
        case PROGRAM:
            return LimeXMLNames.APPLICATION_SCHEMA;
        case VIDEO:
            return LimeXMLNames.VIDEO_SCHEMA;
        case TORRENT:
            return LimeXMLNames.TORRENT_SCHEMA;
        }
        throw new UnsupportedOperationException("Category: " + category + " is not supported.");
    }

    /**
     * Returns the lime xml name that maps to the given category and
     * FilePropertyKey. If not mapping exists null is returned.
     */
    public static String getLimeXmlName(Category category, FilePropertyKey filePropertyKey) {
        switch(category) {
        case AUDIO:
            switch (filePropertyKey) {
            case ALBUM:
                return LimeXMLNames.AUDIO_ALBUM;
            case AUTHOR:
                return LimeXMLNames.AUDIO_ARTIST;
            case BITRATE:
                return LimeXMLNames.AUDIO_BITRATE;
            case DESCRIPTION:
                return LimeXMLNames.AUDIO_COMMENTS;
            case GENRE:
                return LimeXMLNames.AUDIO_GENRE;
            case LENGTH:
                return LimeXMLNames.AUDIO_SECONDS;
            case TRACK_NUMBER:
                return LimeXMLNames.AUDIO_TRACK;
            case YEAR:
                return LimeXMLNames.AUDIO_YEAR;
            case TITLE:
                return LimeXMLNames.AUDIO_TITLE;
            }
            break;
        case DOCUMENT:
            switch (filePropertyKey) {
            case AUTHOR:
                return LimeXMLNames.DOCUMENT_AUTHOR;
            case TITLE:
                return LimeXMLNames.DOCUMENT_TITLE;
            case DESCRIPTION:
                return LimeXMLNames.DOCUMENT_TOPIC;
            }
            break;
        case IMAGE:
            switch (filePropertyKey) {
            case AUTHOR:
                return LimeXMLNames.IMAGE_ARTIST;
            case TITLE:
                return LimeXMLNames.IMAGE_TITLE;
            case DESCRIPTION:
                return LimeXMLNames.IMAGE_DESCRIPTION;
            }
            break;
        case PROGRAM:
            switch (filePropertyKey) {
            case COMPANY:
                return LimeXMLNames.APPLICATION_PUBLISHER;
            case PLATFORM:
                return LimeXMLNames.APPLICATION_PLATFORM;
            case TITLE:
                return LimeXMLNames.APPLICATION_NAME;
            }
            break;
        case VIDEO:
            switch (filePropertyKey) {
            case AUTHOR:
                return LimeXMLNames.VIDEO_PRODUCER;
            case BITRATE:
                return LimeXMLNames.VIDEO_BITRATE;
            case DESCRIPTION:
                return LimeXMLNames.VIDEO_COMMENTS;
            case COMPANY:
                return LimeXMLNames.VIDEO_STUDIO;
            case GENRE:
                return LimeXMLNames.VIDEO_TYPE;
            case HEIGHT:
                return LimeXMLNames.VIDEO_HEIGHT;
            case WIDTH:
                return LimeXMLNames.VIDEO_WIDTH;
            case LENGTH:
                return LimeXMLNames.VIDEO_LENGTH;
            case YEAR:
                return LimeXMLNames.VIDEO_YEAR;
            case TITLE:
                return LimeXMLNames.VIDEO_TITLE;
            case RATING:
                return LimeXMLNames.VIDEO_RATING;
            }
            break;
        case TORRENT:
            // torrent doesn't have user editable properties
            return null;
        }
        return null;
    }
}
