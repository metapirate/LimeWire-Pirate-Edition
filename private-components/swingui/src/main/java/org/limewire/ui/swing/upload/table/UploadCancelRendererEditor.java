package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.transfer.TransferRendererResources;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer/editor to display the cancel and remove buttons for the 
 * uploads table.
 */
class UploadCancelRendererEditor extends TableRendererEditor {

    private final UploadActionHandler actionHandler;
    private final JButton cancelButton;
    private final JButton removeButton;
    
    private UploadItem item;
    
    /**
     * Constructs an UploadCancelRendererEditor with the specified action 
     * handler.
     */
    public UploadCancelRendererEditor(UploadActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        
        setLayout(new MigLayout("insets 0, gap 0, nogrid, novisualpadding, alignx center, aligny center"));
        
        TransferRendererResources resources = new TransferRendererResources();

        cancelButton = new IconButton();
        resources.decorateCancelButton(cancelButton);
        cancelButton.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        cancelButton.setToolTipText(I18n.tr("Cancel upload"));

        removeButton = new IconButton();
        resources.decorateCancelButton(removeButton);
        removeButton.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        removeButton.setToolTipText(I18n.tr("Remove upload"));

        if (actionHandler != null) {
            ActionListener listener = new ButtonListener();
            cancelButton.addActionListener(listener);
            removeButton.addActionListener(listener);
        }
        
        add(cancelButton, "hidemode 3");
        add(removeButton, "hidemode 3");
    }
    
    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, 
            boolean isSelected, int row, int column) {
        if (value instanceof UploadItem) {
            item = (UploadItem) value;
            updateButtons(item);
            return this;
        } else {
            return emptyPanel;
        }
    }

    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof UploadItem) {
            updateButtons((UploadItem) value);
            return this;
        } else {
            return emptyPanel;
        }
    }
    
    /**
     * Updates the visibility of the buttons.
     */
    private void updateButtons(UploadItem item) {
        boolean removable = UploadMediator.isRemovable(item);
        cancelButton.setVisible(!removable);
        removeButton.setVisible(removable);
    }
    
    /**
     * Listener to handle button actions.
     */
    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // Reset cursor if source component is IconButton.
            if (e.getSource() instanceof IconButton) {
                ((IconButton) e.getSource()).resetDefaultCursor();
            }
            actionHandler.performAction(e.getActionCommand(), item);
            cancelCellEditing();
        }
    }
}
