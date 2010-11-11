package org.limewire.ui.swing.advanced.connection;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.FWTStatusReason;
import org.limewire.core.api.connection.FirewallStatus;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.api.connection.FirewallTransferStatus;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.listener.EventBean;
import org.limewire.ui.swing.advanced.connection.PopupManager.PopupProvider;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

import com.google.inject.Inject;

/**
 * Display panel for the connection summary. 
 */
public class ConnectionSummaryPanel extends JPanel implements Disposable {

    private static final String DISCONNECTED_PANEL_KEY = "DISCONNECTED_KEY";
    private static final String CONNECTED_PANEL_KEY = "CONNECTED_KEY";
    
    private final String IS_ULTRAPEER = I18n.tr("You are an Ultrapeer node");
    private final String IS_LEAF = I18n.tr("You are a Leaf node");
    private final String IS_NOT_FIREWALLED = I18n.tr("You are not behind a firewall");
    private final String IS_FIREWALLED_TRANSFERS = I18n.tr("You are behind a firewall and support firewall transfers");
    private final String IS_FIREWALLED_NO_TRANSFERS = I18n.tr("You are behind a firewall and do not support firewall transfers");
    private final String CONNECTED_TO = I18n.tr("Connected to:");
    private final String RESOLVE = I18n.tr("Show hostnames of connected peers");
    
    @Resource
    private Icon questionMarkIcon;
    
    /** Manager instance for connection data. */
    private final GnutellaConnectionManager gnutellaConnectionManager;
    
    /** Bean instance for firewall status. */
    private final EventBean<FirewallStatusEvent> firewallStatusBean;
    
    /** Bean instance for firewall transfer status. */
    private final EventBean<FirewallTransferStatusEvent> firewallTransferBean;

    /** List of connections. */
    private TransformedList<ConnectionItem, ConnectionItem> connectionList;

    private final JLabel nodeLabel = new JLabel();
    private final FirewallPanel firewallLabelPanel;
    private final JLabel summaryLabel = new JLabel();
    private final JTable summaryTable = new JTable();
    private final SummaryTableModel summaryTableModel = new SummaryTableModel();
    private final JCheckBox resolveCheckBox = new JCheckBox(RESOLVE);
    private final JLabel disconnectedMessageLabel = new JLabel();
    
    private final CardLayout switcher;
    private final JPanel disconnectedPanel;
    private final JPanel connectedPanel;
    
