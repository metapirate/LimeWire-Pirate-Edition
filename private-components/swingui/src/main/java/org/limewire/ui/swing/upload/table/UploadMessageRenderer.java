package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.transfer.TransferRendererResources;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer for the message column in the Uploads table.
 */
class UploadMessageRenderer extends TableRendererEditor {

    @Resource private Icon infoIcon;
    
    private final NumberFormat formatter = new DecimalFormat("0.00");
    
    private final UploadActionHandler actionHandler;
    private JLabel messageLabel;
    private JButton infoButton;
    
    private UploadItem uploadItem;
    
    /**
     * Constructs an UploadMessageRenderer.
     */
    public UploadMessageRenderer(UploadActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        new TransferRendererResources().decorateComponent(this);
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, aligny center, nogrid, novisualpadding"));
        
        messageLabel = new JLabel();
        
        infoButton = new IconButton(infoIcon);
        infoButton.setActionCommand(UploadActionHandler.PROPERTIES_COMMAND);
        infoButton.setToolTipText(I18n.tr("Info"));
        infoButton.addActionListener(new ButtonListener());
        
        add(messageLabel, "");
        add(infoButton, "");
    }
    
    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if (value instanceof UploadItem) {
            uploadItem = (UploadItem) value;
            setButtonVisible(uploadItem);
            messageLabel.setText(getMessage(uploadItem));
            return this;
        } else {
            setButtonVisible(null);
            return emptyPanel;
        }
    }

    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof UploadItem) {
            uploadItem = (UploadItem) value;
            messageLabel.setText(getMessage(uploadItem));
            setButtonVisible(uploadItem);
            return this;
        } else {
            setButtonVisible(null);
            return emptyPanel;
        }
    }
    
    private void setButtonVisible(UploadItem item) {
        if(item == null) {
            infoButton.setVisible(false);
        } else {
            infoButton.setVisible(item.getState() != UploadState.BROWSE_HOST && item.getState() != UploadState.BROWSE_HOST_DONE);
        }
    }
    
    /**
     * Returns the display message for the specified upload item.
     */
    private String getMessage(UploadItem item) {
        switch (item.getState()) {
        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            return I18n.tr("Library was browsed");
            
        case DONE:
            return I18n.tr("Done uploading");
            
        case UPLOADING:
            if (UploadItemType.BITTORRENT == item.getUploadItemType()) {
                int numConnections = item.getNumUploadConnections();
                String ratio = formatter.format(item.getSeedRatio());
                return I18n.trn("{0} to {1} person - ratio: {2}",
                        "{0} to {1} people - ratio: {2}",
                        numConnections, GuiUtils.formatKilobytesPerSec(item.getUploadSpeed()), numConnections, ratio);
            } else {
                return I18n.tr("{0} of {1} ({2})", 
                        GuiUtils.formatUnitFromBytes(item.getTotalAmountUploaded()), 
                        GuiUtils.formatUnitFromBytes(item.getFileSize()), 
                        GuiUtils.formatKilobytesPerSec(item.getUploadSpeed()));
            }
            
        case PAUSED:
            return I18n.tr("Paused");
            
        case QUEUED:
            return I18n.tr("Waiting...");
            
        case REQUEST_ERROR:
            return I18n.tr("Unable to upload: invalid request");
            
        case LIMIT_REACHED:
            return I18n.tr("Unable to upload: upload limit reached");

        default:
            return "";
        }
    }
    
    /**
     * Action listener for editor buttons.
     */
    private class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Reset cursor if source component is IconButton.  If the action
            // displays a modal dialog, then IconButton does not receive the
            // mouseExited event to reset the default cursor.
            if (e.getSource() instanceof IconButton) {
                ((IconButton) e.getSource()).resetDefaultCursor();
            }
            
            actionHandler.performAction(e.getActionCommand(), uploadItem);
            cancelCellEditing();
        }
    }
}
