package org.limewire.ui.swing.table;

import org.limewire.util.Objects;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Abstract implementation of {@link TableFormat} that handles the column
 * part of the interface requirements. 
 */
public abstract class AbstractTableFormat<E> implements TableFormat<E> {
    
    private final String[] columnNames;
    
    public AbstractTableFormat(String... columnNames) {
        // if this throws, this could also mean you implicitly used the default
        // constructor
        this.columnNames = Objects.nonNull(columnNames, "columnNames");
    }
    
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

}
