package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.EventListTableSortFormat;

/**
 * Abstract table format for columns for local file items
 */
public abstract class AbstractLibraryFormat<T extends FileItem> extends AbstractColumnStateFormat<T> implements EventListTableSortFormat {
    
    private final String sortID;
    private final int sortedColumn;
    private final boolean isAscending;
    private final int actionIndex;
    
    public AbstractLibraryFormat(int actionIndex, String sortID, int sortedColumn, boolean isAscending, ColumnStateInfo... columnInfo) {
        super(columnInfo);
        this.sortID = sortID;
        this.sortedColumn = sortedColumn;
        this.isAscending = isAscending;
        this.actionIndex = actionIndex;
    }
    
    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }
    
    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
    
    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == actionIndex;
    }
    
    public int getActionColumn() {
        return actionIndex;
    }
        
    @Override
    public Class getColumnClass(int column) {
        return String.class;
    }
    
    @Override
    public List<SortKey> getPreSortColumns() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean getSortOrder() {
        return isAscending;
    }

    @Override
    public String getSortOrderID() {
        return sortID;
    }

    @Override
    public int getSortedColumn() {
        return sortedColumn;
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Arrays.asList(
                new SortKey(((TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == true) ?
                    SortOrder.ASCENDING : SortOrder.DESCENDING ),
                    TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue()));
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return getLimeComparator();
    }
}
