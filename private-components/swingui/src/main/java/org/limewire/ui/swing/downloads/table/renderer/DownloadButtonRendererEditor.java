package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.JTable;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.table.TableRendererEditor;

import com.google.inject.Inject;

public class DownloadButtonRendererEditor extends TableRendererEditor{
    private DownloadButtonPanel buttonPanel;
    private DownloadItem item;
    
    @Inject
    public DownloadButtonRendererEditor(){        
        buttonPanel = new DownloadButtonPanel();
        
        setLayout(new BorderLayout());
        add(buttonPanel);
    }
    
    @Inject
    public void setActionHandler(final DownloadActionHandler actionHandler) {
        buttonPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionHandler.performAction(e.getActionCommand(), item);
                cancelCellEditing();
            }
        });
    }

    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value instanceof DownloadItem) {
            item =(DownloadItem)value;
            buttonPanel.updateButtons(item);
            return this;
        } else {
            return emptyPanel;
        }
    }

    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof DownloadItem) {
            buttonPanel.updateButtons(((DownloadItem)value));
            return this;
        } else {
            return emptyPanel;
        }
    }
    
    @Override
    public final boolean shouldSelectCell(EventObject e) {
        return true;
    }

}
