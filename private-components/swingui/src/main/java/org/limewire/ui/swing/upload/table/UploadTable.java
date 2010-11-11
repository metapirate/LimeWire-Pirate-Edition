package org.limewire.ui.swing.upload.table;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.transfer.TransferTable;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Table to display the list of UploadItems.
 */
public class UploadTable extends TransferTable<UploadItem> {
    
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
    
    private final CategoryIconManager iconManager;
    private final UploadActionHandler uploadActionHandler;
    private final DefaultEventSelectionModel<UploadItem> selectionModel;
    private final UploadProgressRenderer uploadProgressRenderer;
    
    @Inject
    public UploadTable(@Assisted UploadMediator uploadMediator,
            LibraryManager libraryManager,
            CategoryIconManager iconManager,
            ProgressBarDecorator progressBarDecorator,
            UploadPopupMenuFactory popupMenuFactory,
            UploadActionHandler uploadActionHandler) {
        super(uploadMediator.getUploadList(), new UploadTableFormat());
        
        this.iconManager = iconManager;
        this.uploadActionHandler = uploadActionHandler;
        this.uploadProgressRenderer = new UploadProgressRenderer(progressBarDecorator);
        
        GuiUtils.assignResources(this);
        
        // Set selection model.
        selectionModel = new DefaultEventSelectionModel<UploadItem>(uploadMediator.getUploadList());
        selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        setSelectionModel(selectionModel);
        
        setRowHeight(rowHeight);
        setPopupHandler(new UploadPopupHandler(this, popupMenuFactory));
        setEnterKeyAction(new LaunchAction());
        
        initializeColumns();
        initializeRenderers();
    }

    /**
     * Initializes the columns in the table.
     */
    private void initializeColumns() {
        setColumnWidths(UploadTableFormat.TITLE_COL, titleMinWidth, titlePrefWidth, titleMaxWidth);
        setColumnWidths(UploadTableFormat.TITLE_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.PROGRESS_COL, progressMinWidth, progressPrefWidth, progressMaxWidth);
        setColumnWidths(UploadTableFormat.PROGRESS_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.MESSAGE_COL, messageMinWidth, messagePrefWidth, messageMaxWidth);
        setColumnWidths(UploadTableFormat.MESSAGE_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.ACTION_COL, actionMinWidth, actionPrefWidth, actionMaxWidth);
        setColumnWidths(UploadTableFormat.ACTION_GAP, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setColumnWidths(UploadTableFormat.CANCEL_COL, cancelMinWidth, cancelPrefWidth, cancelMaxWidth);
    }
    
    /**
     * Initializes the cell renderers and editors in the table.
     */
    private void initializeRenderers() {
        // Set column renderers.
        setColumnRenderer(UploadTableFormat.TITLE_COL, new UploadTitleRenderer(iconManager));
        setColumnRenderer(UploadTableFormat.MESSAGE_COL, new UploadMessageRenderer(uploadActionHandler));
        setColumnRenderer(UploadTableFormat.PROGRESS_COL, uploadProgressRenderer);
        setColumnRenderer(UploadTableFormat.ACTION_COL, new UploadActionRendererEditor(null));
        setColumnRenderer(UploadTableFormat.CANCEL_COL, new UploadCancelRendererEditor(null));
        
        // Set column gap renderers.
        TableCellRenderer gapRenderer = new GapRenderer();
        setColumnRenderer(UploadTableFormat.TITLE_GAP, gapRenderer);
        setColumnRenderer(UploadTableFormat.PROGRESS_GAP, gapRenderer);
        setColumnRenderer(UploadTableFormat.MESSAGE_GAP, gapRenderer);
        setColumnRenderer(UploadTableFormat.ACTION_GAP, gapRenderer);
        
        // Set column editors.
        setColumnEditor(UploadTableFormat.ACTION_COL, new UploadActionRendererEditor(uploadActionHandler));
        setColumnEditor(UploadTableFormat.CANCEL_COL, new UploadCancelRendererEditor(uploadActionHandler));
        setColumnEditor(UploadTableFormat.MESSAGE_COL, new UploadMessageRenderer(uploadActionHandler));
    }
    
    /**
     * Returns a list of the selected upload items.
     */
    public List<UploadItem> getSelectedItems() {
        return Collections.unmodifiableList(selectionModel.getSelected());
    }
    
    /**
     * Returns the upload item associated with the specified table row.
     */
    public UploadItem getUploadItem(int row) {
        return getElementAt(row);
    }
    
    /**
     * Action to launch the selected upload.
     */
    private class LaunchAction extends AbstractAction {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<UploadItem> selectedItems = getSelectedItems();
            if (selectedItems.size() == 1) {
                UploadItem item = selectedItems.get(0);
                if (item.getUploadItemType() == UploadItemType.GNUTELLA && !UploadMediator.isBrowseHost(item)) {
                    uploadActionHandler.performAction(UploadActionHandler.LAUNCH_COMMAND, item);
                }
            }
        }
    }
}
