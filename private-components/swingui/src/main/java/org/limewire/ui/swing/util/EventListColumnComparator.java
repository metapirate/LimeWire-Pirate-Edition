package org.limewire.ui.swing.util;

import java.util.Comparator;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * A comparator that sorts a table by the column that was clicked.  This is a 
 * revised version of the GlazedLists TableColumnComparator.  The main 
 * difference here is that the <code>compare(E, E)</code> method throws an 
 * enhanced exception message if the object comparison fails.
 */
public class EventListColumnComparator<E> implements Comparator<E>  {

    /** the table format knows to map objects to their fields */
    private TableFormat<E> tableFormat;

    /** the field of interest */
    private int column;

    /** comparison is delegated to a Comparator */
    private Comparator comparator = null;

    /**
     * Creates a new EventListColumnComparator that sorts objects by the
     * specified column using the specified table format and the specified 
     * comparator.
     */
    public EventListColumnComparator(TableFormat<E> tableFormat, int column, 
            Comparator comparator) {
        this.column = column;
        this.tableFormat = tableFormat;
        this.comparator = comparator;
    }

    /**
     * Compares the two objects, returning a result based on how they compare.
     */
    @Override
    @SuppressWarnings("unchecked")
    public int compare(E alpha, E beta) {
        final Object alphaField = tableFormat.getColumnValue(alpha, column);
        final Object betaField = tableFormat.getColumnValue(beta, column);
        try {
            return comparator.compare(alphaField, betaField);
        } catch (Exception e) {

            // Handle special case of empty columns
            if ("".equals(alphaField)) {
                if ("".equals(betaField)) {
                    return 0;
                } else {
                    return -1;
                }
            } else if ("".equals(betaField)) {
                return 1;
            }
            
            // Throw exception with table format name and column.
            IllegalStateException ise = new IllegalStateException(
                    "Cannot compare \"" + alphaField + "\" to \"" + betaField + 
                    "\" in " + tableFormat.getClass().getName() + ", column " + column);
            ise.initCause(e);
            throw ise;
        }
    }

    /**
     * Tests if this EventListColumnComparator is equal to the other specified
     * EventListColumnComparator.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final EventListColumnComparator that = (EventListColumnComparator) o;

        if (column != that.column) return false;
        if (!comparator.equals(that.comparator)) return false;
        if (!tableFormat.equals(that.tableFormat)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = tableFormat.hashCode();
        result = 29 * result + column;
        result = 29 * result + comparator.hashCode();
        return result;
    }
}
