package org.limewire.ui.swing.table;

import java.util.Comparator;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public abstract class AbstractColumnStateFormat<T> implements VisibleTableFormat<T>, AdvancedTableFormat<T>, WritableTableFormat<T> {

    private ColumnStateInfo[] columnInfo;
    private final LimeComparator comparator;
    
    public AbstractColumnStateFormat(ColumnStateInfo... columnInfo) {
        this.columnInfo = columnInfo;
        comparator = new LimeComparator();
    }
    
    @Override
    public int getColumnCount() {
        return columnInfo.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnInfo[column].getName();
    }
    
    @Override
    public boolean isVisibleAtStartup(int column) {
        return columnInfo[column].isShown();
    }
    
    @Override
    public boolean isColumnHideable(int column) {
        return columnInfo[column].isHideable();
    }

    @Override
    public int getInitialWidth(int column) {
        return columnInfo[column].getDefaultWidth();
    }
    
    @Override
    public int getMaxsWidth(int column) {
        return columnInfo[column].getMaxWidth();
    }
    
    public ColumnStateInfo getColumnInfo(int column) {
        return columnInfo[column];
    }
    
    public Comparator getLimeComparator() {
        return comparator;
    }
    
    /**
     * A default comparator for Object values.
     */
    private static class LimeComparator implements Comparator<Object> {
        /**
         * Compares the two Object instances, and returns a negative, 0, or 
         * positive integer if the first value is less than, equal to, or 
         * greater than the second value.  Null values are always less than 
         * non-null values.  Only non-null values of the same type are compared.
         */
        @Override
        @SuppressWarnings("unchecked")
        public int compare(Object alpha, Object beta) {    
            // compare nulls
            if (alpha == null) {
                return (beta == null) ? 0 : -1;                
            } else if (beta == null) {
                return 1;
            } else if(alpha == beta) {
                return 0;
            } else if (alpha.getClass().isInstance(beta)) {
                // Compare objects of the same type.  String values use an 
                // I18n-compatible, case-insensitive comparison.  Non-Comparable
                // values throw an exception to report a data model issue.
                if (alpha instanceof String) {
                    return ((String)alpha).compareToIgnoreCase((String)beta);
                    // TODO: This is making things insanely slow -- commenting out for now.
//                    return StringUtils.compareFullPrimary((String) alpha, (String) beta);
                } else if (alpha instanceof Comparable) {
                    return ((Comparable) alpha).compareTo(beta);
                } else {
                    throw new IllegalStateException(alpha.getClass().getName() +
                            " is not Comparable");
                }

            } else {
                // Handle objects of different types.  We could be forgiving
                // and perform a case-insensitive String comparison.  Instead, 
                // we choose to be strict and throw an exception to report a
                // data model issue.
                throw new IllegalStateException("Cannot compare " + 
                        alpha.getClass().getName() + " to " + beta.getClass().getName());
            }
        }
    }

}
