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
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * video descriptions.
 */
public class VideoTableFormat extends ResultsTableFormat<VisualSearchResult> {
    // Indices into ColumnStateInfo array. 
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int TITLE_INDEX = 2;
    static final int EXTENSION_INDEX = 3;
    public static final int LENGTH_INDEX = 4;
    public static final int QUALITY_INDEX = 5;
    public static final int SIZE_INDEX = 6;
    static final int MISC_INDEX = 7;
    static final int RATING_INDEX = 8;
    static final int DIMENSION_INDEX = 9;
    static final int YEAR_INDEX = 10;
    static final int DESCRIPTION_INDEX = 11;
    static final int GENRE_INDEX = 12;
    static final int IS_SPAM_INDEX = 13;
    
    public VideoTableFormat() {
        super("CLASSIC_SEARCH_VIDEO_TABLE", NAME_INDEX, FROM_INDEX, IS_SPAM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_VIDEO_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_VIDEO_NAME", I18n.tr("Name"), 434, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "CLASSIC_SEARCH_VIDEO_TITLE", I18n.tr("Title"), 100, true, false),
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_VIDEO_EXTENSION", I18n.tr("Extension"), 85, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "CLASSIC_SEARCH_VIDEO_LENGTH", I18n.tr("Length"), 85, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "CLASSIC_SEARCH_VIDEO_QUALITY", I18n.tr("Quality"), 85, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_VIDEO_SIZE", I18n.tr("Size"), 81, true, true),
                new ColumnStateInfo(MISC_INDEX, "CLASSIC_SEARCH_VIDEO_MISC", I18n.tr("Misc"), 60, false, true),
                new ColumnStateInfo(RATING_INDEX, "CLASSIC_SEARCH_VIDEO_RATING", I18n.tr("Rating"), 60, false, true), 
                new ColumnStateInfo(DIMENSION_INDEX, "CLASSIC_SEARCH_VIDEO_RESOLUTION", I18n.tr("Resolution"), 60, false, true),
                new ColumnStateInfo(YEAR_INDEX, "CLASSIC_SEARCH_VIDEO_YEAR", I18n.tr("Year"), 60, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_VIDEO_DESCRIPTION", I18n.tr("Description"), 60, false, true),
                new ColumnStateInfo(GENRE_INDEX, "CLASSIC_SEARCH_VIDEO_GENRE", I18n.tr("Genre"), 80, false, true),
                new ColumnStateInfo(IS_SPAM_INDEX, "CLASSIC_SEARCH_VIDEO_IS_SPAM", "", 10, false, false)
        });
    }

    @Override
    public Class getColumnClass(int index) {
        switch(index) {
        case NAME_INDEX: return Component.class;
        case RATING_INDEX: return Integer.class;
        case YEAR_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(index);
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
        case QUALITY_INDEX:
            return getQualityComparator();
        default:
            return super.getColumnComparator(column);
        }
    }    

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        switch(index) {
            case NAME_INDEX: return vsr;
            case TITLE_INDEX: return vsr.getProperty(FilePropertyKey.TITLE);
            case EXTENSION_INDEX: return  vsr.getFileExtension();
            case LENGTH_INDEX: return vsr.getProperty(FilePropertyKey.LENGTH);
            case YEAR_INDEX: return vsr.getProperty(FilePropertyKey.YEAR);
            case QUALITY_INDEX: return vsr;
            case MISC_INDEX: return vsr.getProperty(FilePropertyKey.DESCRIPTION);
            case DESCRIPTION_INDEX: return "";
            case FROM_INDEX: return vsr;
            case RATING_INDEX: return vsr.getProperty(FilePropertyKey.RATING);
            case DIMENSION_INDEX:
                if(vsr.getProperty(FilePropertyKey.WIDTH) == null || vsr.getProperty(FilePropertyKey.HEIGHT) == null)
                    return null;
                else
                    return (vsr.getProperty(FilePropertyKey.WIDTH) + " X " + vsr.getProperty(FilePropertyKey.HEIGHT));
            case SIZE_INDEX: return vsr.getSize();
            case GENRE_INDEX: return vsr.getProperty(FilePropertyKey.GENRE);
            case IS_SPAM_INDEX: return vsr;
        }
        throw new IllegalArgumentException("Unknown column:" + index);
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.DESCENDING, FROM_INDEX),
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
