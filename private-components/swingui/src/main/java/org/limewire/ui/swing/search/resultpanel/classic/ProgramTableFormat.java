package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
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
 * document descriptions.
 */
public class ProgramTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    public static final int SIZE_INDEX = 2;
    static final int EXTENSION_INDEX = 3;
    static final int PLATFORM_INDEX = 4;
    static final int COMPANY_INDEX = 5;
    static final int DESCRIPTION_INDEX = 6;
    static final int IS_SPAM_INDEX = 7;
    
    public ProgramTableFormat() {
        super("CLASSIC_SEARCH_PROGRAM_TABLE", NAME_INDEX, FROM_INDEX, IS_SPAM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_PROGRAM_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_PROGRAM_NAME", I18n.tr("Name"), 489, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_PROGRAM_SIZE", I18n.tr("Size"), 93, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_PROGRAM_EXTENSION", I18n.tr("Extension"), 70, true, true), 
                new ColumnStateInfo(PLATFORM_INDEX, "CLASSIC_SEARCH_PROGRAM_PLATFORM", I18n.tr("Platform"), 120, false, true),
                new ColumnStateInfo(COMPANY_INDEX, "CLASSIC_SEARCH_PROGRAM_COMPANY", I18n.tr("Company"), 118, true, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_PROGRAM_DESCRIPTION", I18n.tr("Description"), 80, false, true),
                new ColumnStateInfo(IS_SPAM_INDEX, "CLASSIC_SEARCH_PROGRAM_IS_SPAM", "", 10, false, false)
        });
    }

    @Override
    public Class getColumnClass(int index) {
        switch(index) {
        case NAME_INDEX: return Component.class;
        case SIZE_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return  super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        switch(index) {
            case NAME_INDEX: return vsr;
            case SIZE_INDEX: return vsr.getSize();
            case PLATFORM_INDEX: return vsr.getProperty(FilePropertyKey.PLATFORM);
            case COMPANY_INDEX: return vsr.getProperty(FilePropertyKey.COMPANY);
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case FROM_INDEX: return vsr;
            case DESCRIPTION_INDEX: return "";
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
