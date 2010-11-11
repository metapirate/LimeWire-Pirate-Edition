package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Arrays;
import java.util.Calendar;
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
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 */
public class DocumentTableFormat extends ResultsTableFormat<VisualSearchResult> {
    // Indices into ColumnStateInfo array.
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int TYPE_INDEX = 2;
    static final int EXTENSION_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    static final int DATE_INDEX = 5;
    static final int AUTHOR_INDEX = 6;
    static final int DESCRIPTION_INDEX = 7;
    static final int IS_SPAM_INDEX = 8;
    
    /** Icon manager used to find native file type information. */
    private final Provider<IconManager> iconManager;
    
    @Inject
    public DocumentTableFormat(Provider<IconManager> iconManager) {
        super("CLASSIC_SEARCH_DOCUMENT_TABLE", NAME_INDEX, FROM_INDEX, IS_SPAM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_DOCUMENT_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_DOCUMENT_NAME", I18n.tr("Name"), 493, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "CLASSIC_SEARCH_DOCUMENT_TYPE", I18n.tr("Type"), 180, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_DOCUMENT_EXTENSION", I18n.tr("Extension"), 83, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_DOCUMENT_SIZE", I18n.tr("Size"), 92, true, true), 
                new ColumnStateInfo(DATE_INDEX, "CLASSIC_SEARCH_DOCUMENT_DATE", I18n.tr("Date Created"), 104, false, true), 
                new ColumnStateInfo(AUTHOR_INDEX, "CLASSIC_SEARCH_DOCUMENT_AUTHOR", I18n.tr("Author"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_DOCUMENT_DESCRIPTION", I18n.tr("Description"), 80, false, true),
                new ColumnStateInfo(IS_SPAM_INDEX, "CLASSIC_SEARCH_DOCUMENT_IS_SPAM", "", 10, false, false)
        });
        
        this.iconManager = iconManager;
    }
 
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case NAME_INDEX: return Component.class;
        case DATE_INDEX: return Calendar.class;
        case SIZE_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        switch(column) {
            case NAME_INDEX: return vsr;
            case TYPE_INDEX: 
                // Use icon manager to return MIME description.
                return (iconManager != null) ?
                    iconManager.get().getMIMEDescription(vsr.getFileExtension()) : 
                    vsr.getFileExtension();
            case SIZE_INDEX: return vsr.getSize();
            case DATE_INDEX: return vsr.getProperty(FilePropertyKey.DATE_CREATED);
            case FROM_INDEX: return vsr;
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case AUTHOR_INDEX: return vsr.getProperty(FilePropertyKey.AUTHOR);
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
}