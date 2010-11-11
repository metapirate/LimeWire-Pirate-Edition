package org.limewire.ui.swing.advanced.connection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.friend.api.FriendPresence;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;

/**
 * Display panel for the connection details table.
 */
public class ConnectionDetailPanel extends JPanel implements Disposable {

    /** Manager instance for connection data. */
    private GnutellaConnectionManager gnutellaConnectionManager;
    
    /** Action to remove connection. */
    private Action removeConnectionAction = new RemoveConnectionAction(I18n.tr("Remove"));
    
    /** Action to view files on host. */
    private Action viewLibraryAction = new ViewLibraryAction(I18n.tr("Browse Files"));

    private JScrollPane scrollPane = new JScrollPane();
    private ConnectionTable connectionTable = new ConnectionTable();
    private JPopupMenu popupMenu = new JPopupMenu();
    
    private final FriendPresenceActions remoteHostActions;
    
    /**
     * Constructs the ConnectionDetailPanel to display connections details.
     */
    @Inject
    public ConnectionDetailPanel(GnutellaConnectionManager gnutellaConnectionManager, FriendPresenceActions remoteHostActions) {
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        this.remoteHostActions = remoteHostActions;
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setPreferredSize(new Dimension(120, 120));

        // Set table popup handler to display context menu.
        connectionTable.setPopupHandler(new TablePopupHandler() {
            @Override
            public boolean isPopupShowing(int row) {
                // Always return false to prevent color highlighting when popup
                // menu is displayed. See MouseableTable.MenuHighlightPredicate.
                return false;
            }

            @Override
            public void maybeShowPopup(Component component, int x, int y) {
                // Get target row.
                int row = connectionTable.rowAtPoint(new Point(x, y));
                
                // Change selection to include target row.
                if ((row >= 0) && !connectionTable.isRowSelected(row)) {
                    connectionTable.setRowSelectionInterval(row, row);
                }
                
                // Enable View Library action only if host is connected.
                ConnectionItem[] items = connectionTable.getSelectedConnections();
                if (items.length > 0) {
                    viewLibraryAction.setEnabled(items[0].isConnected());
                }
                
                // Show popup menu.
                popupMenu.show(component, x, y);
            }
        });
        
        // Set table double-click handler to view library.
        connectionTable.setDoubleClickHandler(new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                if (row >= 0) {
                    connectionTable.setRowSelectionInterval(row, row);
                }
                viewLibrary();
            }
        });

        // Add View Library action.
        JMenuItem menuItem = new JMenuItem(viewLibraryAction);
        popupMenu.add(menuItem);
        popupMenu.addSeparator();

        // Add Remove action.
        menuItem = new JMenuItem(removeConnectionAction);
        popupMenu.add(menuItem);
        
        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(connectionTable);
    }
    
    /**
     * Initializes the data models in the container.
     */
    public void initData() {
        if (!(connectionTable.getModel() instanceof DefaultEventTableModel)) {
            // Create connection list for Swing.  We wrap the actual list in a
            // Swing list to ensure that all events are fired on the UI thread.
            TransformedList<ConnectionItem, ConnectionItem> connectionList = 
                GlazedListsFactory.swingThreadProxyEventList(
                        gnutellaConnectionManager.getConnectionList());

            // Create table format.
            ConnectionTableFormat connectionTableFormat = new ConnectionTableFormat();

            // Set up connection table model.
            connectionTable.setEventList(connectionList, connectionTableFormat);
        }
    }
    
    /**
     * Clears the data models in the container.
     */
    @Override
    public void dispose() {
        connectionTable.clearEventList();
    }

    /**
     * Triggers a refresh of the data being displayed. 
     */
    public void refresh() {
        connectionTable.refresh();
    }

    /**
     * Displays the libraries for all selected connections.
     */
    private void viewLibrary() {
        // Browse hosts and display all selected connections.
        ConnectionItem[] items = connectionTable.getSelectedConnections();
        remoteHostActions.viewLibrariesOf(Collections2.transform(Collections2.filter(Arrays.asList(items), new Predicate<ConnectionItem>() {
            @Override
            public boolean apply(ConnectionItem input) {
                return input.isConnected();
            }
        }), new Function<ConnectionItem, FriendPresence>() {
            public FriendPresence apply(ConnectionItem from) {
                return from.getFriendPresence();
            }
        }));
        
        for (ConnectionItem item : items) {
            if(!item.isConnected()) {
                FocusJOptionPane.showMessageDialog(this, 
                    I18n.tr("Unable to view files - not yet connected to host"), 
                    I18n.tr("Connections"), JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    /**
     * Action to remove the selected connections. 
     */
    private class RemoveConnectionAction extends AbstractAction {
        
        public RemoveConnectionAction(String name) {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Remove all selected connections.
            ConnectionItem[] items = connectionTable.getSelectedConnections();
            for (ConnectionItem item : items) {
                gnutellaConnectionManager.removeConnection(item);
            }
            
            connectionTable.clearSelection();
        }
    }
    
    /**
     * Action to display the library for the selected connections.
     */
    private class ViewLibraryAction extends AbstractAction {
        
        public ViewLibraryAction(String name) {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            viewLibrary();
        }
    }
}
