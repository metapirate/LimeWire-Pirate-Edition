package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.TablePopupHandler;

public class SearchPopupHandler implements TablePopupHandler {

    private final ResultsTable<VisualSearchResult> resultsTable;
    private final SearchResultMenuFactory menuFactory;
    private final DownloadHandler downloadHandler;

    public SearchPopupHandler(DownloadHandler downloadHandler, ResultsTable<VisualSearchResult> resultsTable,
            SearchResultMenuFactory menuFactory) {
        this.downloadHandler = downloadHandler;
        this.resultsTable = resultsTable;
        this.menuFactory = menuFactory;
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        List<Integer> selectedRows = asList(resultsTable.getSelectedRows());
        int popupRow = resultsTable.rowAtPoint(new Point(x, y));
        
        if (selectedRows.size() <= 1 || !selectedRows.contains(popupRow)) {
            selectedRows.clear();
            selectedRows.add(popupRow);
            resultsTable.setRowSelectionInterval(popupRow, popupRow);
        }

        List<VisualSearchResult> selectedItems = new ArrayList<VisualSearchResult>();
        for (Integer row : selectedRows) {
            if (row != -1) {
                VisualSearchResult visualSearchResult = resultsTable.getEventTableModel()
                        .getElementAt(row);
                if (visualSearchResult != null) {
                    selectedItems.add(visualSearchResult);
                }
            }
        }

        menuFactory.create(downloadHandler, selectedItems, SearchResultMenu.ViewType.Table).show(component, x, y);
    }

    private List<Integer> asList(int[] array) {
        List<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }
}