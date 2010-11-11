package org.limewire.ui.swing.advanced.connection;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.ui.swing.advanced.connection.ConnectionTableFormat.ConnectionColumn;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * The table that displays the connections details.  ConnectionTable installs
 * a header popup menu to allow the user to configure the visible columns.
 */
public class ConnectionTable extends MouseableTable {
    
    /** List of connections. */
    private TransformedList<ConnectionItem, ConnectionItem> connectionList;
    
    /** Table format for connections details. */
    private ConnectionTableFormat tableFormat;
    
    /** Disabled menu item. */
    private JMenuItem disabledMenuItem;
    
    /** Context menu for table header. */
    private JPopupMenu headerPopup;
    
    /** Text array for connection tooltip. */
    private String[] tipArray;

    private Action defaultConfigAction = new DefaultConfigAction(I18n.tr("Revert To Default"));
    private Action autosortAction = new AutosortAction(I18n.tr("Sort Automatically"));
    private Action tooltipsAction = new TooltipsAction(I18n.tr("Extended Tooltips"));
    private Action toggleColumnAction = new ToggleColumnAction();
    
    /**
     * Constructs the connections detail table.
     */
    public ConnectionTable() {
        // Set attributes.
        setIntercellSpacing(new Dimension(1, 0));
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setShowHorizontalLines(false);
        
        // Set up table header to display context menu to configure visible 
        // columns.  As recommended in the API Javadoc for isPopupTrigger(), 
        // we check both the mousePressed and mouseReleased events.
        getTableHeader().setToolTipText(I18n.tr("Right-click to select columns to display"));
        getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showHeaderPopup(e);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showHeaderPopup(e);
                }
            }
        });
    }

    @Override
    protected void setTableHeaderRenderer() {
        //use default table headers
    }
    
    /**
     * Creates the default cell renderers.
     */
    @Override
    protected void createDefaultRenderers() {
        super.createDefaultRenderers();
        
        // Install custom renderer for Object values.
        setDefaultRenderer(Object.class, new DefaultLimeTableCellRenderer());
    }

    /**
     * Creates the tooltip component.  This method is called by the tooltip
     * manager whenever a new tooltip is needed.
     */
    @Override
    public JToolTip createToolTip() {
        // Create tooltip component.
        MultiLineToolTip toolTip = new MultiLineToolTip();
        toolTip.setComponent(this);
        
        // Set text array.
        toolTip.setToolTipArray(tipArray);
        
        return toolTip;
    }
    
    /**
     * Returns the tooltip text at the location of the specified mouse event.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int row = rowAtPoint(p);
        int col = columnAtPoint(p);
        
        // Get tooltip text if action is enabled.
        if (Boolean.TRUE.equals(tooltipsAction.getValue(Action.SELECTED_KEY)) 
                && (row >= 0)) {
            // Get connection item and tooltip text array.
            ConnectionItem item = connectionList.get(convertRowIndexToModel(row));
            tipArray = tableFormat.getToolTipArray(item);
            
            // Return row/col text so tooltip manager will create new tooltip
            // for each table cell.
            return (row + "," + col);
        }

        // Reset tooltip array and return null.
        tipArray = new String[0];
        return null;
    }

    /**
     * Returns true if the table should be sorted in response to the specified
     * table model event. 
     */
    @Override
    protected boolean shouldSortOnChange(TableModelEvent e) {
        if ((autosortAction == null) || 
            (Boolean.TRUE.equals(autosortAction.getValue(Action.SELECTED_KEY)))) {
            return super.shouldSortOnChange(e);
        } else {
            return false;
        }
    }

    /**
     * Sets the visibility of the column with the specified name.
     */
    public void setColumnVisible(String name, boolean visible) {
        // Get column and set visibility.
        TableColumnExt column = getColumnExt(name);
        column.setVisible(visible);

        // Get column title.
        String columnTitle = column.getTitle();
        
        // Find checkbox menu item with matching title.
        JCheckBoxMenuItem matchingItem = null;
        int itemCount = headerPopup.getComponentCount();
        for (int i = 0; i < itemCount; i++) {
            Component menuComponent = headerPopup.getComponent(i);
            if (menuComponent instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem) menuComponent;
                String itemTitle = item.getText();
                if (itemTitle.equals(columnTitle)) {
                    matchingItem = item;
                    break;
                }
            }
        }

        // Select matching menu item.
        if (matchingItem != null) {
            matchingItem.setSelected(visible);
        }
    }

    /**
     * Sets the width of the column with the specified name.
     */
    public void setColumnWidth(String name, int width) {
        getColumnExt(name).setPreferredWidth(width);
    }

    /**
     * Initializes the data model using the specified connection list and table
     * format. 
     */
    public void setEventList(
            TransformedList<ConnectionItem, ConnectionItem> connectionList,
            ConnectionTableFormat tableFormat) {
        
        if (tableFormat == null) {
            throw new IllegalArgumentException("tableFormat cannot be null");
        }
        
        this.connectionList = connectionList;
        this.tableFormat = tableFormat;

        // Set table model using event list and table format.
        setModel(new DefaultEventTableModel<ConnectionItem>(connectionList, tableFormat));

        // Create header popup menu.
        headerPopup = createHeaderPopup();
        
        // Initialize column visibility.
        resetTableColumns();
    }

    /**
     * Clears the data model and releases resources used by the event list.
     */
    public void clearEventList() {
        // Get table model and dispose resources.
        TableModel tableModel = getModel();
        if (tableModel instanceof DefaultEventTableModel) {
            ((DefaultEventTableModel) tableModel).dispose();
        }

        // Set default model to remove old reference.
        setModel(new DefaultTableModel());
        
        // Dispose connection list.
        connectionList.dispose();
        connectionList = null;
    }

    /**
     * Returns an array of the selected connections.
     */
    public ConnectionItem[] getSelectedConnections() {
        // Get selected rows.
        int[] rows = getSelectedRows();
        
        // Create result array.
        ConnectionItem[] items = new ConnectionItem[rows.length];
        for (int i = 0; i < rows.length; i++) {
            items[i] = connectionList.get(convertRowIndexToModel(rows[i]));
        }

        // Return result array.
        return items;
    }
    
    /**
     * Updates the table by firing a table model event.
     */
    public void refresh() {
        TableModel model = getModel();
        if (model instanceof AbstractTableModel) {
            ((AbstractTableModel) model).fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }
    
    /**
     * Resets the table columns and attributes to their default configuration.
     */
    public void resetTableColumns() {
        // Reset column widths and visibility.
        for (int col = 0; col < tableFormat.getColumnCount(); col++) {
            ConnectionColumn column = tableFormat.getColumn(col);
            setColumnWidth(column.getName(), column.getWidth());
            setColumnVisible(column.getName(), column.isVisible());
        }
        
        // Reset attributes.
        autosortAction.putValue(Action.SELECTED_KEY, Boolean.TRUE);
        tooltipsAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
    }

    /**
     * Creates a custom popup menu for the table header.
     */
    private JPopupMenu createHeaderPopup() {
        // Create popup menu.
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem defaultItem = new JMenuItem();
        defaultItem.setAction(defaultConfigAction);
        popupMenu.add(defaultItem);

        JMenu optionsMenu = new JMenu(I18n.tr("More Options"));
        popupMenu.add(optionsMenu);
        popupMenu.addSeparator();

        JMenuItem autosortItem = new JCheckBoxMenuItem();
        autosortItem.setAction(autosortAction);
        optionsMenu.add(autosortItem);
        
        JMenuItem tooltipsItem = new JCheckBoxMenuItem();
        tooltipsItem.setAction(tooltipsAction);
        optionsMenu.add(tooltipsItem);

        // Get column headings in default order.
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            // Create checkbox menu item for each column heading.
            String headerName = tableFormat.getColumnName(i);
            JMenuItem item = new JCheckBoxMenuItem(headerName, true);
            item.addActionListener(toggleColumnAction);
            popupMenu.add(item);
        }
        
        return popupMenu;
    }

    /**
     * Displays the header popup menu at the location of the specified mouse
     * event. 
     */
    private void showHeaderPopup(MouseEvent e) {
        if (headerPopup != null) {
            headerPopup.show((Component) e.getSource(), e.getX(), e.getY());
        }
    }
    
    /**
     * Updates the header popup menu by enabling/disabling menu items based on
     * the number of visible columns.
     */
    private void updateHeaderPopup() {
        // Get number of visible columns.
        int visibleColumnCount = getColumnCount(false);

        // If only one visible column, then disable its menu item.
        if (visibleColumnCount == 1) {
            // Find the only visible column.
            int allColumnCount = getColumnCount(true);
            TableColumnExt column = null;
            for (int i = 0; i < allColumnCount; i++) {
                column = getColumnExt(i);
                if (column.isVisible()) {
                    break;
                }
            }
            
            // Get menu item name from column title.
            assert column != null;
            String headerName = column.getTitle();

            // Find matching menu item, and disable so user cannot hide column.
            Component[] components = headerPopup.getComponents();
            for (Component component : components) {
                if (component instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) component; 
                    // Disable matching menu item.
                    if (item.getText().equals(headerName)) {
                        item.setEnabled(false);
                        disabledMenuItem = item;
                        break;
                    }
                }
            }
            
        } else if (disabledMenuItem != null) {
            // Re-enable previously disabled menu item because more than one
            // column is visible.
            disabledMenuItem.setEnabled(true);
        }
    }

    /**
     * Action to reset table to default configuration. 
     */
    private class DefaultConfigAction extends AbstractAction {

        public DefaultConfigAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            resetTableColumns();
        }
    }
    
    /**
     * Action to toggle option to automatically sort by column.
     */
    private static class AutosortAction extends AbstractAction {

        public AutosortAction(String name) {
            super(name);
            putValue(SELECTED_KEY, Boolean.TRUE);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }
    
    /**
     * Action to toggle option to display tooltips.
     */
    private static class TooltipsAction extends AbstractAction {

        public TooltipsAction(String name) {
            super(name);
            putValue(SELECTED_KEY, Boolean.FALSE);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    /**
     * Action that toggles the visibility of the selected table column.
     */
    private class ToggleColumnAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            // Get table column.
            String name = e.getActionCommand();
            TableColumnExt selectedColumn = getColumnExt(name);

            // Toggle column visibility.
            boolean visible = selectedColumn.isVisible();
            selectedColumn.setVisible(!visible);

            // Update popup menu.
            updateHeaderPopup();
            
            // Resize table columns to fit.
            packAll();
        }
    }

    /**
     * Tooltip component that displays multiple lines using a text array.
     */
    private static class MultiLineToolTip extends JToolTip {
        private static final int LINE_LEN = 72;
        
        private String[] tipArray;

        /**
         * Returns the tooltip text generated using the text array.  The
         * result is an HTML string.  This overrides the superclass method to
         * ignore the tip text set by the tooltip manager.
         * 
         * <p>Note that when the mouse moves to a new table cell, 
         * <code>getToolTipText(MouseEvent)</code> returns a different value,
         * causing the tooltip manager to create a new tooltip and set its
         * tip text.  This method allows the table to control the tooltip,
         * which may not change when the mouse moves between cells in the same
         * row.</p>
         */
        @Override
        public String getTipText() {
            if ((tipArray != null) && (tipArray.length > 0)) {
                // Create buffer to build string.
                StringBuilder buf = new StringBuilder("<html>");
                boolean firstLine = true;

                // Add each text line with breaks between lines.  Long lines
                // are wrapped onto multiple lines.
                for (String text : tipArray) {
                    while (text.length() > LINE_LEN) {
                        buf.append(firstLine ? "" : "<br/>");
                        buf.append(text.substring(0, LINE_LEN));
                        firstLine = false;
                        text = text.substring(LINE_LEN);
                    }
                    buf.append(firstLine ? "" : "<br/>");
                    buf.append(text);
                    firstLine = false;
                }

                // Return result string.
                buf.append("</html>");
                return buf.toString();
            
            } else {
                return null;
            }
        }
        
        /**
         * Sets the array of text lines in the tooltips.
         */
        public void setToolTipArray(String[] tipArray){
            this.tipArray = tipArray;
        }
    }
}
