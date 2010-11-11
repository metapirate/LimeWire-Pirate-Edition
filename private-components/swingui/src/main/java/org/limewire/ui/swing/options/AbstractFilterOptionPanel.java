package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * Creates a reusable OptionPanel that contains a table. The table is
 * expected to be two columns with the second column being reserved for
 * removing that row from the table.
 */
public abstract class AbstractFilterOptionPanel extends OptionPanel {
    
    protected EventList<String> eventList;
    
    public AbstractFilterOptionPanel() {
        eventList = GlazedLists.threadSafeList(new BasicEventList<String>());
    }
    
    protected class FilterTable extends MouseableTable {
        
        public FilterTable(DefaultEventTableModel<String> model) {
            super(model);
            setShowGrid(false, false);
            setColumnSelectionAllowed(false);
            setSelectionMode(0);
            getColumn(1).setCellRenderer(new RemoveButtonRenderer(this));
            getColumn(1).setCellEditor(new RemoveButtonRenderer(this));
        }
    }
      
    protected static class FilterTableFormat extends AbstractTableFormat<String> {

        private static final int NAME_INDEX = 0;
        private static final int BUTTON_INDEX = 1;
        
        public FilterTableFormat(String firstColumnTitle) {
            super(firstColumnTitle, "");
        }

        @Override
        public Object getColumnValue(String baseObject, int column) {
            switch(column) {
                case NAME_INDEX: return baseObject;
                case BUTTON_INDEX: return baseObject;
            }
                
            throw new IllegalStateException("Unknown column:" + column);
        }
    }
    
    protected class RemoveButtonRenderer extends JPanel implements TableCellRenderer, TableCellEditor {
        private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
        
        private final HyperlinkButton button;
        
        public RemoveButtonRenderer(final JXTable table) {

            // lower case since hyperlink
            button = new HyperlinkButton(I18n.tr("remove"));
            button.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = table.rowAtPoint(table.getMousePosition());
                    eventList.remove(index);
                    cancelCellEditing();
                }
            });
            
            setLayout(new MigLayout("insets 2 5 2 5, hidemode 0, align 50%"));
            add(button);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            button.setVisible(eventList.contains(value));
            if(isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            button.setVisible(eventList.contains(value));
            setBackground(table.getSelectionBackground());
            return this;
        }

        @Override
        public void cancelCellEditing() {
            synchronized (listeners) {
                for (int i=0, N=listeners.size(); i<N; i++) {
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
                if (listeners.contains(lis)) listeners.remove(lis);
            }
        }
        
        @Override
        public void addCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (!listeners.contains(lis)) listeners.add(lis);
            }
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            cancelCellEditing();
            return true;
        }
    }
}
