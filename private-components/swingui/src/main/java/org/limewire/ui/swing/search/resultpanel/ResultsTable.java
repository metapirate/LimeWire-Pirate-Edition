package org.limewire.ui.swing.search.resultpanel;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;

import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.ColumnStateHandler;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColumnSelector;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * Table used to display search results.  ResultsTable is an extension of 
 * MouseableTable that provides the following features:
 * <ul>
 *   <li>Ability to set the table model using Glazed List objects by calling
 *     <code>setEventListFormat()</code>.</li>
 *   <li>Support for saving and restoring column settings using 
 *     <code>ColumnStateHandler</code>.</li>
 *   <li>Table header popup menu to display/hide columns using 
 *     <code>TableColumnSelector</code>.</li>
 * </ul> 
 */
public class ResultsTable<E extends VisualSearchResult> extends MouseableTable { 

    private EventList<E> eventList;
    private ResultsTableFormat<E> tableFormat;
    private DefaultEventTableModel<E> tableModel;
    private ColumnStateHandler columnStateHandler;
    private MousePopupListener mousePopupListener;

    /**
     * Constructs a ResultsTable with no data.
     */
    public ResultsTable() {
        super();
        // Initialize table properties.
        setShowHorizontalLines(false);
        setShowGrid(false, true);
    }

    /**
     * Returns the EventTableModel used by the table.  The returned value may
     * be null if the table has been disposed.
     */
    public DefaultEventTableModel<E> getEventTableModel() {
        return tableModel;
    }
    
    /**
     * Sets the event list, table format, and header display indicator for the
     * table.  This method creates a new TableModel for the table, and updates
     * the column model to reflect the new format.
     */
    public void setEventListFormat(EventList<E> eventList, 
            ResultsTableFormat<E> tableFormat, boolean showHeader) {
        // Remove old listeners.
        uninstallListeners();
        
        // Dispose old table model and event list.
        if (tableModel != null) {
            tableModel.dispose();
        }
        if (this.eventList instanceof TransformedList) {
            ((TransformedList) this.eventList).dispose();
        }
        
        // Save event list and table format.
        this.eventList = eventList;
        this.tableFormat = tableFormat;
        
        // Create new table model.
        tableModel = new DefaultEventTableModel<E>(eventList, tableFormat);
        setModel(tableModel);
        
        // Install new listeners.
        installListeners(showHeader);
    }

    /**
     * Initializes the table header based on the specified indicator, and 
     * installs listeners on the table header and column model.
     */
    private void installListeners(boolean showHeader) {
        if (showHeader) {
            JTableHeader tableHeader = getTableHeader();
            
            // Create table header if necessary.
            if (tableHeader == null) {
                tableHeader = createDefaultTableHeader();
                setTableHeader(tableHeader);
            }

            // Set up table header to have a context menu that configures 
            // which columns are visible.  We use MousePopupListener to detect
            // the popup trigger, which differs on Windows, Mac, and Linux.
            mousePopupListener = new MousePopupListener() {
                @Override
                public void handlePopupMouseEvent(MouseEvent e) {
                    JPopupMenu popupMenu = createHeaderPopupMenu();
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            };
            tableHeader.addMouseListener(mousePopupListener);
            
            // Install column state handler.
            columnStateHandler = new ColumnStateHandler(this, tableFormat);
            
        } else {
            // Reset listeners and table header.
            columnStateHandler = null;
            mousePopupListener = null;
            setTableHeader(null);
        }
    }
    
    /**
     * Uninstalls listeners on the table header and column model.
     */
    private void uninstallListeners() {
        if (getTableHeader() != null) {
            getTableHeader().removeMouseListener(mousePopupListener);
        }
        
        if (columnStateHandler != null) {
            columnStateHandler.removeListeners();
        }
    }
    
    /**
     * Creates the popup menu for the table header.
     */
    private JPopupMenu createHeaderPopupMenu() {
        TableColumnSelector columnSelector = new TableColumnSelector(this, tableFormat);
        return columnSelector.getPopupMenu();
    }
    
    /**
     * Loads the saved state of the columns. 
     *
     * <p>NOTE: This method must be called after the renderers and editors
     * have been loaded.  The settings must be applied in this order:
     * width/visibility/order.</p>
     */
    public void applySavedColumnSettings(){
        if (columnStateHandler != null) {
            columnStateHandler.setupColumnWidths();
            columnStateHandler.setupColumnVisibility(false);
            columnStateHandler.setupColumnOrder();
        }
    }
}
