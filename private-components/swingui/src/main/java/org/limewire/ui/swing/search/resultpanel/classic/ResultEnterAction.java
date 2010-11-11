package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

/**
 * Handles the enter key on a row in the classic search results table.
 */
public class ResultEnterAction extends AbstractAction {

    private final List<VisualSearchResult> selectedResults;

    private final DownloadHandler downloadHandler;

    public ResultEnterAction(List<VisualSearchResult> selectedResults, DownloadHandler downloadHandler) {
        this.selectedResults = selectedResults;
        this.downloadHandler = downloadHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (VisualSearchResult result : selectedResults) {
            if (result.getDownloadState() == BasicDownloadState.NOT_STARTED &&
                    !result.isSpam()) {
                downloadHandler.download(result);
            }
        }
    }
}
