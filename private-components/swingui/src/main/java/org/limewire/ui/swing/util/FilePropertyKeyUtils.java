package org.limewire.ui.swing.util;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.i18n.I18nMarker;

/**
 * Contains helper methods (rather a helper method) for displaying {@link FilePropertyKey} objects.
 */
public class FilePropertyKeyUtils {
    
    /**
     * @return Text describing a given {@link FilePropertyKey} applied to a certain {@link SearchCategory} media type.
     */
    public static String getUntraslatedDisplayName(FilePropertyKey key, SearchCategory searchCategory) {
        switch (key) {
            case TITLE: return I18nMarker.marktr("Title");
            case AUTHOR: 
                if (searchCategory == SearchCategory.AUDIO) {
                    return I18nMarker.marktr("Artist");
                }
                else {
                    return I18nMarker.marktr("Author");
                }
            case BITRATE: return I18nMarker.marktr("Bitrate");
            case DESCRIPTION: return I18nMarker.marktr("Description");
            case COMPANY: return I18nMarker.marktr("Company");
            case DATE_CREATED: return I18nMarker.marktr("Date");
            case FILE_SIZE: return I18nMarker.marktr("Size");
            case GENRE: return I18nMarker.marktr("Genre");
            case HEIGHT: return I18nMarker.marktr("Height");
            case LENGTH: return I18nMarker.marktr("Length"); 
            case NAME: return I18nMarker.marktr("Name");
            case PLATFORM: return I18nMarker.marktr("Platform");
            case QUALITY: return I18nMarker.marktr("Quality");
            case RATING: return I18nMarker.marktr("Rating");
            case TRACK_NUMBER: return I18nMarker.marktr("Track");
            case ALBUM: return I18nMarker.marktr("Album");
            case WIDTH: return I18nMarker.marktr("Width");
            case LOCATION: return I18nMarker.marktr("Location");
            case YEAR: return I18nMarker.marktr("Year");
            case TORRENT: return I18nMarker.marktr("Torrent");
            case USERAGENT: return I18nMarker.marktr("User Agent");
            case REFERRER: return I18nMarker.marktr("Referrer");
            default: throw new IllegalArgumentException("Unknown SearchCategory/FileProperyKey combination");
        }
    }
}
