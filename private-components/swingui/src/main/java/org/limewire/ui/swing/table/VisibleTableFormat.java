package org.limewire.ui.swing.table;

import ca.odell.glazedlists.gui.TableFormat;

public interface VisibleTableFormat<T> extends TableFormat<T>{

    /** If true, column is shown at startup.*/
    boolean isVisibleAtStartup(int columnModelIndex);
    
    /** If true, the column cannot be hidden and will not be shown in the remove/add column menu. */
    boolean isColumnHideable(int columnModelIndex);
    
    /** Initial preferred width of the column.*/
    int getInitialWidth(int columnModelIndex);
    
    /** Max width for a given column. */
    int getMaxsWidth(int columnModelIndex);
    
    /** Returns the column state info for this column.*/
    ColumnStateInfo getColumnInfo(int columnModelIndex);
    
    /** Returns the default sort column.*/
    int getSortedColumn();
    
    /** Returns the default sort ordering on sort column.*/
    boolean getSortOrder();
    
    /** Returns the order id for sort information on this table.*/
    String getSortOrderID();
}
