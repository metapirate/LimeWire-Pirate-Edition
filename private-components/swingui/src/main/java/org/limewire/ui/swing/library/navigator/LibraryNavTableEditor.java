package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.OSUtils;

class LibraryNavTableEditor extends JPanel implements TableCellEditor {

    private @Resource Color selectedColor;
    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource int iconGap;
    private @Resource Icon libraryIcon;
    private @Resource Icon publicIcon;
    private @Resource Icon listIcon;
    private @Resource Icon listSharedIcon;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private final JLabel iconHolder;
    private final JTextField textField;
    
    public LibraryNavTableEditor() {
        super(new MigLayout("gap 0, insets 0 6 0 6, fill"));
        
        GuiUtils.assignResources(this);
        
        iconHolder = new JLabel();
        textField = new JTextField();
        TextFieldClipboardControl.install(textField);
        textField.setPreferredSize(new Dimension(120, 22));
        textField.setFont(font);
        textField.setForeground(fontColor);
        textField.setDocument(new TextFieldLimit(40));
        textField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                stopCellEditing();
            }
        });
        // OSX paints a giant blue border when selected which takes up too much room
        // for the table row height
        if(OSUtils.isMacOSX()) 
            textField.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        setBackground(selectedColor);
        
        add(iconHolder, "aligny 50%, gapright " + iconGap);
        add(textField, "growx");
    }
    
    
    void prepareForEditing() {
        textField.requestFocusInWindow();
        textField.selectAll();
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        
        if(value instanceof LibraryNavItem) {
            LibraryNavItem item = (LibraryNavItem) value;
            textField.setText(item.getDisplayText());
            textField.selectAll();
            setIconType(item);
        } else {
            textField.setText("");
            iconHolder.setIcon(null);
        }

        setOpaque(isSelected);
           
        return this;
    }
    
    private void setIconType(LibraryNavItem item) {
        if(item.getType() == NavType.LIBRARY)
            iconHolder.setIcon(libraryIcon);
        else if(item.getType() == NavType.PUBLIC_SHARED)
            iconHolder.setIcon(publicIcon);
        else {
            if(item.getLocalFileList() instanceof SharedFileList) {
                if(((SharedFileList)item.getLocalFileList()).getFriendIds().size() > 0)
                    iconHolder.setIcon(listSharedIcon);
                else
                    iconHolder.setIcon(listIcon);
            } else {
                iconHolder.setIcon(listIcon);
            }
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
        return textField.getText().trim();
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
    
    /**
	 * Sets the max limit for a shared file list name.
	 */
    private static class TextFieldLimit extends PlainDocument {
        private int maxLength;
        
        TextFieldLimit(int maxLength) {
            this.maxLength = maxLength;
        }
        
        @Override
        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if(str == null)
                return;
            if((getLength() + str.length()) < maxLength) {
                super.insertString(offset, str, attr);
            }
        }
    }
}
