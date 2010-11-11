package org.limewire.ui.swing.table;

import java.util.Comparator;

import ca.odell.glazedlists.gui.WritableTableFormat;

/**
 * Generic table model for Single column tables (list views).  Eliminates lots of boilerplate code.
 */
public class LimeSingleColumnTableFormat<T> extends AbstractAdvancedTableFormat<T> implements WritableTableFormat<T> {

    private Class clazz;

    public LimeSingleColumnTableFormat(Class clazz) {
        super("");
        this.clazz = clazz;
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        if (column == 0)
            return baseObject;

        throw new IllegalStateException("Column "+ column + " out of bounds");
    }

    @Override
    public Class getColumnClass(int column) {
        return clazz;
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return null;
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        if (column == 0)
            return true;
        throw new IllegalStateException("Column "+ column + " out of bounds");
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        if (column == 0)
        return baseObject;
        throw new IllegalStateException("Column "+ column + " out of bounds");
    }

}
