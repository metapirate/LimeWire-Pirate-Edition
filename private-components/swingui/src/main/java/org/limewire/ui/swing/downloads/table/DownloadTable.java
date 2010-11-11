package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.downloads.table.renderer.DownloadButtonRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadCancelRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadMessageRendererEditorFactory;
import org.limewire.ui.swing.downloads.table.renderer.DownloadProgressRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadTitleRenderer;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.transfer.TransferTable;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 */
public class DownloadTable extends TransferTable<DownloadItem> {

    @Resource private int rowHeight;  
    @Resource private int gapMinWidth;  
    @Resource private int gapPrefWidth; 
    @Resource private int gapMaxWidth; 
    @Resource private int titleMinWidth;  
    @Resource private int titlePrefWidth; 
    @Resource private int titleMaxWidth;  
    @Resource private int progressMinWidth;
    @Resource private int progressPrefWidth; 
    @Resource private int progressMaxWidth;  
    @Resource private int messageMinWidth; 
    @Resource private int messagePrefWidth; 
    @Resource private int messageMaxWidth;  
    @Resource private int actionMinWidth; 
    @Resource private int actionPrefWidth;  
    @Resource private int actionMaxWidth; 
    @Resource private int cancelMinWidth; 
    @Resource private int cancelPrefWidth;
    @Resource private int cancelMaxWidth;
    
    private DownloadTableModel model;

    private EventList<DownloadItem> selectedItems;
    
    private final DownloadActionHandler actionHandler;
    private final Provider<PlayerMediator> playerMediator;
    private final DownloadMessageRendererEditorFactory messageRendererEditorFactory;

    @Inject
	public DownloadTable(DownloadTitleRenderer downloadTitleRenderer, DownloadProgressRenderer downloadProgressRenderer, 
	        DownloadMessageRendererEditorFactory messageRendererEditorFactory, DownloadCancelRendererEditor cancelEditor,
	        DownloadButtonRendererEditor buttonEditor, DownloadActionHandler actionHandler, DownloadPopupHandlerFactory downloadPopupHandlerFactory,
	        @Assisted EventList<DownloadItem> downloadItems, DownloadableTransferHandler downloadableTransferHandler, Provider<PlayerMediator> playerMediator) {
        super(new DownloadTableModel(downloadItems));
        
        this.actionHandler = actionHandler;
        this.playerMediator = playerMediator;
        this.messageRendererEditorFactory = messageRendererEditorFactory;
        
        GuiUtils.assignResources(this);
                
        initialize(downloadItems, buttonEditor, cancelEditor, downloadPopupHandlerFactory);
        
        addHighlighter(createDisabledHighlighter(new ThreatHighlightPredicate()));
        
        TableCellRenderer gapRenderer = new GapRenderer();
        setUpColumn(DownloadTableFormat.TITLE, downloadTitleRenderer, titleMinWidth, titlePrefWidth, titleMaxWidth);
        setUpColumn(DownloadTableFormat.TITLE_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.PROGRESS, downloadProgressRenderer, progressMinWidth, progressPrefWidth, progressMaxWidth);
        setUpColumn(DownloadTableFormat.PROGRESS_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.MESSAGE, messageRendererEditorFactory.create(null), messageMinWidth, messagePrefWidth, messageMaxWidth);
        setUpColumn(DownloadTableFormat.MESSAGE_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.ACTION, new DownloadButtonRendererEditor(), actionMinWidth, actionPrefWidth, actionMaxWidth);
        setUpColumn(DownloadTableFormat.ACTION_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.CANCEL, new DownloadCancelRendererEditor(), cancelMinWidth, cancelPrefWidth, cancelMaxWidth);
        
        setTransferHandler(downloadableTransferHandler);
        setDragEnabled(true);
        setRowHeight(rowHeight);        
    }
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}
	
    /** Returns a copy of all selected items. */
    public List<DownloadItem> getSelectedItems() {
        return new ArrayList<DownloadItem>(selectedItems);
    }
       
    public void selectAndScrollTo(URN urn) {
        if(urn != null) {
            for(int y=0; y < model.getRowCount(); y++) {
                DownloadItem item = getDownloadItem(y);
                if(item != null && item.getUrn() != null && urn.equals(item.getUrn())) {
                    getSelectionModel().setSelectionInterval(y, y);
                    ensureRowVisible(y);
                    break;
                }
            }
        }        
    }
    
    public void selectAndScrollTo(DownloadItem item) {
        for(int y = 0; y < model.getRowCount(); y++) {
            if(item == getDownloadItem(y)) {
                getSelectionModel().setSelectionInterval(y, y);
                ensureRowVisible(y);
                break;
            }
        }
    }
    
    private void setUpColumn(int index, TableCellRenderer renderer, int minWidth, int prefWidth, int maxWidth){
        setColumnRenderer(index, renderer);
        setColumnWidths(index, minWidth, prefWidth, maxWidth);
    }

    private void initialize(EventList<DownloadItem> downloadItems, DownloadButtonRendererEditor buttonEditor, 
            DownloadCancelRendererEditor cancelEditor, DownloadPopupHandlerFactory downloadPopupHandlerFactory) {
        model = (DownloadTableModel) getModel();
        
        DefaultEventSelectionModel<DownloadItem> model = new DefaultEventSelectionModel<DownloadItem>(downloadItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.selectedItems = model.getSelected();

        TablePopupHandler popupHandler = downloadPopupHandlerFactory.create(this);

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
               launch(row);
            }
        };

        setDoubleClickHandler(clickHandler);        
        
        Action enterAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = getSelectedRow();
                if (row > -1) {
                    launch(row);
                }
            }
        };        

        setEnterKeyAction(enterAction);
        
        getColumnModel().getColumn(DownloadTableFormat.MESSAGE).setCellEditor(messageRendererEditorFactory.create(actionHandler));
        getColumnModel().getColumn(DownloadTableFormat.ACTION).setCellEditor(buttonEditor);
        getColumnModel().getColumn(DownloadTableFormat.CANCEL).setCellEditor(cancelEditor);

    }
    
    private void launch(int row){
        DownloadItem item = getDownloadItem(row);
        if(item != null && item.isLaunchable()) {
            DownloadItemUtils.launch(item, playerMediator);
        }
    }
    
    /**
     * Implementation of Predicate that specifies rules to highlight table row
     * containing download threat.
     */
    private class ThreatHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            DownloadItem item = getDownloadItem(adapter.row);
            DownloadState state = item.getState();
            return (state == DownloadState.DANGEROUS ||
                    state == DownloadState.THREAT_FOUND ||
                    state == DownloadState.SCAN_FAILED);
        }
    }
}
