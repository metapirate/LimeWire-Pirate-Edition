package org.limewire.ui.swing.properties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.components.YesNoCheckBoxDialog;
import org.limewire.ui.swing.library.table.RemoveButton;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.settings.QuestionsHandler;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class FileInfoSharingPanel implements FileInfoPanel {

    @Resource private Color foreground;
    @Resource private Font headerFont;
    @Resource private Icon publicIcon;
    @Resource private Icon listSharedIcon;
    
    private final JPanel component;
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    private final SharedFileListManager sharedFileListManager;
    
    public FileInfoSharingPanel(FileInfoType type, PropertiableFile propertiableFile, 
            SharedFileListManager sharedFileListManager) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        this.sharedFileListManager = sharedFileListManager;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx"));
        
        init();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
        //currently the lists are updated on click, not on save
    }
    
    @Override
    public void dispose() {
        //no listeners registered
    }
    
    @Override
    public void updatePropertiableFile(PropertiableFile file) {
        //do nothing
    }
    
    private void init() {
        component.setOpaque(false);
        switch(type) {
        case LOCAL_FILE:
            if(propertiableFile instanceof LocalFileItem) {
                if(((LocalFileItem)propertiableFile).isShareable()) {
                    EventList<SharedFileList> sharedWithList = getSharedWithList((LocalFileItem)propertiableFile);
                    if(sharedWithList.size() > 0) {
                        component.add(createHeaderLabel(I18n.tr("Sharing from these lists")), "span, wrap");
                        
                        JXTable table = new MouseableTable(new DefaultEventTableModel<SharedFileList>(sharedWithList, 
                                new TableFormat<SharedFileList>() {
                                    @Override
                                    public int getColumnCount() {
                                        return 3;
                                    }
                                    @Override
                                    public String getColumnName(int column) {
                                        return null;
                                    }
                                    @Override
                                    public Object getColumnValue(SharedFileList baseObject, int column) {
                                        switch (column) {
                                        
                                        case 0 :
                                            return baseObject.isPublic() ? publicIcon : listSharedIcon;
                                        case 2 :
                                            return baseObject;
                                        default :
                                            return baseObject.getCollectionName();
                                        
                                        }
                                    }
                                }
                        ));
                        table.setShowGrid(false, false);
                        table.setTableHeader(null);
                        
                        table.getColumn(0).setCellRenderer(new IconRenderer());
                        table.getColumn(0).setMinWidth(26);
                        table.getColumn(0).setMaxWidth(26);
                        table.getColumn(0).setWidth(26);
                        
                        JScrollPane scroll = new JScrollPane(table);
                        scroll.setOpaque(false);
                        scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                        
                        TableColumn column = table.getColumn(2);
                        column.setCellRenderer(new RemoveRenderer());
                        column.setCellEditor(new RemoveEditor());
                        column.setMinWidth(16);
                        column.setMaxWidth(16);
                        column.setWidth(16);
                         
                        component.add(scroll, "grow, wrap");
                    } else {
                        component.add(createHeaderLabel(I18n.tr("This file is not shared")), "span, wrap");
                    }
                } else {
                    component.add(createHeaderLabel(I18n.tr("This file cannot be shared")), "span, wrap");
                }
            }
            break;
        }
    }
    
    /**
     * Returns list of file lists that are shared and contain this file.
     */
    private EventList<SharedFileList> getSharedWithList(LocalFileItem fileItem) {
        EventList<SharedFileList> sharedWith = new BasicEventList<SharedFileList>();
        
        sharedFileListManager.getModel().getReadWriteLock().readLock().lock();
        try {
            for(SharedFileList sharedFileList : sharedFileListManager.getModel()) {
                if(sharedFileList.contains(fileItem.getFile()) && sharedFileList.getFriendIds().size() > 0)
                    sharedWith.add(sharedFileList);
            }
        } finally {
            sharedFileListManager.getModel().getReadWriteLock().readLock().unlock();
        }
        return sharedWith;
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        label.setForeground(foreground);
        return label;
    }
    
    private boolean showConfirmation(String message) {
        if (!QuestionsHandler.CONFIRM_REMOVE_FILE_INFO_SHARING.getValue()) {
            // no need to confirm here
            return true;
        }

        final YesNoCheckBoxDialog yesNoCheckBoxDialog = new YesNoCheckBoxDialog(I18n.tr("Remove File"), message, I18n
                .tr("Don't ask me again"), !QuestionsHandler.CONFIRM_REMOVE_FILE_INFO_SHARING.getValue(),
                I18n.tr("Yes"), I18n.tr("No"));
        yesNoCheckBoxDialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        yesNoCheckBoxDialog.setVisible(true);

        QuestionsHandler.CONFIRM_REMOVE_FILE_INFO_SHARING.setValue(!yesNoCheckBoxDialog.isCheckBoxSelected());
        
        return yesNoCheckBoxDialog.isConfirmed();
    }
    
    
    private class RemoveRenderer extends JPanel implements TableCellRenderer {
        
        private final JButton button = new RemoveButton();

        public RemoveRenderer() {
            super(new BorderLayout());
            add(button, BorderLayout.CENTER);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, 
                int row, int column) {
            
            return this;
        }
        
    }
    
    private class RemoveEditor extends JPanel implements TableCellEditor {
        
        private final JButton button = new RemoveButton();
        
        private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
        
        private SharedFileList activeList = null;
        
        public RemoveEditor() {
           
            super(new BorderLayout());
            
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
            
                    if(showConfirmation(I18n.tr("Remove {0} from list {1}?",
                            propertiableFile.getFileName(), activeList.getCollectionName()))) {
               
                        activeList.removeFile(((LocalFileItem)propertiableFile).getFile());
                    }
                }
            });
            
            add(button, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2,
                int arg3, int arg4) {
            
            if (arg1 == null) {
                return null;
            }
            
            activeList = (SharedFileList)arg1;
            return this;
        }

        @Override
        public Object getCellEditorValue() {
            return activeList;
        }

        @Override
        public void addCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (!listeners.contains(lis))
                    listeners.add(lis);
            }
        }

        @Override
        public void cancelCellEditing() {
            synchronized (listeners) {
                for (int i = 0, N = listeners.size(); i < N; i++) {
                    listeners.get(i).editingCanceled(new ChangeEvent(this));
                }
            }
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (listeners.contains(lis))
                    listeners.remove(lis);
            }
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return false;
        }

        @Override
        public boolean stopCellEditing() {
            synchronized (listeners) {
                for (int i = 0, N = listeners.size(); i < N; i++) {
                    listeners.get(i).editingStopped(new ChangeEvent(this));
                }
            }
            return true;
        }
    }
    
    private static class IconRenderer extends JPanel implements TableCellRenderer {

        private static final Border border = BorderFactory.createEmptyBorder(0,5,0,5);
        private final JLabel label = new JLabel();
        
        public IconRenderer() {
            super(new BorderLayout());
            add(label, BorderLayout.CENTER);
            setBorder(border);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            label.setIcon((Icon)value);
            return this;
        }
        
    }
}
