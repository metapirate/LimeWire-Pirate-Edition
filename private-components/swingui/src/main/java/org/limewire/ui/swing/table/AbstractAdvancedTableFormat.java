package org.limewire.ui.swing.table;

import ca.odell.glazedlists.gui.AdvancedTableFormat;

/**
 * Abstract implementation of {@link AdvancedTableFormat} that handles the column
 * part of the interface requirements. 
 */
public abstract class AbstractAdvancedTableFormat<E> extends AbstractTableFormat<E> implements
        AdvancedTableFormat<E> {

    public AbstractAdvancedTableFormat(String...columnNames) {
        super(columnNames);
    }
}
