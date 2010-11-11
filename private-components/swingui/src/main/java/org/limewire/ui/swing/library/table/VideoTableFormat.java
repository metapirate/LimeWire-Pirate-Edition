package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Table format for the Video Table when it is in My Library.
 */
public class VideoTableFormat<T extends LocalFileItem> extends AbstractLibraryFormat<T> {

    static final int NAME_INDEX = 0;
    static final int TITLE_INDEX = 1;
    static final int LENGTH_INDEX = 2;
    static final int MISC_INDEX = 3;
    static final int YEAR_INDEX = 4;
    static final int SIZE_INDEX = 5;
    static final int RATING_INDEX = 6;
    static final int DIMENSION_INDEX = 7;
    static final int DESCRIPTION_INDEX = 8;
    static final int GENRE_INDEX = 9;
    static final int HIT_INDEX = 10;
    static final int UPLOADS_INDEX = 11;
    static final int UPLOAD_ATTEMPTS_INDEX = 12;
    static final int PATH_INDEX = 13;
    static final int ACTION_INDEX = 14;
    
    @Inject
    public VideoTableFormat() {
        super(ACTION_INDEX, "LIBRARY_VIDEO_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_VIDEO_NAME", I18n.tr("Name"), 658, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "LIBRARY_VIDEO_TITLE", I18n.tr("Title"), 100, false, true), 
                new ColumnStateInfo(LENGTH_INDEX, "LIBRARY_VIDEO_LENGTH", I18n.tr("Length"), 52, true, true), 
                new ColumnStateInfo(MISC_INDEX, "LIBRARY_VIDEO_MISC", I18n.tr("Misc"), 100, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "LIBRARY_VIDEO_YEAR", I18n.tr("Year"), 80, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_VIDEO_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(RATING_INDEX, "LIBRARY_VIDEO_RATING", I18n.tr("Rating"), 60, false, true), 
                new ColumnStateInfo(DIMENSION_INDEX, "LIBRARY_VIDEO_RESOLUTION", I18n.tr("Resolution"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "LIBRARY_VIDEO_DESCRIPTION", I18n.tr("Description"), 100, false, true), 
                new ColumnStateInfo(GENRE_INDEX, "LIBRARY_VIDEO_GENRE", I18n.tr("Genre"), 80, false, true),
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_VIDEO_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_VIDEO_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_VIDEO_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(PATH_INDEX, "LIBRARY_VIDEO_PATH", I18n.tr("Location"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_VIDEO_ACTION", I18n.tr(" "), 22, 22, true, false)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case TITLE_INDEX: return baseObject.getProperty(FilePropertyKey.TITLE);
        case LENGTH_INDEX: return baseObject.getProperty(FilePropertyKey.LENGTH);
        case MISC_INDEX: return "";
        case YEAR_INDEX: return baseObject.getProperty(FilePropertyKey.YEAR);
        case RATING_INDEX: return baseObject.getProperty(FilePropertyKey.RATING);
        case SIZE_INDEX: return baseObject.getSize();
        case DIMENSION_INDEX: 
            if(baseObject.getProperty(FilePropertyKey.WIDTH) == null || baseObject.getProperty(FilePropertyKey.HEIGHT) == null)
                return null;
            else
                return baseObject.getProperty(FilePropertyKey.WIDTH) + " X " + baseObject.getProperty(FilePropertyKey.HEIGHT); 
        case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        case GENRE_INDEX: return baseObject.getProperty(FilePropertyKey.GENRE);
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case PATH_INDEX: return baseObject.getProperty(FilePropertyKey.LOCATION);
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.ASCENDING, NAME_INDEX),
                    new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
        else
            return super.getDefaultSortKeys();
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}
