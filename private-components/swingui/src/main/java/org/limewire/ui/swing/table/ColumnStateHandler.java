package org.limewire.ui.swing.table;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.ui.swing.settings.TablesHandler;

/**
 * Saves the state of columns in a given table. 
 */
public class ColumnStateHandler implements TableColumnModelListener, MouseListener, PropertyChangeListener {

    private final GlazedJXTable table;
    private final VisibleTableFormat format;
    
    
    private boolean columnMoved = false;
    private boolean visibleChangeEnabled = true;
    
    public ColumnStateHandler(GlazedJXTable table, VisibleTableFormat format) {
        this.table = table;
        this.format = format;
        startListening();
        for(int i = 0; i < format.getColumnCount(); i++) {
            table.getColumnExt(i).addPropertyChangeListener(this);
        }
    }
    
    private void startListening() {
        table.getTableHeader().addMouseListener(this);
        table.getColumnModel().addColumnModelListener(this);
//        for(int i = 0; i < format.getColumnCount(); i++) {
//            table.getColumnExt(i).addPropertyChangeListener(this);
//        }
    }
    
    private void stopListening() {
        table.getTableHeader().removeMouseListener(this);
        table.getColumnModel().removeColumnModelListener(this);
//        for(int i = 0; i < format.getColumnCount(); i++) {
//            table.getColumnExt(i).removePropertyChangeListener(this);
//        }
    }
    
    public void removeListeners() {
        stopListening();
        for(int i = 0; i < format.getColumnCount(); i++) {
            TableColumnExt ext = table.getColumnExt(format.getColumnName(i));
            if(ext != null)
                ext.removePropertyChangeListener(this);
        }
    }
    
    @Override
    public void columnAdded(TableColumnModelEvent e) {}

    @Override
    public void columnMarginChanged(ChangeEvent e) {}


    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if( e.getFromIndex() == e.getToIndex() || !table.isShowing()) return;
        
