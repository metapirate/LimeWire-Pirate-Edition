package org.limewire.ui.swing.table;

import ca.odell.glazedlists.gui.TableFormat;

/** A table that only has a single column. */
public class SingleColumnTableFormat<T> implements TableFormat<T> {
    
    private final String name;
    
    public SingleColumnTableFormat(String columnName) {
        this.name = columnName;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        return name;
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        return baseObject;
    }

}