    /**
     * Constructs the ConnectionDetailPanel to display connections details.
     */
    @Inject
    public ConnectionSummaryPanel(GnutellaConnectionManager gnutellaConnectionManager,
            EventBean<FirewallStatusEvent> firewallStatusBean,
            EventBean<FirewallTransferStatusEvent> firewallTransferBean) {
        
        GuiUtils.assignResources(this);
        
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        this.firewallStatusBean = firewallStatusBean;
        this.firewallTransferBean = firewallTransferBean;
        
        setBorder(BorderFactory.createTitledBorder(""));
        setPreferredSize(new Dimension(120, 120));
        setOpaque(false);
        
        firewallLabelPanel = new FirewallPanel(questionMarkIcon);
        firewallLabelPanel.setOpaque(false);
        
        summaryLabel.setText(CONNECTED_TO);

        summaryTable.setModel(summaryTableModel);
        summaryTable.setSelectionForeground(Color.BLACK);
        summaryTable.setPreferredSize(new Dimension(120, 120));
        summaryTable.setShowGrid(false);
        summaryTable.setFocusable(false);

        // Set column widths.
        summaryTable.getColumnModel().getColumn(0).setPreferredWidth(24);
        summaryTable.getColumnModel().getColumn(1).setPreferredWidth(96);

        // Install renderer to align summary value.
        summaryTable.getColumnModel().getColumn(0).setCellRenderer(new SummaryCellRenderer());
        
        resolveCheckBox.setContentAreaFilled(false);
        boolean resolve = SwingUiSettings.RESOLVE_CONNECTION_HOSTNAMES.getValue();
        resolveCheckBox.setSelected(resolve);
        resolveCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean resolve = resolveCheckBox.isSelected();
                SwingUiSettings.RESOLVE_CONNECTION_HOSTNAMES.setValue(resolve);
            }
        });

        connectedPanel = new JPanel(new MigLayout("insets 0 0 0 0,fill",
            "[left]", "[top][top][bottom][top,fill]"));
        connectedPanel.setOpaque(false);
        
        connectedPanel.add(nodeLabel      , "cell 0 0");
        connectedPanel.add(firewallLabelPanel  , "cell 0 1, growx 100");
        connectedPanel.add(summaryLabel   , "cell 0 2");
        connectedPanel.add(summaryTable   , "cell 0 3");
        connectedPanel.add(resolveCheckBox, "cell 0 4");
        
        disconnectedPanel = new JPanel(new GridBagLayout());
        disconnectedPanel.setOpaque(false);
        
        disconnectedPanel.add(disconnectedMessageLabel);
        
        switcher = new CardLayout();
        setLayout(switcher);
        add(connectedPanel, CONNECTED_PANEL_KEY);
        add(disconnectedPanel, DISCONNECTED_PANEL_KEY);
    }

    @Override
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
        if (summaryTable != null) {
            summaryTable.setBackground(bgColor);
            summaryTable.setSelectionBackground(bgColor);
        }
    }
    
    /**
     * Initializes the data models in the container.
     */
    public void initData() {
        if (connectionList == null) {
            // Create connection list for Swing.  We wrap the actual list in a
            // Swing list to ensure that all events are fired on the UI thread.
            connectionList = GlazedListsFactory.swingThreadProxyEventList(
                    gnutellaConnectionManager.getConnectionList());

            // Set status.
            updateStatus();
        }
    }
    
    /**
     * Clears the data models in the container.
     */
    @Override
    public void dispose() {
        if (connectionList != null) {
            connectionList.dispose();
            connectionList = null;
        }
    }

    /**
     * Triggers a refresh of the data being displayed. 
     */
    public void refresh() {
        updateStatus();
        summaryTableModel.update(connectionList);
    }

    /**
     * Updates the firewall status fields.
     */
    private void updateStatus() {
        if (gnutellaConnectionManager.isConnected()) {
            switcher.show(this, CONNECTED_PANEL_KEY);
            
            // Set node description.
            boolean isUltrapeer = gnutellaConnectionManager.isUltrapeer();
            nodeLabel.setText(isUltrapeer ? IS_ULTRAPEER : IS_LEAF);

            // Get firewall status.
            FirewallStatus firewallStatus = firewallStatusBean.getLastEvent().getData();
        
            if (firewallStatus == FirewallStatus.FIREWALLED) {
                // Get firewall transfer status and reason.
                FirewallTransferStatusEvent event = firewallTransferBean.getLastEvent();
                FirewallTransferStatus transferStatus = event.getData();
                FWTStatusReason transferReason = event.getType();

                // Set firewall status and reason.
                if (transferStatus == FirewallTransferStatus.DOES_NOT_SUPPORT_FWT) {
                    firewallLabelPanel.setStatusText(IS_FIREWALLED_NO_TRANSFERS, getReasonText(transferReason));
                } else {
                    firewallLabelPanel.setStatusText(IS_FIREWALLED_TRANSFERS, null);
                }
            
            } else {
                // Not firewalled so clear transfer status and reason.
                firewallLabelPanel.setStatusText(IS_NOT_FIREWALLED, null);
            }
        } 
        else {
            switcher.show(this, DISCONNECTED_PANEL_KEY);
            switch (gnutellaConnectionManager.getConnectionStrength()) {
                case NO_INTERNET :
                    disconnectedMessageLabel.setText(I18n.tr("No internet connection!"));
                    break;
                case DISCONNECTED :
                    disconnectedMessageLabel.setText("<html>" + I18n.tr("Gnutella is disconnected however internet connection detected!")+"</html>");
                    break;
            }
        }
    }

    /**
     * Returns the display text for the specified firewall transfer status 
     * reason.
     */
    private String getReasonText(FWTStatusReason reason) {
        switch (reason) {
        case INVALID_EXTERNAL_ADDRESS:
            return I18n.tr("LimeWire has not been able to determine the external IP address of your NAT or firewall");
        case NO_SOLICITED_INCOMING_MESSAGES:
            return I18n.tr("LimeWire has not received any incoming UDP messages");
        case REUSING_STATUS_FROM_PREVIOUS_SESSION:
            return I18n.tr("LimeWire was not able to support firewall transfers in a previous session");
        case PORT_UNSTABLE:
            return I18n.tr("LimeWire is behind a NAT or firewall that assigns a different external port to each connection");
        case UNKNOWN:
        default:
            return "";
        }
    }

    /**
     * Panel to display the firewall's transfer status with a question mark icon
     * that opens a popup giving further explanation of the transfer status.  
     */
    private class FirewallPanel extends JPanel implements PopupProvider {
        
        private Point popupLocation;
        private String reasonText;
        private HTMLLabel statusLabel;
        private IconButton reasonButton;
        private PopupManager reasonPopupManager;
        
        public FirewallPanel(Icon argIcon) {           
            
            statusLabel = new HTMLLabel();
            statusLabel.setOpaque(false);
            reasonButton = new IconButton(argIcon);
            reasonButton.setBorder(null);
            reasonButton.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    popupLocation = e.getPoint();
                    // the location is relative to the component which in this case is a button,
                    // but we're interested in knowing the location relative to the container panel.
                    // So, let's set the panel width as the x position since the button is at 
                    // the far right hand side of the container.
                    popupLocation.x = getWidth();
                    
                    showPopup();
                }
            });
            
            reasonButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    reasonPopupManager.hidePopup();
                }
            });
            
            reasonButton.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    showPopup();
                }
            });

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(statusLabel);
            add(reasonButton);
            reasonButton.setVisible(false);

            this.reasonPopupManager = new PopupManager(this);
        }

        private void showPopup() {     
            // We offset the popup box a bit from the mouse location.  So, the popup won't be under the mouse and won't occlude the text.
            reasonPopupManager.showTimedPopup(this, popupLocation.x + 18, popupLocation.y + 10);
        }

        @Override
        public Component getPopupContent() {
            if ((reasonText != null) && (reasonText.length() > 0)) {
                // Return tooltip component for popup.
                JToolTip toolTip = createToolTip();
                toolTip.setBackground(Color.WHITE);
                toolTip.setTipText(reasonText);
                toolTip.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                return toolTip;
            } else {
                return null;
            }
        }
        
        public void setStatusText(String statusText, String reasonText) {
            statusLabel.setText(statusText);
            
            this.reasonText = reasonText;
            if ((statusText != null) && (statusText.length() > 0) && (reasonText != null) && (reasonText.length() > 0)) {
                reasonButton.setVisible(true);
            } else {
                reasonButton.setVisible(false);
            }

            repaint();
        }

        /*
         * For some reason the panel was sometimes resized to show only one of the two lines of text.
         * So, I overrode this method to return the minimum size from the status label. 
         */
        @Override
        public Dimension getMinimumSize() {
            return statusLabel.getMinimumSize();
        }
        
        /*
         * For some reason the panel was sometimes resized to show only one of the two lines of text.
         * So, I overrode this method to return the minimum size from the status label. 
         */
        @Override
        public Dimension getPreferredSize() {
            return statusLabel.getPreferredSize();
        }    
    }
    
    /**
     * Table cell renderer for connection count values in the summary table.
     * Values are right-aligned with a right margin.
     */
    private static class SummaryCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component renderer = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (renderer instanceof JLabel) {
                ((JLabel) renderer).setHorizontalAlignment(JLabel.RIGHT);
                ((JLabel) renderer).setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
            }

            return renderer;
        }
    }
    
    /**
     * Table model for the connection summary table. 
     */
    private static class SummaryTableModel extends AbstractTableModel {

        private final String CONNECTING = I18n.tr("Connecting");
        private final String LEAVES = I18n.tr("Leaves");
        private final String PEERS = I18n.tr("Peers");
        private final String STANDARD = I18n.tr("Standard");
        private final String ULTRAPEERS = I18n.tr("Ultrapeers");
        
        /** A 5-element array containing the number of connections with the
         *  following states: connecting, ultrapeer, peer, leaf, standard.
         */
        private int[] connectCounts = new int[5];

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return 5;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                // Return summary counts.  This maps the table rows to the 
                // elements in the connectCounts array.  Note that table row 3 
                // for 'connecting' maps to array index 0.
                switch (rowIndex) {
                case 0:
                    return connectCounts[1];
                case 1:
                    return connectCounts[2];
                case 2:
                    return connectCounts[3];
                case 3:
                    return connectCounts[0];
                case 4:
                    return connectCounts[4];
                default:
                    return null;
                }
            
            } else if (columnIndex == 1) {
                // Return labels.
                switch (rowIndex) {
                case 0:
                    return ULTRAPEERS;
                case 1:
                    return PEERS;
                case 2:
                    return LEAVES;
                case 3:
                    return CONNECTING;
                case 4:
                    return STANDARD;
                default:
                    return null;
                }
            }
            
            return null;
        }

        /**
         * Updates the data model using the specified connection list.
         */
        public void update(EventList<ConnectionItem> connectionList) {
            // The data model is a 5-element array containing the number of 
            // connections with the following states: connecting, ultrapeer,
            // peer, leaf, standard.
            Arrays.fill(connectCounts, 0);

            // Update connection counts.
            for (int i = 0; i < connectionList.size(); i++) {
                ConnectionItem item = connectionList.get(i);
                if (!item.isConnected()) {
                    connectCounts[0]++;
                } else if (item.isUltrapeer()) {
                    connectCounts[1]++;
                } else if (item.isPeer()) {
                    connectCounts[2]++;
                } else if (item.isLeaf()) {
                    connectCounts[3]++;
                } else {
                    connectCounts[4]++;
                }
            }
            
            // Fire event to update table.
            fireTableDataChanged();
        }
    }
}
