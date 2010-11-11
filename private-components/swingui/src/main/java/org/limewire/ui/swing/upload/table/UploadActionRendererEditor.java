package org.limewire.ui.swing.upload.table;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.transfer.TransferRendererResources;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer/editor to display the action button for the Uploads table.
 */
class UploadActionRendererEditor extends TableRendererEditor {

    private final UploadActionHandler actionHandler;
    private final JButton pauseButton;
    private final JButton resumeButton;
    
    private UploadItem item;
    
    /**
     * Constructs an UploadActionRendererEditor with the specified action 
     * handler.
     */
    public UploadActionRendererEditor(UploadActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        
        setLayout(new BorderLayout());
        
        TransferRendererResources resources = new TransferRendererResources();
        
        pauseButton = new HyperlinkButton(I18n.tr("Pause"));
        pauseButton.setActionCommand(UploadActionHandler.PAUSE_COMMAND);
        pauseButton.setFont(resources.getFont());
        pauseButton.setToolTipText(I18n.tr("Pause upload"));
        
        resumeButton =  new HyperlinkButton(I18n.tr("Resume"));
        resumeButton.setActionCommand(UploadActionHandler.RESUME_COMMAND);
        resumeButton.setFont(resources.getFont());
        resumeButton.setToolTipText(I18n.tr("Resume upload"));
        
        if (actionHandler != null) {
            ActionListener listener = new ButtonListener();
            pauseButton.addActionListener(listener);
            resumeButton.addActionListener(listener);
        }
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding, fill, aligny center"));
        buttonPanel.setOpaque(false);
        buttonPanel.add(pauseButton, "hidemode 3");
        buttonPanel.add(resumeButton, "hidemode 3");
        
        add(buttonPanel, BorderLayout.CENTER);
    }
    
    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
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

    @Override
    public final boolean shouldSelectCell(EventObject e) {
        return true;
    }
    
    /**
     * Updates the visibility of the buttons.
     */
    private void updateButtons(UploadItem uploadItem) {
        pauseButton.setVisible(UploadMediator.isPausable(uploadItem));
        resumeButton.setVisible(UploadMediator.isResumable(uploadItem));
    }
    
    /**
     * Listener to handle button actions.
     */
    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), item);
            cancelCellEditing();
        }
    }
}
