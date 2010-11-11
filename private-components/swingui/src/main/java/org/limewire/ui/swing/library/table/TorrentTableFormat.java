package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.settings.SearchSettings;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Table format for the Torrent Table when it is in My Library.
 */
public class TorrentTableFormat<T extends LocalFileItem> extends AbstractLibraryFormat<T> {
   
    static final int NAME_INDEX = 0;
    static final int SIZE_INDEX = 1;
    static final int HIT_INDEX = 2;
    static final int UPLOADS_INDEX = 3;
    static final int UPLOAD_ATTEMPTS_INDEX = 4;
    static final int PATH_INDEX = 5;
    static final int FILES_INDEX = 6;
    static final int TRACKERS_INDEX = 7;
    static final int ACTION_INDEX = 8;
    
    @Inject
    public TorrentTableFormat() {
        super(ACTION_INDEX, "LIBRARY_TORRENT_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_TORRENT_NAME", I18n.tr("Name"), 480, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_TORRENT_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_TORRENT_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_TORRENT_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_TORRENT_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(PATH_INDEX, "LIBRARY_TORRENT_PATH", I18n.tr("Location"), 200, false, true),
                new ColumnStateInfo(FILES_INDEX, "LIBRARY_TORRENT_FILES", I18n.tr("Files"), 400, 480, SearchSettings.USE_TORRENT_SCRAPER.getValue(), true),
                new ColumnStateInfo(TRACKERS_INDEX, "LIBRARY_TORRENT_TRACKERS", I18n.tr("Trackers"), 400, 480, SearchSettings.USE_TORRENT_SCRAPER.getValue(), true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_TORRENT_ACTION", I18n.tr(" "), 22, 22, SearchSettings.USE_TORRENT_SCRAPER.getValue(), false)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        
        Torrent torrent = null;
        
        switch(column) {
        case NAME_INDEX: return baseObject;
        case SIZE_INDEX: return baseObject.getSize();
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case PATH_INDEX: return baseObject.getProperty(FilePropertyKey.LOCATION);
        case ACTION_INDEX: return baseObject;
        case FILES_INDEX:
            torrent = (Torrent) baseObject.getProperty(FilePropertyKey.TORRENT);
            return torrent != null ? torrent.getTorrentFileEntries() : null;
        case TRACKERS_INDEX:
            torrent = (Torrent) baseObject.getProperty(FilePropertyKey.TORRENT);
            return torrent != null ? torrent.getTrackers() : null;
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
