package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;
import org.limewire.ui.swing.search.resultpanel.ResultsTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;

/**
 * Handles a double click on a row in the classic search results table.
 */
public class ClassicDoubleClickHandler implements TableDoubleClickHandler {

    private final ResultsTable<VisualSearchResult> resultsTable;
    private final DownloadHandler downloadHandler;
    
    public ClassicDoubleClickHandler(ResultsTable<VisualSearchResult> resultsTable, DownloadHandler downloadHandler) {
        this.resultsTable = resultsTable;
        this.downloadHandler = downloadHandler;
    }
    
    @Override
    public void handleDoubleClick(int row) {
        if (row < 0 || row >= resultsTable.getRowCount()) {
            return;
        }

        VisualSearchResult result = resultsTable.getEventTableModel().getElementAt(row);
        if(!result.isSpam())
            downloadHandler.download(result);
    }
}
