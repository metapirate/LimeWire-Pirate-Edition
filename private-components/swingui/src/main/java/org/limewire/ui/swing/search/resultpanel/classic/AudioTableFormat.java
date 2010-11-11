package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.table.TrackComparator;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 */
public class AudioTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int TITLE_INDEX = 1;
    static final int ARTIST_INDEX = 2;
    static final int ALBUM_INDEX = 3;
    public static final int LENGTH_INDEX = 4;
    public static final int QUALITY_INDEX = 5;
    static final int BITRATE_INDEX = 6;
    static final int GENRE_INDEX = 7;
    static final int TRACK_INDEX = 8;
    static final int YEAR_INDEX = 9;
    static final int NAME_INDEX = 10;
    static final int EXTENSION_INDEX = 11;
    public static final int SIZE_INDEX = 12;
    static final int DESCRIPTION_INDEX = 13;
    static final int IS_SPAM_INDEX = 14;
    
    public AudioTableFormat() {
        super("CLASSIC_SEARCH_AUDIO_TABLE", TITLE_INDEX, FROM_INDEX, IS_SPAM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_AUDIO_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "CLASSIC_SEARCH_AUDIO_TITLE", I18n.tr("Name"), 255, true, true),     
                new ColumnStateInfo(ARTIST_INDEX, "CLASSIC_SEARCH_AUDIO_ARTIST", I18n.tr("Artist"), 174, true, true), 
                new ColumnStateInfo(ALBUM_INDEX, "CLASSIC_SEARCH_AUDIO_ALBUM", I18n.tr("Album"), 157, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "CLASSIC_SEARCH_AUDIO_LENGTH", I18n.tr("Length"), 64, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "CLASSIC_SEARCH_AUDIO_QUALITY", I18n.tr("Quality"), 105, true, true), 
                new ColumnStateInfo(BITRATE_INDEX, "CLASSIC_SEARCH_AUDIO_BITRATE", I18n.tr("Bitrate"), 55, false, true), 
                new ColumnStateInfo(GENRE_INDEX, "CLASSIC_SEARCH_AUDIO_GENRE", I18n.tr("Genre"), 80, false, true),
                new ColumnStateInfo(TRACK_INDEX, "CLASSIC_SEARCH_AUDIO_TRACK", I18n.tr("Track"), 60, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "CLASSIC_SEARCH_AUDIO_YEAR", I18n.tr("Year"), 60, false, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_AUDIO_NAME", I18n.tr("Filename"), 550, false, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_AUDIO_EXTENSION", I18n.tr("Extension"), 60, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_AUDIO_SIZE", I18n.tr("Size"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_AUDIO_DESCRIPTION", I18n.tr("Description"), 60, false, false),
                new ColumnStateInfo(IS_SPAM_INDEX, "CLASSIC_SEARCH_AUDIO_IS_SPAM", "", 10, false, false)
        });
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case TITLE_INDEX: return Component.class;
        case BITRATE_INDEX: return Integer.class;
        case TRACK_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
        case TRACK_INDEX:
            return getTrackComparator();
        case QUALITY_INDEX:
            return getQualityComparator();
        default:
            return super.getColumnComparator(column);
        }
    }    
    
    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        switch(column) {
            case FROM_INDEX: return vsr;
            case TITLE_INDEX: return vsr;
            case ARTIST_INDEX: return vsr.getProperty(FilePropertyKey.AUTHOR);
            case ALBUM_INDEX: return vsr.getProperty(FilePropertyKey.ALBUM);
            case LENGTH_INDEX: return vsr.getProperty(FilePropertyKey.LENGTH);
            case QUALITY_INDEX: return vsr;
            case BITRATE_INDEX: return vsr.getProperty(FilePropertyKey.BITRATE);
            case GENRE_INDEX: return vsr.getProperty(FilePropertyKey.GENRE);
            case TRACK_INDEX: return vsr.getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR_INDEX: return vsr.getProperty(FilePropertyKey.YEAR);
            case NAME_INDEX: return vsr.getProperty(FilePropertyKey.NAME);
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case SIZE_INDEX: return vsr.getSize();
            case DESCRIPTION_INDEX: return "";
            case IS_SPAM_INDEX: return vsr;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.DESCENDING, FROM_INDEX),
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
     * Returns a comparator for the track column.
     */
    public Comparator getTrackComparator() {
        return new TrackComparator();
    }
}