        // wait till after the mouse was released to save new column ordering
        columnMoved = true;
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {}

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if(SwingUtilities.isLeftMouseButton(e)) {
            TableColumn column = table.getSortedColumn();
            if(column != null) {
                SortOrder sortOrder = getSortOrder(table, column.getModelIndex());
                setSortedColumn(column.getModelIndex(), sortOrder.isAscending());
            }
        }
    }
    
    private SortOrder getSortOrder(GlazedJXTable table, int modelColumn) {
        SortController sortController = table.getSortController();
        if (sortController == null) {
            return SortOrder.UNSORTED;
        }
        
        List<? extends SortKey> sortKeys = sortController.getSortKeys();
        if (sortKeys == null) {
            return SortOrder.UNSORTED;
        }
        
        SortKey firstKey = SortKey.getFirstSortingKey(sortKeys);
        if ((firstKey != null) && (firstKey.getColumn() == modelColumn)) {
            return firstKey.getSortOrder();
        } else {
            return SortOrder.UNSORTED;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
        //if columns were moved, save any changes after the moving stopped
        if(columnMoved) {
            columnMoved = false;

            saveColumnOrder();
        }
    }
    
    /**
     * Saves the current column ordering to disk if a column is not in its default
     * index. Ignores hidden columns and saves the current ordering of visible
     * columns.
     */
    private void saveColumnOrder() {
        for(int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumn(i);
            ColumnStateInfo info = format.getColumnInfo(column.getModelIndex());
            if(info.getPreferredViewIndex() != i) {
                info.setPreferredViewIndex(i);
                setOrder(info, i);
            }
        }
    }
    
    public void revertToDefault() {
//        stopListening();

        //TODO: revert to defaults

//        startListening();
    }
    
    public void setupColumnWidths() {
        for(int i = 0; i < format.getColumnCount(); i++) {
            if(format.getMaxsWidth(i) != -1) {
                table.getColumn(i).setMaxWidth(format.getMaxsWidth(i));
            }
            table.getColumn(i).setPreferredWidth(format.getInitialWidth(i));
        }
    }
    
    /**
     * Sets the column order of the table based on the preffered index of each column.
     * Because table.getColumnModel.move is not stable. Meaning moving an item to index 2 might 
     * move the item already there to the left, or maybe to the right. We have to jump through a few
     * hoops to make it stable.
     * First we make a list ordering all columns in the reverse preferred order, then move each item
     * from where it currently is in the column model to the zero index. Since items cannot be moved to
     * the left of the zero index this makes the move stable, and places the items in the proper
     * preferred index order.
     */
    public void setupColumnOrder() {
        stopListening();
        List<TableColumn> columns = new ArrayList<TableColumn>();
        for(int i = 0; i < table.getColumnCount(); i++) {
            TableColumn tableColumn = table.getColumn(i);
            columns.add(tableColumn);
        }
        Collections.sort(columns, new Comparator<TableColumn> () {
           @Override
            public int compare(TableColumn o1, TableColumn o2) {
                ColumnStateInfo info1 = format.getColumnInfo(o1.getModelIndex());
                ColumnStateInfo info2 = format.getColumnInfo(o2.getModelIndex());
                Integer prefferedIndex1 = info1.getPreferredViewIndex();
                Integer prefferedIndex2 = info2.getPreferredViewIndex();
                return prefferedIndex1.compareTo(prefferedIndex2) * -1;
            } 
        });
        
        for(TableColumn tableColumn : columns) {
            int currentIndex = getCurrentIndex(tableColumn);
            if(currentIndex > 0 && currentIndex < table.getColumnCount()) {
                table.getColumnModel().moveColumn(currentIndex, 0);
            }
        }
       
        startListening();
    }
    
    private int getCurrentIndex(TableColumn tableColumn) {
        for(int i = 0; i < table.getColumnCount(); i++) {
            if(table.getColumn(i) == tableColumn) {
                return i;
            }
        }
        return -1;
    }

    public void setupColumnVisibility() {
        setupColumnVisibility(true);
    }
    
    /**
     * Applies the column visibility attributes to the table columns.  If 
     * <code>propertyChangeEnabled</code> is false, then changes to the visible
     * property in the columns are NOT handled.  This is useful in situations
     * where the column visibility is initialized before the column order has
     * been set up.  (This occurs when the search results table format changes.)
     * In this situation, handling the visible property change would cause the 
     * default column order to be saved, thereby overwriting any custom column
     * reordering done by the user.
     * @see #propertyChange(PropertyChangeEvent)
     */
    public void setupColumnVisibility(boolean propertyChangeEnabled) {
        visibleChangeEnabled = propertyChangeEnabled;
        stopListening();

        for(int i = format.getColumnCount()-1; i >= 0; i--) {
            TableColumnExt column = table.getColumnExt(i);
            column.setVisible(format.isVisibleAtStartup(column.getModelIndex()));
        }

        startListening();
        visibleChangeEnabled = true;
    }
    
    private void setVisibility(ColumnStateInfo info, boolean isVisible) {
        TablesHandler.getVisibility(info.getId(), info.isDefaultlyShown()).setValue(isVisible);
    }
    
    private void setOrder(ColumnStateInfo column, int order) {
        TablesHandler.getOrder(column.getId(), column.getModelIndex()).setValue(order);
    }
    
    private void setWidth(ColumnStateInfo column, int width) {
        TablesHandler.getWidth(column.getId(), column.getDefaultWidth()).setValue(width);
    }
    
    private void setSortedColumn(int sortedColumn, boolean sortOrder) {
        TablesHandler.getSortedColumn(format.getSortOrderID(), format.getSortedColumn()).setValue(sortedColumn);
        TablesHandler.getSortedOrder(format.getSortOrderID(), format.getSortOrder()).setValue(sortOrder);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {        
        //width of column changed
        if(evt.getPropertyName().equals("width") && table.isShowing()) {
            ColumnStateInfo info = format.getColumnInfo(((TableColumnExt)evt.getSource()).getModelIndex());
            setWidth(info,(Integer) evt.getNewValue() );
        }
        
        //visibility changed
        if(evt.getPropertyName().equals("visible") && table.isShowing() && visibleChangeEnabled) {
            ColumnStateInfo info = format.getColumnInfo(((TableColumnExt)evt.getSource()).getModelIndex());
            setVisibility(info, Boolean.TRUE.equals(evt.getNewValue()));
            
            //column visibility changed, so check the ordering
            saveColumnOrder();
        }
    }
}
