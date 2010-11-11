package org.limewire.ui.swing.search.resultpanel.list;

import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a single-column table
 * that displays the list view of search results.
 */
public class ListViewTableFormat extends ResultsTableFormat<VisualSearchResult> {

    @Override
    public Class getColumnClass(int index) {
        return VisualSearchResult.class;
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
        return vsr;
    }

    @Override
    public int getInitialWidth(int index) {
        return 570;
    }

    @Override
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == 1;
    }
    
    @Override
    public int getNameColumn() {
        //no name column here
        return -1;
    }

    @Override
    public boolean isVisibleAtStartup(int column) {
        return false;
    }

    @Override
    public boolean isColumnHideable(int column) {
        return false;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        return "";
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Collections.emptyList();
    }
    
    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        return Collections.emptyList();
    }
}