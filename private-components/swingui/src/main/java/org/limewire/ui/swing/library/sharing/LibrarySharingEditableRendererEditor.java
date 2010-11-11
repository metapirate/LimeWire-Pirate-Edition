package org.limewire.ui.swing.library.sharing;

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
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.Network;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

class LibrarySharingEditableRendererEditor extends JCheckBox implements TableCellRenderer, TableCellEditor {

    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color backgroundColor;
    private @Resource Icon checkedCheckBox;
    private @Resource Icon uncheckedCheckBox;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private EditableSharingData data;
    
    public LibrarySharingEditableRendererEditor() {

        GuiUtils.assignResources(this);
       
        setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        setIcon(uncheckedCheckBox);
        setSelectedIcon(checkedCheckBox);
        setOpaque(false);
        setIconTextGap(6);
        setFont(font);
        setFocusPainted(false);
        setForeground(fontColor);
        addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(data != null) {
                    data.setSelected(isSelected());
                }
                stopCellEditing();
            }
        });
                
        setBackground(backgroundColor);
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {

        if(value instanceof EditableSharingData) {
            data = (EditableSharingData) value;
            setText(textFor(data));
            setSelected(data.isSelected());
            setToolTipText(getToolTipText(data));
        } else {
            setText("");
            setSelected(false);
            setToolTipText("");
        }     
        return this;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value instanceof EditableSharingData) {
            EditableSharingData data = (EditableSharingData) value;
            setText(textFor(data));
            setSelected(data.isSelected());
            setToolTipText(getToolTipText(data));
        } else {
            setText("");
            setSelected(false);
            setToolTipText("");
        }     
        return this;
    }
    
    private String getToolTipText(EditableSharingData data) {
        if(data.getFriend() != null) {
            if(data.getFriend().getNetwork().getType() != Network.Type.FACEBOOK) {
                return data.getFriend().getRenderName() + " <" + data.getFriend().getId() + ">";
            } else {
                return data.getFriend().getRenderName();
            }
        } else {
            return I18n.tr("{0} friends from other accounts", data.getIds().size());
        }
    }
    
    private String textFor(EditableSharingData data) {
        if(data.getFriend() != null) {
            return data.getFriend().getRenderName();
        } else {
            return I18n.tr("{0} friends from other accounts", data.getIds().size());
        }
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
    public Object getCellEditorValue() {
        return null;
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
        return true;
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