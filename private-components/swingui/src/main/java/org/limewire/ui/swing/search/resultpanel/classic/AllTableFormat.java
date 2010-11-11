package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.Category;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int EXTENSION_INDEX = 2;
    static final int TYPE_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    static final int IS_SPAM_INDEX = 5;
    
    private final Provider<IconManager> iconManager;
    
    @Inject
    public AllTableFormat(Provider<IconManager> iconManager) {
        super("CLASSIC_SEARCH_ALL_TABLE", NAME_INDEX, FROM_INDEX, IS_SPAM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_ALL_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_ALL_NAME", I18n.tr("Name"), 467, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_ALL_EXTENSION", I18n.tr("Extension"), 95, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "CLASSIC_SEARCH_ALL_TYPE", I18n.tr("Type"), 110, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_ALL_SIZE", I18n.tr("Size"), 83, true, true),
                new ColumnStateInfo(IS_SPAM_INDEX, "CLASSIC_SEARCH_ALL_IS_SPAM", "", 10, false, false)
        });
        
        this.iconManager = iconManager;
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case NAME_INDEX: return Component.class;
        case SIZE_INDEX: return Long.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        switch(column) {
            case FROM_INDEX: return vsr;
            case NAME_INDEX: return vsr;
            case TYPE_INDEX: 
                if( vsr.getCategory() == Category.DOCUMENT || vsr.getCategory() == Category.PROGRAM || vsr.getCategory() == Category.OTHER) {
                    String mime = iconManager.get().getMIMEDescription(vsr.getFileExtension());
                    if(mime != null)
                        return I18n.tr(vsr.getCategory().getSingularName()) + " (" + mime + ")";
                    else
                        return I18n.tr(vsr.getCategory().getSingularName());
                } else
                    return I18n.tr(vsr.getCategory().getSingularName());
            case SIZE_INDEX: return vsr.getSize();
            case EXTENSION_INDEX: return vsr.getFileExtension();
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
                    new SortKey(SortOrder.ASCENDING, NAME_INDEX),
                    new SortKey(SortOrder.ASCENDING, TYPE_INDEX),
                    new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
        else
            return super.getDefaultSortKeys();
    }
    
    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(TYPE_INDEX, SIZE_INDEX);
        case TYPE_INDEX:
            return Arrays.asList(NAME_INDEX, SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX, TYPE_INDEX);
        default:
            return Collections.emptyList();
        }
    }
 
    /**
     * Overrides superclass method to return a Name comparator that utilizes
     * the Artist and Title for audio files.
     */
    @Override
    public Comparator getNameComparator() {
        return new NameComparator(true);
    }
}