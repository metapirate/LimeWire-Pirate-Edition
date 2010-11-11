package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.table.TrackComparator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertyUtils;

import com.google.inject.Inject;

/**
 * Table format for the Audio Table when it is in My Library.
 */
public class AudioTableFormat<T extends LocalFileItem> extends AbstractLibraryFormat<T> {

    static final int PLAY_INDEX = 0;
    static final int TITLE_INDEX = 1;
    static final int ARTIST_INDEX = 2;
    static final int ALBUM_INDEX = 3;
    static final int LENGTH_INDEX = 4;
    static final int GENRE_INDEX = 5;
    static final int BITRATE_INDEX = 6;
    static final int SIZE_INDEX = 7;
    static final int FILENAME_INDEX = 8;
    static final int TRACK_INDEX = 9;
    static final int YEAR_INDEX = 10;
    static final int DESCRIPTION_INDEX = 11;
    static final int HIT_INDEX = 12;
    static final int UPLOADS_INDEX = 13;
    static final int UPLOAD_ATTEMPTS_INDEX = 14;
    static final int PATH_INDEX = 15;
    static final int ACTION_INDEX = 16;
    
    @Inject
    public AudioTableFormat() {
        super(ACTION_INDEX, "LIBRARY_AUDIO_TABLE", ARTIST_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(PLAY_INDEX, "LIBRARY_AUDIO_PLAY", "", 16, 16, true, false), 
                new ColumnStateInfo(TITLE_INDEX, "LIBRARY_AUDIO_TITLE", I18n.tr("Name"), 274, true, true),     
                new ColumnStateInfo(ARTIST_INDEX, "LIBRARY_AUDIO_ARTIST", I18n.tr("Artist"), 182, true, true), 
                new ColumnStateInfo(ALBUM_INDEX, "LIBRARY_AUDIO_ALBUM", I18n.tr("Album"), 163, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "LIBRARY_AUDIO_LENGTH", I18n.tr("Length"), 59, true, true), 
                new ColumnStateInfo(GENRE_INDEX, "LIBRARY_AUDIO_GENRE", I18n.tr("Genre"), 60, false, true), 
                new ColumnStateInfo(BITRATE_INDEX, "LIBRARY_AUDIO_BITRATE", I18n.tr("Bitrate"), 50, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_AUDIO_SIZE", I18n.tr("Size"), 50, false, true),
                new ColumnStateInfo(FILENAME_INDEX, "LIBRARY_AUDIO_FILENAME", I18n.tr("Filename"), 100, false, true), 
                new ColumnStateInfo(TRACK_INDEX, "LIBRARY_AUDIO_TRACK", I18n.tr("Track"), 50, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "LIBRARY_AUDIO_YEAR", I18n.tr("Year"), 50, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "LIBRARY_AUDIO_DESCRIPTION_UNUSED", I18n.tr("Description"), 100, false, false), 
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_AUDIO_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_AUDIO_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_AUDIO_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(PATH_INDEX, "LIBRARY_AUDIO_PATH", I18n.tr("Location"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_AUDIO_ACTION", I18n.tr(" "), 22, 22, true, false)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case PLAY_INDEX: return baseObject;
        case TITLE_INDEX: return baseObject;
        case ARTIST_INDEX: return baseObject.getProperty(FilePropertyKey.AUTHOR);
        case ALBUM_INDEX: return baseObject.getProperty(FilePropertyKey.ALBUM);
        case LENGTH_INDEX: return baseObject.getProperty(FilePropertyKey.LENGTH);
        case GENRE_INDEX: return baseObject.getProperty(FilePropertyKey.GENRE);
        case BITRATE_INDEX: return baseObject.getProperty(FilePropertyKey.BITRATE);
        case FILENAME_INDEX: return baseObject.getFileName();
        case SIZE_INDEX: return baseObject.getSize();
        case TRACK_INDEX: return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
        case YEAR_INDEX: return baseObject.getProperty(FilePropertyKey.YEAR);
        case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case PATH_INDEX: return baseObject.getProperty(FilePropertyKey.LOCATION);
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Class getColumnClass(int column) {
        switch(column) {
            case PLAY_INDEX:
            case TITLE_INDEX:
                return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        switch(column) {
            case TITLE_INDEX: return new NameComparator();
            case TRACK_INDEX: return new TrackComparator();
        }
        return super.getColumnComparator(column);
    }

    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.ASCENDING, ARTIST_INDEX),
                    new SortKey(SortOrder.ASCENDING, ALBUM_INDEX),
                    new SortKey(SortOrder.ASCENDING, TRACK_INDEX),
                    new SortKey(SortOrder.ASCENDING, TITLE_INDEX));
        else
            return super.getDefaultSortKeys();
    }
    
    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case ARTIST_INDEX:
            return Arrays.asList(ALBUM_INDEX, TRACK_INDEX, TITLE_INDEX);
        case ALBUM_INDEX:
            return Arrays.asList(TRACK_INDEX, TITLE_INDEX);
        default:
            return Collections.emptyList();
        }
    }
    
    /**
     * Compares the title field in the NAME_COLUMN.
     */
    private class NameComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {
            String title1 = PropertyUtils.getTitle(o1);
            String title2 = PropertyUtils.getTitle(o2);
            
            return title1.toLowerCase(Locale.US).compareTo(title2.toLowerCase(Locale.US));
        }
    }
}
