package org.limewire.ui.swing.table;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Creates a Popup Menu for selecting which TableColumns are visible in the table.
 */
public class TableColumnSelector {

    private final JPopupMenu popupMenu;
    
    private final JXTable table;
    
    private final ColumnListener listener;
    
    public TableColumnSelector(JXTable table, VisibleTableFormat format) {
        popupMenu = new JPopupMenu();
        this.listener = new ColumnListener();
        this.table = table;
        
        addColumnHeader(format);
    }
    
    private void addColumnHeader(VisibleTableFormat format) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for(int i = 0; i < format.getColumnCount(); i++) {
            if(!format.isColumnHideable(i))
                continue;
            map.put(format.getColumnName(i), Integer.valueOf(i));
        }
        TreeSet<String> sortedSet = new TreeSet<String>(map.keySet());
        for(String value : sortedSet) {
            JMenuItem item = createMenuItem(format, map.get(value));
            item.addActionListener(listener);
            popupMenu.add(item);
        }
    }
    
    private JMenuItem createMenuItem(TableFormat format, int index) {
        return new JCheckBoxMenuItem(format.getColumnName(index), table.getColumnExt(format.getColumnName(index)).isVisible());
    }
    
    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }
    
    private class ColumnListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String columnName = e.getActionCommand();
            TableColumnExt column = table.getColumnExt(columnName);
            if(column.isVisible() && table.getColumnCount(false) == 1) { 
                //TODO: show error message? 
                return;
            }
            column.setVisible(!column.isVisible());
        }
    }
}
